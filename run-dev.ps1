# =========================================
# REXCINEMAS - POWERSHELL ENVIRONMENT & RUN
# =========================================

# 1. SERVER CONFIG
$env:SPRING_PROFILES_ACTIVE = "dev,mysql"
$env:PORT = "8080"
$env:COOKIE_SECURE = "false"

# 2. MYSQL CONFIG
# Luu y: Doi mat khau root MySQL phu hop voi may cua ban
$env:DB_URL = "jdbc:mysql://localhost:3306/rex_chain_cinema?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh"
$env:DB_USERNAME = "root"
if (-not $env:DB_PASSWORD) { $env:DB_PASSWORD = "huy210906" }

# 3. JWT SECRET
$env:JWT_SECRET = "rexchaincinemasecretkey2026forjwttokengenerationatleast32byteslong"
$env:JWT_EXPIRATION_MS = "86400000"
$env:JWT_REMEMBER_EXPIRATION_MS = "2592000000"

# 4. GMAIL SMTP
$env:MAIL_USERNAME = "huybach219@gmail.com"
$env:MAIL_PASSWORD = "fucbvlakryphvhir"

# 5. CLOUDINARY
$env:CLOUDINARY_CLOUD_NAME = "htuzwhzl"
$env:CLOUDINARY_API_KEY = "832325448116728"
$env:CLOUDINARY_API_SECRET = "uN4onbX067nukMkIv7hT8_zD9b8"

# 6. CLOUDFLARE TUNNEL
$env:PAYMENT_BASE_URL = "https://bathrooms-launched-conclusions-orbit.trycloudflare.com"

# 7. SEPAY / VIETQR
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

# 8. DATABASE AUTO DDL
$env:FLYWAY_ENABLED = "false"
$env:HIBERNATE_DDL_AUTO = "update"

# 9. ADMIN / DEMO ACCOUNTS
$env:ADMIN_INITIAL_EMAIL = "admin@rexchain.vn"
$env:ADMIN_INITIAL_PASSWORD = "1"
$env:SEED_DEMO_USERS = "false"
$env:DEMO_CUSTOMER_PASSWORD = "1"
$env:DEMO_MANAGER_PASSWORD = "1"

Write-Host ">>> Da nap xong bien moi truong RexCinemas!" -ForegroundColor Green
Write-Host ">>> Dang khoi chay Spring Boot..." -ForegroundColor Cyan

mvn spring-boot:run
