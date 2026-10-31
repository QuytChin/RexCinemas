[CmdletBinding()]
param(
    [string]$OutputDirectory = (Join-Path (Split-Path -Parent $PSScriptRoot) 'backups\mysql'),
    [string]$DatabaseName,
    [switch]$AllowNonLocalHost
)
$ErrorActionPreference = 'Stop'

function Fail([string]$Message) { throw $Message }
function Required([string]$Name, [string]$Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) { Fail "Missing required setting: $Name" }
}

function Get-Settings {
    $url = [Environment]::GetEnvironmentVariable('DB_URL')
    $user = [Environment]::GetEnvironmentVariable('DB_USERNAME')
    $password = [Environment]::GetEnvironmentVariable('DB_PASSWORD')
    return @{ Url = $url; User = $user; Password = $password }
}

function Parse-Jdbc([string]$Url) {
    if ($Url -notmatch '^jdbc:mysql://(?<host>[^/:?]+)(:(?<port>\d+))?/(?<db>[^?]+)') { Fail 'DB_URL is not a supported MySQL JDBC URL.' }
    return @{ Host = $Matches.host; Port = $(if ($Matches.port) { $Matches.port } else { '3306' }); Database = $Matches.db }
}

$settings = Get-Settings
Required 'DB_URL' $settings.Url; Required 'DB_USERNAME' $settings.User; Required 'DB_PASSWORD' $settings.Password
$server = Parse-Jdbc $settings.Url
if (-not $AllowNonLocalHost -and $server.Host -notin @('localhost', '127.0.0.1', '::1')) { Fail 'Refusing a non-local MySQL backup without -AllowNonLocalHost.' }
if ($DatabaseName) { $server.Database = $DatabaseName }
Required 'database name' $server.Database

function Find-MySqlTool([string]$Name) {
    $command = Get-Command "$Name.exe" -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    @(
        "C:\Program Files\MySQL\MySQL Server 8.0\bin\$Name.exe",
        "C:\Program Files\MySQL\MySQL Server 9.0\bin\$Name.exe"
    ) | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
}
$dump = Find-MySqlTool 'mysqldump'
if (-not $dump) { Fail 'mysqldump.exe was not found in PATH or the standard MySQL installation folders.' }
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$output = Join-Path $OutputDirectory ("rex-cinemas-{0}-{1}.sql" -f $server.Database, $stamp)
$optionFile = Join-Path ([System.IO.Path]::GetTempPath()) ("rex-mysql-{0}.cnf" -f ([guid]::NewGuid().ToString('N')))

try {
    $escapedPassword = $settings.Password.Replace('\', '\\').Replace('"', '\"')
    [System.IO.File]::WriteAllLines($optionFile,
        [string[]]@("[client]", "user=$($settings.User)", ("password=" + '"' + $escapedPassword + '"'), "host=$($server.Host)", "port=$($server.Port)"),
        [System.Text.UTF8Encoding]::new($false))
    & icacls.exe $optionFile /inheritance:r /grant:r "${env:USERNAME}:(R,W)" | Out-Null
    & $dump --defaults-extra-file=$optionFile --single-transaction --quick --routines --triggers --events --hex-blob --set-gtid-purged=OFF --no-create-db --databases $server.Database --result-file=$output
    if ($LASTEXITCODE -ne 0) { Fail 'mysqldump failed.' }
    $item = Get-Item -LiteralPath $output
    if ($item.Length -le 0) { Fail 'Backup file is empty.' }
    Write-Output ("Backup created: {0} ({1} bytes)" -f $item.FullName, $item.Length)
} finally {
    Remove-Item -LiteralPath $optionFile -Force -ErrorAction SilentlyContinue
}
