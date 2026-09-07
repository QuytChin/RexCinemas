# Rex Chain Cinema V3 - Local Merge

Bản này được nâng cấp từ project local đã chạy được của người dùng lên V3.

Đã giữ nguyên:
- `src/main/resources/application-mysql.properties` từ project local.
- Cấu hình IntelliJ trong `.idea` (JDK 21 / Maven project settings).

Đã thay bằng V3:
- Java source, Thymeleaf templates, CSS/JS, REST API JWT.
- Booking 4 bước, WebSocket giữ ghế, Rex Member.
- Event / Article / Media, dashboard nâng cao và các phần quản trị V3.
- README / CHANGELOG / kiến trúc V3.

Thư mục `target` cũ không được giữ lại để tránh class V2 bị dùng nhầm. IntelliJ/Maven sẽ build lại `target` khi chạy.
