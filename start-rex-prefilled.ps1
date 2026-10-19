#Requires -Version 5.1
<#
RexCinemas startup (Windows PowerShell 5.1+ / PowerShell 7)
First run: prompts for settings, encrypts secrets with Windows DPAPI for current user.
Next runs: reuse saved settings. Use -Reconfigure to update values.
Run inside the RexCinemas project folder (where pom.xml is located).
IMPORTANT: This launches a local DEV profile and may update the local MySQL schema.
#>
[CmdletBinding()]
param(
    [switch] $Reconfigure,
    [switch] $NoStart
)
$ErrorActionPreference = 'Stop'

function Read-Text([string] $Prompt, [string] $Default = '') {
    if ($Default) { $value = Read-Host "$Prompt [$Default]" }
    else { $value = Read-Host $Prompt }
    if ([string]::IsNullOrWhiteSpace($value)) { return $Default }
    return $value.Trim()
}
function Read-YesNo([string] $Prompt, [bool] $Default = $false) {
    $suffix = if ($Default) { '[Y/n]' } else { '[y/N]' }
    $answer = Read-Host "$Prompt $suffix"
    if ([string]::IsNullOrWhiteSpace($answer)) { return $Default }
    return $answer -match '^(y|yes|co|c)$'
}
function Read-Secret([string] $Label, [bool] $Required = $false) {
    do {
        $s = Read-Host $Label -AsSecureString
        $plain = [System.Net.NetworkCredential]::new('', $s).Password
        if (-not $Required -or $plain.Length -gt 0) { return $s }
        Write-Warning "$Label cannot be empty."
    } while ($true)
}
function Set-EncryptedSecret([string] $Name, [System.Security.SecureString] $Secret, [string] $Dir) {
    $path = Join-Path $Dir "$Name.dpapi"
    # ConvertFrom-SecureString on Windows uses current-user DPAPI without a custom key.
    Set-Content -LiteralPath $path -Value ($Secret | ConvertFrom-SecureString) -Encoding UTF8
}
function Get-PlainSecret([string] $Name, [string] $Dir) {
    $path = Join-Path $Dir "$Name.dpapi"
    if (-not (Test-Path -LiteralPath $path)) { return '' }
    $secure = (Get-Content -LiteralPath $path -Raw).Trim() | ConvertTo-SecureString
    return [System.Net.NetworkCredential]::new('', $secure).Password
}
function Require-Setting([string] $Name, [string] $Value) {
    if ([string]::IsNullOrWhiteSpace($Value)) { throw "Missing setting: $Name. Run .\start-rex.ps1 -Reconfigure" }
}

# Project path is the folder containing this script. Place the script next to pom.xml.
$ProjectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not (Test-Path -LiteralPath (Join-Path $ProjectDir 'pom.xml'))) {
    throw 'Place start-rex.ps1 next to RexCinemas pom.xml, then run it again.'
}
if (-not $env:LOCALAPPDATA) { throw 'This script is intended for Windows with LOCALAPPDATA available.' }
$StateDir = Join-Path $env:LOCALAPPDATA 'RexCinemas\startup-config'
$ConfigPath = Join-Path $StateDir 'settings.json'

