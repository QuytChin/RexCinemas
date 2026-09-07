# Rex Chain Cinema V3 — Architecture

## High-level flow
```text
Browser / Postman
       |
       v
Spring Security + JwtAuthenticationFilter
       |
       +--> Thymeleaf MVC Controllers
       |       |
       |       +--> BookingService ----> WebSocket SeatBroadcastService
       |       +--> DashboardService
       |       +--> CloudinaryService
       |       +--> MomoPaymentService / VnpayPaymentService
       |
       +--> REST CinemaApiController
               |
               v
        Spring Data JPA
               |
       MySQL / PostgreSQL / SQL Server
```

## Main domain
```text
Cinema 1 --- n Auditorium 1 --- n Seat
Movie  1 --- n Showtime n --- 1 Auditorium
Showtime 1 --- n ShowtimeSeat n --- 1 Seat

User 1 --- n Booking 1 --- n BookingSeat
                     1 --- n BookingCombo n --- 1 ComboProduct
                     + --- Voucher snapshot/code

User 1 --- n Review n --- 1 Movie

Event
Article
MediaItem
```

## Seat concurrency
```text
AVAILABLE
   |
   | user selects + POST hold
   v
HELD (userId, holdUntil = now + 5m)
   |                         |
   | checkout success        | timeout/back/error
   v                         v
BOOKED                    AVAILABLE
```
`ShowtimeSeat` uses `@Version` for optimistic locking. Every hold/book/release is broadcast to `/topic/showtimes/{showtimeId}/seats`.

## Booking state
```text
PAY_AT_COUNTER: create -> CONFIRMED
MOMO/VNPAY:     create -> PENDING -> CONFIRMED
                                   -> CANCELLED
```
Online pending bookings expire after 15 minutes and release seats.

## Price
```text
STANDARD = basePrice
VIP      = basePrice + 20,000
COUPLE   = basePrice + 40,000

seatAmount  = sum(showtimeSeat.price)
comboAmount = sum(combo.price * qty)
subtotal    = seatAmount + comboAmount
discount    = voucher(subtotal)
total       = max(0, subtotal - discount)
```

## Membership
```text
confirmed booking -> floor(total / 10,000) points
MEMBER  < 250
SILVER  >= 250
GOLD    >= 600
DIAMOND >= 1200
```
Admin cancellation reverses awarded points and decrements voucher usage when applicable.

## Roles
```text
CUSTOMER
- browse public content
- booking/profile/review/member points

CINEMA_MANAGER
- customer access
- /admin/** except /admin/users/**

ADMIN
- full administration including user roles/lock
```

## Content
Home page content is database-driven:
```text
cinema_events
articles
media_items
```
Admin may create/edit/toggle/delete and upload images through Cloudinary `rex-chain/content`.

## V4 — QR Gate Check-in Flow

```text
Customer -> Schedule -> Booking -> CONFIRMED Booking
                              -> QR token stored in bookings.qr_token
                              -> GET /bookings/{id}/qr -> ZXing PNG

Cinema staff -> /admin/checkin -> camera/file/manual code
             -> POST /admin/checkin/api/scan
             -> BookingService.checkInTicket()
             -> validate status / duplicate / expiry
             -> bookings.checked_in_at + checked_in_by
```

QR token is independent from the numeric database id and remains invalid if the booking becomes cancelled.
