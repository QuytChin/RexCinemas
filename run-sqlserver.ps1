# =========================================
# REXCINEMAS - POWERSHELL ENVIRONMENT (SQL SERVER)
# =========================================

# SERVER & PROFILE
$env:SPRING_PROFILES_ACTIVE = "dev,sqlserver"
$env:PORT = "8080"
$env:COOKIE_SECURE = "false"

# SQL SERVER
$env:DB_URL = "jdbc:sqlserver://localhost:1433;databaseName=rex_chain_cinema;encrypt=false;trustServerCertificate=true"
$env:DB_USERNAME = "sa"
$env:DB_PASSWORD = "huy210906"

# JWT
$env:JWT_SECRET = "rexchaincinemasecretkey2026forjwttokengenerationatleast32byteslong"
$env:JWT_EXPIRATION_MS = "86400000"
$env:JWT_REMEMBER_EXPIRATION_MS = "2592000000"

# GMAIL SMTP
$env:MAIL_USERNAME = "huybach219@gmail.com"
$env:MAIL_PASSWORD = "fucbvlakryphvhir"

# CLOUDINARY
$env:CLOUDINARY_CLOUD_NAME = "htuzwhzl"
$env:CLOUDINARY_API_KEY = "832325448116728"
$env:CLOUDINARY_API_SECRET = "uN4onbX067nukMkIv7hT8_zD9b8"

# CLOUDFLARE TUNNEL
$env:PAYMENT_BASE_URL = "https://bathrooms-launched-conclusions-orbit.trycloudflare.com"

# SEPAY / VIETQR
$env:SEPAY_ENABLED = "true"
$env:SEPAY_API_KEY = "REX_SEPAY_TEST_123456"
$env:SEPAY_BANK_ACCOUNT = "0000501454103"
$env:SEPAY_BANK_CODE = "MBBank"
$env:SEPAY_BANK_NAME = "MBBank"
$env:SEPAY_ACCOUNT_HOLDER = "LE PHAM QUANG KHANH"
$env:SEPAY_WEBHOOK_SECRET = "REX_SEPAY_TEST_123456"
$env:SEPAY_WEBHOOK_AUTH_MODE = "API_KEY"
$env:SEPAY_WEBHOOK_TIMESTAMP_TOLERANCE_SECONDS = "300"
$env:SEPAY_PAYMENT_PREFIX = "REX"

# DATABASE MIGRATION & DDL
$env:FLYWAY_ENABLED = "false"
$env:HIBERNATE_DDL_AUTO = "update"

# ADMIN / DEMO SEED
$env:ADMIN_INITIAL_EMAIL = "admin@rexchain.vn"
$env:ADMIN_INITIAL_PASSWORD = "1"
$env:SEED_DEMO_USERS = "false"
$env:DEMO_CUSTOMER_PASSWORD = "user@rexchain.vn"
$env:DEMO_MANAGER_PASSWORD = "1"

Write-Host "Da nap xong bien moi truong RexCinemas (SQL Server)." -ForegroundColor Green
Write-Host "Dang khoi dong server..." -ForegroundColor Cyan

mvn spring-boot:run