if ($Reconfigure -or -not (Test-Path -LiteralPath $ConfigPath)) {
    Write-Host '=== RexCinemas: one-time setup ===' -ForegroundColor Cyan
    Write-Warning 'Public ngrok URL can change; update this config with -Reconfigure whenever it does.'
    Write-Host 'Secrets are encrypted for this Windows account (not kept in the source folder).'
    New-Item -ItemType Directory -Force -Path $StateDir | Out-Null

    $cfg = [ordered]@{}
    $cfg.DB_URL = Read-Text 'MySQL JDBC URL' 'jdbc:mysql://localhost:3306/rex_chain_cinema?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh'
    $cfg.DB_USERNAME = Read-Text 'MySQL username' 'root'
    Set-EncryptedSecret 'DB_PASSWORD' (Read-Secret 'MySQL password' $true) $StateDir

    $jwtPath = Join-Path $StateDir 'JWT_SECRET.dpapi'
    if (-not (Test-Path -LiteralPath $jwtPath)) {
        $jwtBytes = New-Object byte[] 48
        [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($jwtBytes)
        $jwtPlain = [Convert]::ToBase64String($jwtBytes)
        $jwtSecure = ConvertTo-SecureString -String $jwtPlain -AsPlainText -Force
        Set-EncryptedSecret 'JWT_SECRET' $jwtSecure $StateDir
        Write-Host 'Generated and securely saved a persistent JWT_SECRET.' -ForegroundColor Green
    } else {
        Write-Host 'Keeping previously saved JWT_SECRET to preserve sessions.'
    }

    $cfg.MAIL_USERNAME = Read-Text 'Gmail address (for OTP)'
    Set-EncryptedSecret 'MAIL_PASSWORD' (Read-Secret 'Gmail App Password' $true) $StateDir

    $cloudOn = Read-YesNo 'Configure Cloudinary?' $true
    $cfg.CLOUDINARY_CLOUD_NAME = if ($cloudOn) { Read-Text 'Cloudinary Cloud Name' } else { '' }
    $cfg.CLOUDINARY_API_KEY = if ($cloudOn) { Read-Text 'Cloudinary API Key' } else { '' }
    if ($cloudOn) { Set-EncryptedSecret 'CLOUDINARY_API_SECRET' (Read-Secret 'Cloudinary API Secret' $true) $StateDir }

    $cfg.SEPAY_ENABLED = 'false'
    $cfg.SEPAY_BANK_ACCOUNT = '0000501454103'
    $cfg.SEPAY_BANK_CODE = 'MB'
    $cfg.SEPAY_BANK_NAME = 'MBBank'
    $cfg.SEPAY_ACCOUNT_HOLDER = 'LE PHAM QUANG KHANH'
    $cfg.SEPAY_PAYMENT_PREFIX = 'REX'
    $cfg.SEPAY_WEBHOOK_AUTH_MODE = 'API_KEY'
    $cfg.PAYMENT_BASE_URL = 'https://37fd-2001-ee0-5006-fb0-e524-afbe-9123-8dc6.ngrok-free.app'
    if (Read-YesNo 'Configure SePay BANK_QR now?' $true) {
        $cfg.SEPAY_BANK_ACCOUNT = Read-Text 'SePay bank account number' '0000501454103'
        $cfg.SEPAY_BANK_CODE = Read-Text 'SePay bank code' 'MB'
        $cfg.SEPAY_BANK_NAME = Read-Text 'Bank display name (optional)' 'MBBank'
        $cfg.SEPAY_ACCOUNT_HOLDER = Read-Text 'Account holder (optional)' 'LE PHAM QUANG KHANH'
        $cfg.SEPAY_PAYMENT_PREFIX = Read-Text 'Transfer reference prefix' 'REX'
        $cfg.SEPAY_WEBHOOK_AUTH_MODE = Read-Text 'Backend webhook auth mode (API_KEY/HMAC; verify actual backend & SePay settings)' 'API_KEY'
        # Keep keys out of this .ps1 file; enter them privately once (DPAPI-encrypted at rest).
        Set-EncryptedSecret 'SEPAY_API_KEY' (Read-Secret 'SePay API Key' $true) $StateDir
        Set-EncryptedSecret 'SEPAY_WEBHOOK_SECRET' (Read-Secret 'SePay Webhook Secret' $true) $StateDir
        $cfg.PAYMENT_BASE_URL = Read-Text 'Public HTTPS payment base URL (ngrok; update if address changes)' 'https://37fd-2001-ee0-5006-fb0-e524-afbe-9123-8dc6.ngrok-free.app'
        if (Read-YesNo 'Enable SePay now? Only after verifying webhook authentication, bank and public URL.' $false) {
            if ($cfg.PAYMENT_BASE_URL -notmatch '^https://') {
                Write-Warning 'SePay stays disabled: public payment URL must start with https://.'
            } elseif (-not $cfg.SEPAY_BANK_ACCOUNT -or -not $cfg.SEPAY_BANK_CODE) {
                Write-Warning 'SePay stays disabled: missing bank account or bank code.'
            } else { $cfg.SEPAY_ENABLED = 'true' }
        }
    }
    # ConvertTo-Json writes non-secret settings only.
    $cfg | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $ConfigPath -Encoding UTF8
    Write-Host "Saved configuration in: $StateDir" -ForegroundColor Green
}

$cfg = Get-Content -LiteralPath $ConfigPath -Raw | ConvertFrom-Json
Require-Setting 'DB_URL' $cfg.DB_URL
Require-Setting 'DB_USERNAME' $cfg.DB_USERNAME
Require-Setting 'MAIL_USERNAME' $cfg.MAIL_USERNAME

# Set env vars only in this PowerShell process and its Maven/Spring child process.
$env:SPRING_PROFILES_ACTIVE = 'dev,mysql'
$env:PORT = '8080'
$env:COOKIE_SECURE = 'false' # Set true when exclusively serving HTTPS behind a trusted reverse proxy.
$env:DB_URL = [string]$cfg.DB_URL
$env:DB_USERNAME = [string]$cfg.DB_USERNAME
$env:DB_PASSWORD = Get-PlainSecret 'DB_PASSWORD' $StateDir
$env:JWT_SECRET = Get-PlainSecret 'JWT_SECRET' $StateDir
$env:MAIL_USERNAME = [string]$cfg.MAIL_USERNAME
$env:MAIL_PASSWORD = Get-PlainSecret 'MAIL_PASSWORD' $StateDir
$env:CLOUDINARY_CLOUD_NAME = [string]$cfg.CLOUDINARY_CLOUD_NAME
$env:CLOUDINARY_API_KEY = [string]$cfg.CLOUDINARY_API_KEY
$env:CLOUDINARY_API_SECRET = Get-PlainSecret 'CLOUDINARY_API_SECRET' $StateDir
$env:SEPAY_ENABLED = [string]$cfg.SEPAY_ENABLED
$env:SEPAY_BANK_ACCOUNT = [string]$cfg.SEPAY_BANK_ACCOUNT
$env:SEPAY_BANK_CODE = [string]$cfg.SEPAY_BANK_CODE
$env:SEPAY_BANK_NAME = [string]$cfg.SEPAY_BANK_NAME
$env:SEPAY_ACCOUNT_HOLDER = [string]$cfg.SEPAY_ACCOUNT_HOLDER
$env:SEPAY_PAYMENT_PREFIX = [string]$cfg.SEPAY_PAYMENT_PREFIX
$env:SEPAY_WEBHOOK_AUTH_MODE = [string]$cfg.SEPAY_WEBHOOK_AUTH_MODE
$env:SEPAY_API_KEY = Get-PlainSecret 'SEPAY_API_KEY' $StateDir
$env:SEPAY_WEBHOOK_SECRET = Get-PlainSecret 'SEPAY_WEBHOOK_SECRET' $StateDir
$env:PAYMENT_BASE_URL = [string]$cfg.PAYMENT_BASE_URL

Require-Setting 'DB_PASSWORD' $env:DB_PASSWORD
Require-Setting 'JWT_SECRET' $env:JWT_SECRET
Require-Setting 'MAIL_PASSWORD' $env:MAIL_PASSWORD
if ([System.Text.Encoding]::UTF8.GetByteCount($env:JWT_SECRET) -lt 32) { throw 'JWT_SECRET is too short.' }

Write-Host '=== RexCinemas environment loaded ===' -ForegroundColor Green
Write-Host ('Profile: {0} | SePay: {1} | Payment base URL: {2}' -f $env:SPRING_PROFILES_ACTIVE,$env:SEPAY_ENABLED,$env:PAYMENT_BASE_URL)
Write-Warning 'DEV profile may run Hibernate schema update and SQL init on your local MySQL. Back up important data.'
if ($NoStart) { Write-Host 'NoStart selected; server not started.'; exit 0 }

Set-Location -LiteralPath $ProjectDir
$mvnWrapper = Join-Path $ProjectDir 'mvnw.cmd'
if (Test-Path -LiteralPath $mvnWrapper) { & $mvnWrapper 'spring-boot:run' }
elseif (Get-Command mvn -ErrorAction SilentlyContinue) { & mvn 'spring-boot:run' }
else { throw 'Maven not found. Install Maven or ensure mvnw.cmd exists.' }
