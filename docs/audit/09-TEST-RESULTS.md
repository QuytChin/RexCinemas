# 09 — KẾT QUẢ KIỂM THỬ

Môi trường: Windows, JDK 21.0.12, Maven 3.10.0. Database test: H2 in-memory `MODE=MySQL`, `ddl-auto=create-drop`. Không dùng MySQL production, không gọi webhook/thanh toán thật.

## Lệnh đã chạy

| Lệnh | Kết quả |
|---|---|
| `mvn -B -q -DskipTests compile` | BUILD SUCCESS |
| `mvn -B -q -DskipTests test-compile` | BUILD SUCCESS |
| `mvn -B test -Dtest=WebSocketConfigTest,SupportChatApiControllerTest,BookingHoldReleaseGuardTest,PaymentServiceTest,SePayWebhookServiceTest` | 29 bài, 0 lỗi |
| `mvn -B test` (toàn bộ) | **190 bài, 0 failure, 0 error, 9 skipped, BUILD SUCCESS** |

## Phân bố 188 bài

| Nhóm | Bài |
|---|---|
| controller | 8 BookingTicketAccess + 22 AuthControllerSecurity + 11 AdminPagination + 4 ExceptionMapping + 4 SupportChatApi + 3 UploadAuthorization + 3 SePayWebhookController + 2 SePayWebhookHmac + 2 AccountExperience + 1 PaymentControllerExpiry |
| config | 6 WebSocketConfig, cùng các bài cho CinemaArtwork/NotFoundAware/RequiredSecrets/RuntimeContract/SchemaMigration/SchemaSafety |
| repository | DashboardAggregation, HomeContentQueryRegression, PaymentMetadataSchemaScope, PaymentTransactionPagination, RepositoryMapping |
| security | JwtAuthenticationFilter (2), JwtService (4) |
| service | BookingHoldCycleIntegration (5), BookingHoldReleaseGuard (3), PaymentSettings (2), PaymentService (7), SePayWebhookService (9), OtpService (8), AuthService (6), SeatReservation (3), CheckIn (4), Membership (5), PasswordReset (4), Loyalty (2), LatePaymentCase (2), Notification (2), Profile (1), MovieOccupancy (2), Cloudinary (3), ImageUploadValidator (3), AccessTokenRevocation (3), SePayReconciliation (3) |
| UI/encoding regression | BookingUi, ProfilePasswordLayout, UiDesignSystem, UnicodeEncoding |

## 9 bài bị skip (có điều kiện)

Các bài yêu cầu MySQL thật, tự skip khi không có biến môi trường:
`NotificationReadMySqlIntegrationTest` (2), `HomeContentQueryMySqlIntegrationTest` (1), `AuthSessionMySqlIntegrationTest` (1), `LatePaymentCaseMySqlIntegrationTest` (1), `ManualRefundMySqlIntegrationTest` (1), `MembershipPolicyMySqlIntegrationTest` (1), `OtpFailurePersistenceMySqlIntegrationTest` (2).

→ Trạng thái: **NOT VERIFIED** cho các luồng này (không được ghi PASS).

## Những gì KHÔNG được kiểm chứng

| Hạng mục | Trạng thái | Lý do |
|---|---|---|
| Đồng thời thật (2 request cùng ghế, 2 webhook cùng lúc) | NOT VERIFIED | Chỉ suy luận từ khóa bi quan + UNIQUE; không chạy test đa luồng với MySQL thật |
| SePay end-to-end (webhook thật) | NOT VERIFIED | Không gọi dịch vụ thật |
| Cloudinary / SMTP | NOT VERIFIED | Không cấu hình |
| MySQL 8 thật (schema, index, collation) | NOT VERIFIED | Không có server |
| Playwright E2E / responsive / accessibility | NOT VERIFIED | `tests/example.spec.js` chỉ test trang playwright.dev; không chạy trình duyệt |
| Kiểm tra CVE phụ thuộc | NOT VERIFIED | Không chạy plugin |
| WebSocket runtime (handshake + principal) | NOT VERIFIED ở mức tích hợp | Đã test logic `validateInbound` bằng unit test; chưa chạy server thật |

## Ghi chú về bài test mới

- `WebSocketConfigTest` kiểm trực tiếp `validateInbound(command, destination, principal)` — đây là điểm quyết định quyền, không phụ thuộc hạ tầng broker.
- `BookingHoldReleaseGuardTest` kiểm đúng nhánh controller lỗi (mô phỏng exception sau commit).
- Không có test nào bị làm yếu để PASS.