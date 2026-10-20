[CmdletBinding(SupportsShouldProcess=$true, ConfirmImpact='High')]
param(
    [Parameter(Mandatory=$true)][string]$BackupFile,
    [Parameter(Mandatory=$true)][string]$TargetDatabase,
    [string]$TargetHost = 'localhost',
    [int]$TargetPort = 3306
)
$ErrorActionPreference = 'Stop'

function Fail([string]$Message) { throw $Message }
function Read-DpapiSecret([string]$Name, [string]$StateDirectory) {
    $path = Join-Path $StateDirectory "$Name.dpapi"
    if (-not (Test-Path -LiteralPath $path)) { return '' }
    $secure = (Get-Content -LiteralPath $path -Raw).Trim() | ConvertTo-SecureString
    return [System.Net.NetworkCredential]::new('', $secure).Password
}

if (-not (Test-Path -LiteralPath $BackupFile -PathType Leaf)) { Fail 'Backup file does not exist.' }
$backup = Get-Item -LiteralPath $BackupFile
if ($backup.Length -le 0) { Fail 'Backup file is empty.' }
if ($TargetHost -notin @('localhost', '127.0.0.1', '::1')) { Fail 'Restore target must be local.' }
if ($TargetDatabase -notmatch '^REX_TEST_[A-Za-z0-9_]+$') { Fail 'Restore target must be a dedicated REX_TEST_ database.' }

$url = [Environment]::GetEnvironmentVariable('DB_URL')
$user = [Environment]::GetEnvironmentVariable('DB_USERNAME')
$password = [Environment]::GetEnvironmentVariable('DB_PASSWORD')
if ([string]::IsNullOrWhiteSpace($url) -or [string]::IsNullOrWhiteSpace($user) -or [string]::IsNullOrWhiteSpace($password)) {
    if (-not $env:LOCALAPPDATA) { Fail 'DB settings are unavailable.' }
    $state = Join-Path $env:LOCALAPPDATA 'RexCinemas\startup-config'
    $cfg = Get-Content -LiteralPath (Join-Path $state 'settings.json') -Raw | ConvertFrom-Json
    $user = [string]$cfg.DB_USERNAME
    $password = Read-DpapiSecret 'DB_PASSWORD' $state
}
if ([string]::IsNullOrWhiteSpace($user) -or [string]::IsNullOrWhiteSpace($password)) { Fail 'DB credentials are unavailable.' }
function Find-MySqlTool([string]$Name) {
    $command = Get-Command "$Name.exe" -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    @(
        "C:\Program Files\MySQL\MySQL Server 8.0\bin\$Name.exe",
        "C:\Program Files\MySQL\MySQL Server 9.0\bin\$Name.exe"
    ) | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
}
$mysql = Find-MySqlTool 'mysql'
if (-not $mysql) { Fail 'mysql.exe was not found in PATH or the standard MySQL installation folders.' }

$optionFile = Join-Path ([System.IO.Path]::GetTempPath()) ("rex-mysql-restore-{0}.cnf" -f ([guid]::NewGuid().ToString('N')))
try {
    $escapedPassword = $password.Replace('\', '\\').Replace('"', '\"')
    @("[client]", "user=$user", ("password=" + '"' + $escapedPassword + '"'), "host=$TargetHost", "port=$TargetPort") |
        Set-Content -LiteralPath $optionFile -Encoding UTF8
    & icacls.exe $optionFile /inheritance:r /grant:r "$env:USERNAME:(R,W)" | Out-Null
    if (-not $PSCmdlet.ShouldProcess($TargetDatabase, 'Restore MySQL backup into dedicated test database')) { return }
    & $mysql --defaults-extra-file=$optionFile --batch --skip-column-names --execute="CREATE DATABASE IF NOT EXISTS $TargetDatabase CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    if ($LASTEXITCODE -ne 0) { Fail 'Could not prepare the dedicated restore database.' }
    $before = & $mysql --defaults-extra-file=$optionFile --batch --skip-column-names --database=$TargetDatabase --execute='SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE();'
    if ($LASTEXITCODE -ne 0) { Fail 'Could not inspect restore target.' }
    cmd.exe /d /c "`"$mysql`" --defaults-extra-file=`"$optionFile`" --database=$TargetDatabase < `"$BackupFile`""
    if ($LASTEXITCODE -ne 0) { Fail 'mysql restore failed.' }
    $after = & $mysql --defaults-extra-file=$optionFile --batch --skip-column-names --database=$TargetDatabase --execute='SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE();'
    if ($LASTEXITCODE -ne 0) { Fail 'Could not verify restored tables.' }
    Write-Output ("Restore verified. Tables before: {0}; tables after: {1}." -f $before, $after)
} finally {
    Remove-Item -LiteralPath $optionFile -Force -ErrorAction SilentlyContinue
}
