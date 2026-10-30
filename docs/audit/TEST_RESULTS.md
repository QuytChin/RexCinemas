# Kết quả kiểm thử và công cụ

| Kiểm tra | Kết quả | Ghi chú |
|---|---|---|
| `mvn -B -q test` | PASS: 245 tests, 0 failures, 0 errors, 10 skipped | Profile test/H2; lần cuối hoàn tất sau chỉnh sửa. |
| Hold-cycle concurrency regression | PASS trên H2 | Hai request đầu của cùng user dùng một session checkout; chưa chứng minh behavior MySQL. |
| `npx playwright test --project=chromium` | PASS: 23/23 | Test XSS chat hiện có. Firefox/WebKit chưa cài. |
| UI smoke 5 viewports | 40/40 response HTTP 200 | Chạy trên app 8080 có sẵn trước khi sửa. Không có overflow, ảnh hỏng, JS pageerror trong lượt đó. |
| Semgrep Java/OWASP | PASS: 0 finding | 160 rules, 283 tracked files; 68 files ignored. |
| SpotBugs `check` | FAIL gate: 136 warnings | 0 errors; chủ yếu EI_EXPOSE_REP/REP2. Xem còn tồn đọng. |
| Lighthouse mobile/desktop | Có baseline, chưa re-run sau sửa | Mobile 64/82/77/91; desktop 85/83/77/91 (Performance/A11y/Best Practices/SEO). |
| MySQL | Read-only audit + backup | Không chạy integration suite trên MySQL, không DDL/DML nghiệp vụ. |
| Thanh toán/mail/Cloudinary/Grok | Không gọi dịch vụ | SePay/webhook/SMTP thật bị loại khỏi thử nghiệm. |

Các log/bằng chứng runtime nằm dưới `target/` và bị ignore; báo cáo này không tuyên bố 100% coverage. Các bản Playwright browser chạy duy nhất là Chromium.
