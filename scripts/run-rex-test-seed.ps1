param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('restore', 'local')]
    [string] $Target
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$url = $env:DB_URL
$user = $env:DB_USERNAME
$dbPassword = $env:DB_PASSWORD
$demoPassword = $env:DEMO_CUSTOMER_PASSWORD
if ([string]::IsNullOrWhiteSpace($url) -or [string]::IsNullOrWhiteSpace($user) -or
    [string]::IsNullOrWhiteSpace($dbPassword) -or [string]::IsNullOrWhiteSpace($demoPassword)) {
    throw 'DB_URL, DB_USERNAME, DB_PASSWORD, and DEMO_CUSTOMER_PASSWORD must be set in the process environment.'
}

if ($Target -eq 'local') {
    $schema = 'rex_chain_cinema'
    $urlPattern = '^jdbc:mysql://(localhost|127\.0\.0\.1)(:3306)?/rex_chain_cinema([?].*)?$'
} else {
    $urlPattern = '^jdbc:mysql://(localhost|127\.0\.0\.1)(:3306)?/(rex_chain_cinema_restore_[0-9]{8}_[0-9]{6})([?].*)?$'
}
if ($url -notmatch $urlPattern) { throw "DB_URL must point only to the approved localhost schema for Target=$Target." }
if ($Target -eq 'restore') { $schema = $Matches[3] }

$mysqlBin = 'C:\Program Files\MySQL\MySQL Server 8.0\bin'
$dumpTool = Join-Path $mysqlBin 'mysqldump.exe'
$mysqlTool = Join-Path $mysqlBin 'mysql.exe'
if (!(Test-Path -LiteralPath $dumpTool) -or !(Test-Path -LiteralPath $mysqlTool)) {
    throw 'Local MySQL client tools are required (mysqldump.exe and mysql.exe).'
}

$backupDir = Join-Path $env:LOCALAPPDATA 'Temp\rex-cinema-seed-audit'
New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$backupPath = Join-Path $backupDir "${schema}_${stamp}.sql"
$env:MYSQL_PWD = $dbPassword
try {
    & $dumpTool --host=localhost --port=3306 --user=$user --single-transaction --routines --triggers --events --result-file=$backupPath $schema 2>$null
    if ($LASTEXITCODE -ne 0 -or !(Test-Path -LiteralPath $backupPath) -or (Get-Item -LiteralPath $backupPath).Length -lt 1024) {
        throw 'MySQL backup failed or the resulting file is unexpectedly small.'
    }
    $expectedTables = (Select-String -LiteralPath $backupPath -Pattern '^CREATE TABLE ' | Measure-Object).Count
    if ($expectedTables -lt 1) { throw 'Backup is not readable as a MySQL schema/data dump.' }

    # Restore the snapshot to a fresh, uniquely named local audit schema before touching the requested DB.
    $restoreSchema = "rex_chain_cinema_restore_$stamp"
    & $mysqlTool --host=localhost --port=3306 --user=$user --execute="CREATE DATABASE ``$restoreSchema`` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci" 2>$null
    if ($LASTEXITCODE -ne 0) { throw 'Could not create isolated schema for backup restore verification.' }
    $sourcePath = $backupPath.Replace('\', '/')
    & $mysqlTool --host=localhost --port=3306 --user=$user --database=$restoreSchema --execute="source $sourcePath" 2>$null
    if ($LASTEXITCODE -ne 0) { throw 'Backup restore into isolated schema failed.' }
    $tableCount = (& $mysqlTool --host=localhost --port=3306 --user=$user --database=information_schema --batch --skip-column-names --execute="SELECT COUNT(*) FROM tables WHERE table_schema='$restoreSchema' AND table_type='BASE TABLE'" 2>$null | Select-Object -Last 1)
    if ($LASTEXITCODE -ne 0 -or [int]$tableCount -ne $expectedTables) { throw 'Restored schema table count does not match the backup.' }

    Push-Location $repo
    try {
        mvn -q -DskipTests test-compile
        if ($LASTEXITCODE -ne 0) { throw 'Maven test compilation failed.' }
        mvn -q "-Dmdep.outputFile=target/test-seed-classpath.txt" "-Dmdep.includeScope=test" dependency:build-classpath
        if ($LASTEXITCODE -ne 0) { throw 'Could not assemble the test runtime classpath.' }
        $cp = (Get-Content -Raw (Join-Path $repo 'target/test-seed-classpath.txt')).Trim()
        & java -cp "target/test-classes;target/classes;$cp" com.rexchain.cinema.tools.LocalTestDataSeeder
        if ($LASTEXITCODE -ne 0) { throw 'REX_TEST seed failed; the utility rolls back its transaction on failure.' }
    } finally { Pop-Location }
    Write-Output "Seed succeeded for $schema. Backup and verified restore are stored under the user temp audit folder."
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}
