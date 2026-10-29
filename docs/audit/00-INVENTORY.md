# 00 — FILE INVENTORY

Phạm vi: mọi file văn bản do nhóm phát triển tự viết. Loại trừ `target/`, `node_modules/`, `.git/`, `playwright-report/`, `test-results/`, `.playwright-cli/` và file nhị phân (ảnh).

Cột **Đọc** ghi mức độ thực tế của phiên kiểm toán này: `FULL` = đọc toàn văn, `PARTIAL` = đọc phần lớn/đọc các đoạn logic chính, `GREP` = chỉ tra cứu có mục tiêu, `NO` = chưa đọc.

| # | File | Dòng | Module | Đọc | Vấn đề phát hiện |
|---:|---|---:|---|---|---|
| 1 | `({i` | 4 | Root | FULL |  |
| 2 | `.env.example` | 33 | Root | FULL |  |
| 3 | `.github/modernize/java-upgrade/.gitignore` | 1 | CI | FULL |  |
| 4 | `.github/modernize/java-upgrade/hooks/scripts/recordToolUse.ps1` | 11 | CI | FULL |  |
| 5 | `.github/modernize/java-upgrade/hooks/scripts/recordToolUse.sh` | 20 | CI | FULL |  |
| 6 | `.github/workflows/playwright.yml` | 27 | CI | FULL |  |
| 7 | `.gitignore` | 22 | Root | FULL |  |
| 8 | `.vscode/settings.json` | 3 | Root | FULL |  |
| 9 | `agent-test.txt` | 1 | Root | FULL |  |
| 10 | `database/mysql/initial-data.sql` | 20 | SQL | FULL |  |
| 11 | `database/mysql/sample-data.sql` | 218 | SQL | FULL |  |
| 12 | `database/mysql/schema.sql` | 745 | SQL | FULL |  |
| 13 | `database/mysql/upgrade-staff-supervisor.sql` | 47 | SQL | FULL |  |
| 14 | `docs/ARCHITECTURE.md` | 27 | Docs | FULL |  |
| 15 | `docs/CUSTOMER_POLICIES.md` | 9 | Docs | FULL |  |
| 16 | `docs/OPERATIONS_MYSQL_BACKUP_RESTORE.md` | 19 | Docs | FULL |  |
| 17 | `FIXES-2026-10-29.md` | 22 | Root | FULL |  |
| 18 | `package.json` | 27 | Root | FULL |  |
| 19 | `package-lock.json` | 106 | Root | FULL |  |
| 20 | `playwright.config.js` | 71 | Root | FULL |  |
| 21 | `pom.xml` | 59 | Root | FULL |  |
| 22 | `README.md` | 216 | Root | FULL |  |
| 23 | `report.txt` | 564 | Root | FULL |  |
| 24 | `run.ps1` | 48 | Root | FULL |  |
| 25 | `scripts/backup-mysql.ps1` | 69 | Ops | FULL |  |
| 26 | `scripts/restore-mysql-test.ps1` | 60 | Ops | FULL |  |
| 27 | `src/main/java/com/rexchain/cinema/config/CinemaArtwork.java` | 54 | Config | FULL |  |
| 28 | `src/main/java/com/rexchain/cinema/config/CloudinaryConfig.java` | 6 | Config | FULL |  |
| 29 | `src/main/java/com/rexchain/cinema/config/DataSeeder.java` | 319 | Config | FULL |  |
| 30 | `src/main/java/com/rexchain/cinema/config/NotFoundAwareAuthenticationEntryPoint.java` | 42 | Config | FULL |  |
| 31 | `src/main/java/com/rexchain/cinema/config/RequiredSecretsValidator.java` | 29 | Config | FULL |  |
| 32 | `src/main/java/com/rexchain/cinema/config/SchemaSafetyGuard.java` | 36 | Config | FULL |  |
| 33 | `src/main/java/com/rexchain/cinema/config/SecurityConfig.java` | 112 | Config | FULL |  |
| 34 | `src/main/java/com/rexchain/cinema/config/SePayProperties.java` | 37 | Config | FULL |  |
| 35 | `src/main/java/com/rexchain/cinema/config/UiText.java` | 62 | Config | FULL |  |
| 36 | `src/main/java/com/rexchain/cinema/config/WebSocketConfig.java` | 147 | Config | FULL |  |
| 37 | `src/main/java/com/rexchain/cinema/controller/AccountExperienceController.java` | 46 | Controller | FULL |  |
| 38 | `src/main/java/com/rexchain/cinema/controller/AdminContentController.java` | 212 | Controller | FULL |  |
| 39 | `src/main/java/com/rexchain/cinema/controller/AdminController.java` | 974 | Controller | FULL |  |
| 40 | `src/main/java/com/rexchain/cinema/controller/AdminListSupport.java` | 159 | Controller | FULL |  |
| 41 | `src/main/java/com/rexchain/cinema/controller/AdminRewardsController.java` | 66 | Controller | FULL |  |
| 42 | `src/main/java/com/rexchain/cinema/controller/AdminSupportController.java` | 120 | Controller | FULL |  |
| 43 | `src/main/java/com/rexchain/cinema/controller/AuthController.java` | 271 | Controller | FULL |  |
| 44 | `src/main/java/com/rexchain/cinema/controller/BookingController.java` | 208 | Controller | FULL |  |
| 45 | `src/main/java/com/rexchain/cinema/controller/CheckInController.java` | 61 | Controller | FULL |  |
| 46 | `src/main/java/com/rexchain/cinema/controller/CinemaApiController.java` | 282 | Controller | FULL |  |
| 47 | `src/main/java/com/rexchain/cinema/controller/GlobalExceptionHandler.java` | 189 | Controller | FULL |  |
| 48 | `src/main/java/com/rexchain/cinema/controller/HeaderModelAdvice.java` | 46 | Controller | FULL |  |
| 49 | `src/main/java/com/rexchain/cinema/controller/HomeController.java` | 172 | Controller | FULL |  |
| 50 | `src/main/java/com/rexchain/cinema/controller/PaymentController.java` | 83 | Controller | FULL |  |
| 51 | `src/main/java/com/rexchain/cinema/controller/PolicyController.java` | 10 | Controller | FULL |  |
| 52 | `src/main/java/com/rexchain/cinema/controller/ProfileController.java` | 64 | Controller | FULL |  |
| 53 | `src/main/java/com/rexchain/cinema/controller/ReviewController.java` | 8 | Controller | FULL |  |
| 54 | `src/main/java/com/rexchain/cinema/controller/RewardsController.java` | 43 | Controller | FULL |  |
| 55 | `src/main/java/com/rexchain/cinema/controller/SePayWebhookController.java` | 103 | Controller | FULL |  |
| 56 | `src/main/java/com/rexchain/cinema/controller/SupportChatApiController.java` | 124 | Controller | FULL |  |
| 57 | `src/main/java/com/rexchain/cinema/dto/BankTransferSnapshot.java` | 29 | DTO | FULL |  |
| 58 | `src/main/java/com/rexchain/cinema/dto/BookingQuote.java` | 7 | DTO | FULL |  |
| 59 | `src/main/java/com/rexchain/cinema/dto/BookingQuoteRequest.java` | 3 | DTO | FULL |  |
| 60 | `src/main/java/com/rexchain/cinema/dto/CheckInRequest.java` | 6 | DTO | FULL |  |
| 61 | `src/main/java/com/rexchain/cinema/dto/CheckInResult.java` | 15 | DTO | FULL |  |
| 62 | `src/main/java/com/rexchain/cinema/dto/HoldRequest.java` | 7 | DTO | FULL |  |
| 63 | `src/main/java/com/rexchain/cinema/dto/LoginRequest.java` | 10 | DTO | FULL |  |
| 64 | `src/main/java/com/rexchain/cinema/dto/ManualRefundConfirmation.java` | 32 | DTO | FULL |  |
| 65 | `src/main/java/com/rexchain/cinema/dto/MembershipBenefitQuote.java` | 13 | DTO | FULL |  |
| 66 | `src/main/java/com/rexchain/cinema/dto/MembershipStatus.java` | 12 | DTO | FULL |  |
| 67 | `src/main/java/com/rexchain/cinema/dto/MovieOccupancyDto.java` | 17 | DTO | FULL |  |
| 68 | `src/main/java/com/rexchain/cinema/dto/PasswordResetCompleteRequest.java` | 11 | DTO | FULL |  |
| 69 | `src/main/java/com/rexchain/cinema/dto/PasswordResetRequest.java` | 10 | DTO | FULL |  |
| 70 | `src/main/java/com/rexchain/cinema/dto/PasswordResetVerifyRequest.java` | 12 | DTO | FULL |  |
| 71 | `src/main/java/com/rexchain/cinema/dto/PaymentSettingsDto.java` | 47 | DTO | FULL |  |
| 72 | `src/main/java/com/rexchain/cinema/dto/PaymentSettingsUpdateRequest.java` | 30 | DTO | FULL |  |
| 73 | `src/main/java/com/rexchain/cinema/dto/PaymentTransactionDto.java` | 57 | DTO | FULL |  |
| 74 | `src/main/java/com/rexchain/cinema/dto/RegisterRequest.java` | 41 | DTO | FULL |  |
| 75 | `src/main/java/com/rexchain/cinema/dto/SeatUpdateMessage.java` | 2 | DTO | FULL |  |
| 76 | `src/main/java/com/rexchain/cinema/dto/SePayWebhookRequest.java` | 16 | DTO | FULL |  |
| 77 | `src/main/java/com/rexchain/cinema/dto/SupportMessageDto.java` | 39 | DTO | FULL |  |
| 78 | `src/main/java/com/rexchain/cinema/dto/VerifiedRegistration.java` | 20 | DTO | FULL |  |
| 79 | `src/main/java/com/rexchain/cinema/dto/VerifyOtpRequest.java` | 12 | DTO | FULL |  |
| 80 | `src/main/java/com/rexchain/cinema/entity/AccountNotification.java` | 30 | Entity | FULL |  |
| 81 | `src/main/java/com/rexchain/cinema/entity/AgeRating.java` | 38 | Entity | FULL |  |
| 82 | `src/main/java/com/rexchain/cinema/entity/Article.java` | 52 | Entity | FULL |  |
| 83 | `src/main/java/com/rexchain/cinema/entity/Auditorium.java` | 19 | Entity | FULL |  |
| 84 | `src/main/java/com/rexchain/cinema/entity/Booking.java` | 175 | Entity | FULL |  |
| 85 | `src/main/java/com/rexchain/cinema/entity/BookingCombo.java` | 24 | Entity | FULL |  |
| 86 | `src/main/java/com/rexchain/cinema/entity/BookingSeat.java` | 37 | Entity | FULL |  |
| 87 | `src/main/java/com/rexchain/cinema/entity/BookingStatus.java` | 2 | Entity | FULL |  |
| 88 | `src/main/java/com/rexchain/cinema/entity/Cinema.java` | 19 | Entity | FULL |  |
| 89 | `src/main/java/com/rexchain/cinema/entity/ComboProduct.java` | 31 | Entity | FULL |  |
| 90 | `src/main/java/com/rexchain/cinema/entity/ComboType.java` | 6 | Entity | FULL |  |
| 91 | `src/main/java/com/rexchain/cinema/entity/DiscountType.java` | 2 | Entity | FULL |  |
| 92 | `src/main/java/com/rexchain/cinema/entity/Event.java` | 44 | Entity | FULL |  |
| 93 | `src/main/java/com/rexchain/cinema/entity/LatePaymentCase.java` | 54 | Entity | FULL |  |
| 94 | `src/main/java/com/rexchain/cinema/entity/LatePaymentCaseAudit.java` | 27 | Entity | FULL |  |
| 95 | `src/main/java/com/rexchain/cinema/entity/LatePaymentCaseStatus.java` | 3 | Entity | FULL |  |
| 96 | `src/main/java/com/rexchain/cinema/entity/LoyaltyLedgerEntry.java` | 52 | Entity | FULL |  |
| 97 | `src/main/java/com/rexchain/cinema/entity/LoyaltyPolicyAudit.java` | 21 | Entity | FULL |  |
| 98 | `src/main/java/com/rexchain/cinema/entity/LoyaltySettings.java` | 24 | Entity | FULL |  |
| 99 | `src/main/java/com/rexchain/cinema/entity/LoyaltySource.java` | 4 | Entity | FULL |  |
| 100 | `src/main/java/com/rexchain/cinema/entity/LoyaltyTransactionType.java` | 4 | Entity | FULL |  |
| 101 | `src/main/java/com/rexchain/cinema/entity/MediaItem.java` | 40 | Entity | FULL |  |
| 102 | `src/main/java/com/rexchain/cinema/entity/MediaType.java` | 4 | Entity | FULL |  |
| 103 | `src/main/java/com/rexchain/cinema/entity/MembershipTier.java` | 4 | Entity | FULL |  |
| 104 | `src/main/java/com/rexchain/cinema/entity/MembershipTierPolicy.java` | 26 | Entity | FULL |  |
| 105 | `src/main/java/com/rexchain/cinema/entity/Movie.java` | 79 | Entity | FULL |  |
| 106 | `src/main/java/com/rexchain/cinema/entity/MovieStatus.java` | 13 | Entity | FULL |  |
| 107 | `src/main/java/com/rexchain/cinema/entity/OtpChallenge.java` | 97 | Entity | FULL |  |
| 108 | `src/main/java/com/rexchain/cinema/entity/OtpRateLimit.java` | 33 | Entity | FULL |  |
| 109 | `src/main/java/com/rexchain/cinema/entity/PasswordResetChallenge.java` | 78 | Entity | FULL |  |
| 110 | `src/main/java/com/rexchain/cinema/entity/PaymentMethod.java` | 5 | Entity | FULL |  |
| 111 | `src/main/java/com/rexchain/cinema/entity/Review.java` | 21 | Entity | FULL |  |
| 112 | `src/main/java/com/rexchain/cinema/entity/RevokedAccessToken.java` | 24 | Entity | FULL |  |
| 113 | `src/main/java/com/rexchain/cinema/entity/Reward.java` | 34 | Entity | FULL |  |
| 114 | `src/main/java/com/rexchain/cinema/entity/RewardRedemption.java` | 29 | Entity | FULL |  |
| 115 | `src/main/java/com/rexchain/cinema/entity/RewardRedemptionStatus.java` | 4 | Entity | FULL |  |
| 116 | `src/main/java/com/rexchain/cinema/entity/Role.java` | 8 | Entity | FULL |  |
| 117 | `src/main/java/com/rexchain/cinema/entity/Seat.java` | 17 | Entity | FULL |  |
| 118 | `src/main/java/com/rexchain/cinema/entity/SeatHoldSession.java` | 32 | Entity | FULL |  |
| 119 | `src/main/java/com/rexchain/cinema/entity/SeatStatus.java` | 2 | Entity | FULL |  |
| 120 | `src/main/java/com/rexchain/cinema/entity/SeatType.java` | 2 | Entity | FULL |  |
| 121 | `src/main/java/com/rexchain/cinema/entity/SenderType.java` | 6 | Entity | FULL |  |
| 122 | `src/main/java/com/rexchain/cinema/entity/Showtime.java` | 79 | Entity | FULL |  |
| 123 | `src/main/java/com/rexchain/cinema/entity/ShowtimeSeat.java` | 29 | Entity | FULL |  |
| 124 | `src/main/java/com/rexchain/cinema/entity/SupportMessage.java` | 68 | Entity | FULL |  |
| 125 | `src/main/java/com/rexchain/cinema/entity/User.java` | 106 | Entity | FULL |  |
| 126 | `src/main/java/com/rexchain/cinema/entity/Voucher.java` | 40 | Entity | FULL |  |
| 127 | `src/main/java/com/rexchain/cinema/repository/AccountNotificationRepository.java` | 20 | Repository | FULL |  |
| 128 | `src/main/java/com/rexchain/cinema/repository/ArticleRepository.java` | 9 | Repository | FULL |  |
| 129 | `src/main/java/com/rexchain/cinema/repository/AuditoriumRepository.java` | 18 | Repository | FULL |  |
| 130 | `src/main/java/com/rexchain/cinema/repository/BankTransferSnapshotRepository.java` | 43 | Repository | FULL |  |
| 131 | `src/main/java/com/rexchain/cinema/repository/BookingRepository.java` | 246 | Repository | FULL |  |
| 132 | `src/main/java/com/rexchain/cinema/repository/BookingSeatRepository.java` | 38 | Repository | FULL |  |
| 133 | `src/main/java/com/rexchain/cinema/repository/CinemaRepository.java` | 17 | Repository | FULL |  |
| 134 | `src/main/java/com/rexchain/cinema/repository/ComboProductRepository.java` | 14 | Repository | FULL |  |
| 135 | `src/main/java/com/rexchain/cinema/repository/DatabaseSchemaResolver.java` | 21 | Repository | FULL |  |
| 136 | `src/main/java/com/rexchain/cinema/repository/EventRepository.java` | 8 | Repository | FULL |  |
| 137 | `src/main/java/com/rexchain/cinema/repository/LatePaymentCaseAuditRepository.java` | 7 | Repository | FULL |  |
| 138 | `src/main/java/com/rexchain/cinema/repository/LatePaymentCaseRepository.java` | 39 | Repository | FULL |  |
| 139 | `src/main/java/com/rexchain/cinema/repository/LoyaltyLedgerRepository.java` | 17 | Repository | FULL |  |
| 140 | `src/main/java/com/rexchain/cinema/repository/LoyaltyPolicyAuditRepository.java` | 7 | Repository | FULL |  |
| 141 | `src/main/java/com/rexchain/cinema/repository/LoyaltySettingsRepository.java` | 4 | Repository | FULL |  |
| 142 | `src/main/java/com/rexchain/cinema/repository/MediaItemRepository.java` | 8 | Repository | FULL |  |
| 143 | `src/main/java/com/rexchain/cinema/repository/MembershipTierPolicyRepository.java` | 10 | Repository | FULL |  |
| 144 | `src/main/java/com/rexchain/cinema/repository/MovieRepository.java` | 23 | Repository | FULL |  |
| 145 | `src/main/java/com/rexchain/cinema/repository/OtpChallengeRepository.java` | 12 | Repository | FULL |  |
| 146 | `src/main/java/com/rexchain/cinema/repository/OtpRateLimitRepository.java` | 10 | Repository | FULL |  |
| 147 | `src/main/java/com/rexchain/cinema/repository/PasswordResetChallengeRepository.java` | 12 | Repository | FULL |  |
| 148 | `src/main/java/com/rexchain/cinema/repository/PaymentSettingsRepository.java` | 73 | Repository | FULL |  |
| 149 | `src/main/java/com/rexchain/cinema/repository/PaymentTransactionRepository.java` | 121 | Repository | FULL |  |
| 150 | `src/main/java/com/rexchain/cinema/repository/ReviewRepository.java` | 8 | Repository | FULL |  |
| 151 | `src/main/java/com/rexchain/cinema/repository/RevokedAccessTokenRepository.java` | 11 | Repository | FULL |  |
| 152 | `src/main/java/com/rexchain/cinema/repository/RewardRedemptionRepository.java` | 12 | Repository | FULL |  |
| 153 | `src/main/java/com/rexchain/cinema/repository/RewardRepository.java` | 14 | Repository | FULL |  |
| 154 | `src/main/java/com/rexchain/cinema/repository/SeatHoldSessionRepository.java` | 14 | Repository | FULL |  |
| 155 | `src/main/java/com/rexchain/cinema/repository/SeatRepository.java` | 3 | Repository | FULL |  |
| 156 | `src/main/java/com/rexchain/cinema/repository/ShowtimeRepository.java` | 46 | Repository | FULL |  |
| 157 | `src/main/java/com/rexchain/cinema/repository/ShowtimeSeatRepository.java` | 37 | Repository | FULL |  |
| 158 | `src/main/java/com/rexchain/cinema/repository/SupportMessageRepository.java` | 46 | Repository | FULL |  |
| 159 | `src/main/java/com/rexchain/cinema/repository/UserRepository.java` | 35 | Repository | FULL |  |
| 160 | `src/main/java/com/rexchain/cinema/repository/VoucherRepository.java` | 19 | Repository | FULL |  |
| 161 | `src/main/java/com/rexchain/cinema/RexChainCinemaApplication.java` | 11 | Bootstrap | FULL |  |
| 162 | `src/main/java/com/rexchain/cinema/security/CsrfCookieFilter.java` | 18 | Security | FULL |  |
| 163 | `src/main/java/com/rexchain/cinema/security/CustomUserDetailsService.java` | 28 | Security | FULL |  |
| 164 | `src/main/java/com/rexchain/cinema/security/JwtAuthenticationFilter.java` | 48 | Security | FULL |  |
| 165 | `src/main/java/com/rexchain/cinema/security/JwtService.java` | 21 | Security | FULL |  |
| 166 | `src/main/java/com/rexchain/cinema/service/AccessTokenRevocationService.java` | 37 | Service | FULL |  |
| 167 | `src/main/java/com/rexchain/cinema/service/AiMovieConsultantService.java` | 153 | Service | FULL |  |
| 168 | `src/main/java/com/rexchain/cinema/service/AuthService.java` | 143 | Service | FULL |  |
| 169 | `src/main/java/com/rexchain/cinema/service/BankTransferPaymentService.java` | 57 | Service | FULL |  |
| 170 | `src/main/java/com/rexchain/cinema/service/BookingService.java` | 405 | Service | FULL |  |
| 171 | `src/main/java/com/rexchain/cinema/service/CheckInService.java` | 240 | Service | FULL |  |
| 172 | `src/main/java/com/rexchain/cinema/service/CinemaSetupService.java` | 90 | Service | FULL |  |
| 173 | `src/main/java/com/rexchain/cinema/service/CloudinaryService.java` | 71 | Service | FULL |  |
| 174 | `src/main/java/com/rexchain/cinema/service/CryptoUtil.java` | 7 | Service | FULL |  |
| 175 | `src/main/java/com/rexchain/cinema/service/DashboardService.java` | 129 | Service | FULL |  |
| 176 | `src/main/java/com/rexchain/cinema/service/ImageUploadValidator.java` | 137 | Service | FULL |  |
| 177 | `src/main/java/com/rexchain/cinema/service/LatePaymentCaseService.java` | 114 | Service | FULL |  |
| 178 | `src/main/java/com/rexchain/cinema/service/LoyaltyService.java` | 103 | Service | FULL |  |
| 179 | `src/main/java/com/rexchain/cinema/service/MembershipService.java` | 196 | Service | FULL |  |
| 180 | `src/main/java/com/rexchain/cinema/service/MovieOccupancyService.java` | 150 | Service | FULL |  |
| 181 | `src/main/java/com/rexchain/cinema/service/NotificationService.java` | 105 | Service | FULL |  |
| 182 | `src/main/java/com/rexchain/cinema/service/OtpRateLimitException.java` | 4 | Service | FULL |  |
| 183 | `src/main/java/com/rexchain/cinema/service/OtpService.java` | 363 | Service | FULL |  |
| 184 | `src/main/java/com/rexchain/cinema/service/PasswordChangeRateLimiter.java` | 16 | Service | FULL |  |
| 185 | `src/main/java/com/rexchain/cinema/service/PasswordResetService.java` | 243 | Service | FULL |  |
| 186 | `src/main/java/com/rexchain/cinema/service/PaymentService.java` | 292 | Service | FULL |  |
| 187 | `src/main/java/com/rexchain/cinema/service/PaymentSettingsService.java` | 51 | Service | FULL |  |
| 188 | `src/main/java/com/rexchain/cinema/service/ProfileService.java` | 38 | Service | FULL |  |
| 189 | `src/main/java/com/rexchain/cinema/service/QrCodeService.java` | 33 | Service | FULL |  |
| 190 | `src/main/java/com/rexchain/cinema/service/RewardRedemptionService.java` | 104 | Service | FULL |  |
| 191 | `src/main/java/com/rexchain/cinema/service/SeatBroadcastService.java` | 30 | Service | FULL |  |
| 192 | `src/main/java/com/rexchain/cinema/service/SeatReservationService.java` | 295 | Service | FULL |  |
| 193 | `src/main/java/com/rexchain/cinema/service/SePayPaymentService.java` | 60 | Service | FULL |  |
| 194 | `src/main/java/com/rexchain/cinema/service/SePayWebhookService.java` | 219 | Service | FULL |  |
| 195 | `src/main/java/com/rexchain/cinema/service/ShowtimeService.java` | 47 | Service | FULL |  |
| 196 | `src/main/java/com/rexchain/cinema/service/SupportChatService.java` | 124 | Service | FULL |  |
| 197 | `src/main/resources/application.properties` | 73 | Config | FULL |  |
| 198 | `src/main/resources/application-dev.properties` | 9 | Config | FULL |  |
| 199 | `src/main/resources/application-mysql.properties` | 8 | Config | FULL |  |
| 200 | `src/main/resources/application-prod.properties` | 13 | Config | FULL |  |
| 201 | `src/main/resources/static/css/app.css` | 2106 | CSS | NO | Chưa review (chỉ ảnh hưởng trình bày) |
| 202 | `src/main/resources/static/images/favicon.svg` | 11 | Config | FULL |  |
| 203 | `src/main/resources/static/images/icons.svg` | 43 | Config | FULL |  |
| 204 | `src/main/resources/static/js/admin-layout.js` | 64 | JS | GREP |  |
| 205 | `src/main/resources/static/js/admin-support.js` | 286 | JS | FULL |  |
| 206 | `src/main/resources/static/js/auth-form.js` | 220 | JS | FULL |  |
| 207 | `src/main/resources/static/js/bank-qr-payment.js` | 105 | JS | FULL |  |
| 208 | `src/main/resources/static/js/booking.js` | 182 | JS | FULL |  |
| 209 | `src/main/resources/static/js/chat-widget.js` | 453 | JS | FULL |  |
| 210 | `src/main/resources/static/js/checkin.js` | 163 | JS | FULL |  |
| 211 | `src/main/resources/static/js/chevron-arrows.js` | 40 | JS | GREP |  |
| 212 | `src/main/resources/static/js/header-account.js` | 70 | JS | GREP |  |
| 213 | `src/main/resources/static/js/header-nav.js` | 21 | JS | GREP |  |
| 214 | `src/main/resources/static/js/image-fallback.js` | 47 | JS | GREP |  |
| 215 | `src/main/resources/static/js/password-reset.js` | 81 | JS | FULL |  |
| 216 | `src/main/resources/static/js/profile.js` | 34 | JS | GREP |  |
| 217 | `src/main/resources/static/js/schedule-date-display.js` | 12 | JS | GREP |  |
| 218 | `src/main/resources/static/js/trailer-modal.js` | 96 | JS | GREP |  |
| 219 | `src/main/resources/templates/admin/articles.html` | 3 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 220 | `src/main/resources/templates/admin/auditoriums.html` | 128 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 221 | `src/main/resources/templates/admin/bookings.html` | 47 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 222 | `src/main/resources/templates/admin/checkin.html` | 52 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 223 | `src/main/resources/templates/admin/cinema-form.html` | 3 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 224 | `src/main/resources/templates/admin/cinemas.html` | 47 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 225 | `src/main/resources/templates/admin/combos.html` | 72 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 226 | `src/main/resources/templates/admin/dashboard.html` | 9 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 227 | `src/main/resources/templates/admin/events.html` | 3 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 228 | `src/main/resources/templates/admin/late-payment-detail.html` | 11 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 229 | `src/main/resources/templates/admin/late-payments.html` | 10 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 230 | `src/main/resources/templates/admin/media.html` | 3 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 231 | `src/main/resources/templates/admin/movie-form.html` | 83 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 232 | `src/main/resources/templates/admin/movies.html` | 68 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 233 | `src/main/resources/templates/admin/payment-settings.html` | 50 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 234 | `src/main/resources/templates/admin/reviews.html` | 3 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 235 | `src/main/resources/templates/admin/rewards.html` | 296 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 236 | `src/main/resources/templates/admin/showtimes.html` | 274 | Template | FULL |  |
| 237 | `src/main/resources/templates/admin/support.html` | 123 | Template | FULL |  |
| 238 | `src/main/resources/templates/admin/transaction-detail.html` | 5 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 239 | `src/main/resources/templates/admin/transactions.html` | 12 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 240 | `src/main/resources/templates/admin/users.html` | 97 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 241 | `src/main/resources/templates/admin/vouchers.html` | 3 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 242 | `src/main/resources/templates/article-detail.html` | 3 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 243 | `src/main/resources/templates/auth/forgot-password.html` | 67 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 244 | `src/main/resources/templates/auth/login.html` | 53 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 245 | `src/main/resources/templates/auth/register.html` | 152 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 246 | `src/main/resources/templates/booking/bank-qr-waiting.html` | 45 | Template | FULL |  |
| 247 | `src/main/resources/templates/booking/my-bookings.html` | 23 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 248 | `src/main/resources/templates/booking/seat-map.html` | 15 | Template | FULL |  |
| 249 | `src/main/resources/templates/booking/ticket.html` | 107 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 250 | `src/main/resources/templates/error.html` | 3 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 251 | `src/main/resources/templates/fragments/admin-list.html` | 22 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 252 | `src/main/resources/templates/fragments/admin-nav.html` | 55 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 253 | `src/main/resources/templates/fragments/chat-widget.html` | 84 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 254 | `src/main/resources/templates/fragments/footer.html` | 16 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 255 | `src/main/resources/templates/fragments/header.html` | 38 | Template | FULL |  |
| 256 | `src/main/resources/templates/fragments/movie-card.html` | 50 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 257 | `src/main/resources/templates/fragments/showtime-selection.html` | 28 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 258 | `src/main/resources/templates/fragments/trailer-modal.html` | 26 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 259 | `src/main/resources/templates/index.html` | 65 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 260 | `src/main/resources/templates/movie-detail.html` | 72 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 261 | `src/main/resources/templates/movies.html` | 9 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 262 | `src/main/resources/templates/notifications.html` | 6 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 263 | `src/main/resources/templates/policies.html` | 59 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 264 | `src/main/resources/templates/profile.html` | 31 | Template | FULL |  |
| 265 | `src/main/resources/templates/rewards/dashboard.html` | 39 | Template | FULL |  |
| 266 | `src/main/resources/templates/schedule.html` | 93 | Template | GREP | Đã grep th:action/th:href, chưa đọc toàn văn |
| 267 | `src/test/java/com/rexchain/cinema/BookingUiRegressionTest.java` | 53 | Test | GREP |  |
| 268 | `src/test/java/com/rexchain/cinema/config/CinemaArtworkTest.java` | 25 | Test | GREP |  |
| 269 | `src/test/java/com/rexchain/cinema/config/NotFoundAwareAuthenticationEntryPointTest.java` | 34 | Test | GREP |  |
| 270 | `src/test/java/com/rexchain/cinema/config/RequiredSecretsValidatorTest.java` | 39 | Test | GREP |  |
| 271 | `src/test/java/com/rexchain/cinema/config/RuntimeConfigurationContractTest.java` | 75 | Test | GREP |  |
| 272 | `src/test/java/com/rexchain/cinema/config/SchemaMigrationContractTest.java` | 36 | Test | GREP |  |
| 273 | `src/test/java/com/rexchain/cinema/config/SchemaSafetyGuardTest.java` | 40 | Test | GREP |  |
| 274 | `src/test/java/com/rexchain/cinema/config/WebSocketConfigTest.java` | 91 | Test | FULL |  |
| 275 | `src/test/java/com/rexchain/cinema/controller/AccountExperienceControllerTest.java` | 47 | Test | GREP |  |
| 276 | `src/test/java/com/rexchain/cinema/controller/AdminPaginationIntegrationTest.java` | 254 | Test | GREP |  |
| 277 | `src/test/java/com/rexchain/cinema/controller/AuthControllerSecurityTest.java` | 371 | Test | GREP |  |
| 278 | `src/test/java/com/rexchain/cinema/controller/ExceptionMappingRegressionTest.java` | 73 | Test | GREP |  |
| 279 | `src/test/java/com/rexchain/cinema/controller/NotificationReadMySqlIntegrationTest.java` | 109 | Test | GREP |  |
| 280 | `src/test/java/com/rexchain/cinema/controller/PaymentControllerExpiryTest.java` | 38 | Test | GREP |  |
| 281 | `src/test/java/com/rexchain/cinema/controller/SePayWebhookControllerTest.java` | 73 | Test | FULL |  |
| 282 | `src/test/java/com/rexchain/cinema/controller/SePayWebhookHmacTest.java` | 46 | Test | GREP |  |
| 283 | `src/test/java/com/rexchain/cinema/controller/SupportChatApiControllerTest.java` | 65 | Test | FULL |  |
| 284 | `src/test/java/com/rexchain/cinema/controller/UploadAuthorizationSecurityTest.java` | 66 | Test | GREP |  |
| 285 | `src/test/java/com/rexchain/cinema/ProfilePasswordLayoutRegressionTest.java` | 34 | Test | GREP |  |
| 286 | `src/test/java/com/rexchain/cinema/repository/DashboardAggregationQueryTest.java` | 36 | Test | GREP |  |
| 287 | `src/test/java/com/rexchain/cinema/repository/HomeContentQueryMySqlIntegrationTest.java` | 73 | Test | GREP |  |
| 288 | `src/test/java/com/rexchain/cinema/repository/HomeContentQueryRegressionTest.java` | 27 | Test | GREP |  |
| 289 | `src/test/java/com/rexchain/cinema/repository/PaymentMetadataSchemaScopeTest.java` | 31 | Test | GREP |  |
| 290 | `src/test/java/com/rexchain/cinema/repository/PaymentTransactionPaginationTest.java` | 42 | Test | GREP |  |
| 291 | `src/test/java/com/rexchain/cinema/repository/RepositoryMappingTest.java` | 125 | Test | GREP |  |
| 292 | `src/test/java/com/rexchain/cinema/security/AuthSessionMySqlIntegrationTest.java` | 137 | Test | GREP |  |
| 293 | `src/test/java/com/rexchain/cinema/security/JwtAuthenticationFilterTest.java` | 42 | Test | GREP |  |
| 294 | `src/test/java/com/rexchain/cinema/security/JwtServiceTest.java` | 42 | Test | GREP |  |
| 295 | `src/test/java/com/rexchain/cinema/service/AccessTokenRevocationServiceTest.java` | 54 | Test | GREP |  |
| 296 | `src/test/java/com/rexchain/cinema/service/AuthServiceTest.java` | 74 | Test | GREP |  |
| 297 | `src/test/java/com/rexchain/cinema/service/BookingHoldCycleIntegrationTest.java` | 218 | Test | FULL |  |
| 298 | `src/test/java/com/rexchain/cinema/service/BookingHoldReleaseGuardTest.java` | 70 | Test | FULL |  |
| 299 | `src/test/java/com/rexchain/cinema/service/BookingServiceTest.java` | 69 | Test | GREP |  |
| 300 | `src/test/java/com/rexchain/cinema/service/CheckInServiceTest.java` | 103 | Test | GREP |  |
| 301 | `src/test/java/com/rexchain/cinema/service/CloudinaryServiceTest.java` | 54 | Test | GREP |  |
| 302 | `src/test/java/com/rexchain/cinema/service/ImageUploadValidatorTest.java` | 61 | Test | GREP |  |
| 303 | `src/test/java/com/rexchain/cinema/service/LatePaymentCaseMySqlIntegrationTest.java` | 150 | Test | GREP |  |
| 304 | `src/test/java/com/rexchain/cinema/service/LatePaymentCaseServiceTest.java` | 53 | Test | GREP |  |
| 305 | `src/test/java/com/rexchain/cinema/service/LoyaltyServiceTest.java` | 39 | Test | GREP |  |
| 306 | `src/test/java/com/rexchain/cinema/service/ManualRefundMySqlIntegrationTest.java` | 164 | Test | GREP |  |
| 307 | `src/test/java/com/rexchain/cinema/service/MembershipPolicyMySqlIntegrationTest.java` | 119 | Test | GREP |  |
| 308 | `src/test/java/com/rexchain/cinema/service/MembershipServiceTest.java` | 116 | Test | GREP |  |
| 309 | `src/test/java/com/rexchain/cinema/service/MovieOccupancyServiceTest.java` | 103 | Test | GREP |  |
| 310 | `src/test/java/com/rexchain/cinema/service/NotificationServiceTest.java` | 37 | Test | GREP |  |
| 311 | `src/test/java/com/rexchain/cinema/service/OtpFailurePersistenceMySqlIntegrationTest.java` | 128 | Test | GREP |  |
| 312 | `src/test/java/com/rexchain/cinema/service/OtpPersistenceContractTest.java` | 34 | Test | GREP |  |
| 313 | `src/test/java/com/rexchain/cinema/service/OtpServiceTest.java` | 206 | Test | GREP |  |
| 314 | `src/test/java/com/rexchain/cinema/service/PasswordResetServiceTest.java` | 155 | Test | GREP |  |
| 315 | `src/test/java/com/rexchain/cinema/service/PaymentServiceTest.java` | 211 | Test | FULL |  |
| 316 | `src/test/java/com/rexchain/cinema/service/ProfileServiceTest.java` | 27 | Test | GREP |  |
| 317 | `src/test/java/com/rexchain/cinema/service/SeatReservationServiceTest.java` | 85 | Test | GREP |  |
| 318 | `src/test/java/com/rexchain/cinema/service/SePayReconciliationServiceTest.java` | 85 | Test | GREP |  |
| 319 | `src/test/java/com/rexchain/cinema/service/SePayWebhookServiceTest.java` | 140 | Test | FULL |  |
| 320 | `src/test/java/com/rexchain/cinema/UiDesignSystemRegressionTest.java` | 103 | Test | GREP |  |
| 321 | `src/test/java/com/rexchain/cinema/UnicodeEncodingRegressionTest.java` | 35 | Test | GREP |  |
| 322 | `src/test/js/bank-qr-payment.test.cjs` | 118 | Test | GREP |  |
| 323 | `src/test/js/header-account.test.cjs` | 14 | Test | GREP |  |
| 324 | `src/test/resources/application.properties` | 7 | Test | GREP |  |
| 325 | `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` | 1 | Test | GREP |  |
| 326 | `start-rex-prefilled.ps1` | 154 | Root | FULL |  |
| 327 | `tests/example.spec.js` | 14 | E2E | FULL |  |

**Tổng: 327 file văn bản, 25559 dòng.**
