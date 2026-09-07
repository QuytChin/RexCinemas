# CHANGELOG V3

## UX / Home
- Redesign dark cinema UI.
- Hero carousel.
- Now Showing / Coming Soon.
- Movie search.
- Schedule by date/cinema/movie.
- Events, articles, trailers/clips.
- Shared search modal and responsive Bootstrap navbar.

## Booking
- 4-step flow: Showtime -> Seat -> Snack -> Payment.
- Standard / VIP / Couple seat types.
- Realtime WebSocket hold state.
- Return held seats when navigating backward.
- Automatic repricing when showtime base price changes.
- Admin cancellation returns seats, member points and voucher usage.

## Rex Member
- loyaltyPoints + lifetimePoints.
- MEMBER / SILVER / GOLD / DIAMOND.
- Progress UI in profile.
- Points awarded on confirmed booking.

## Admin
- Dashboard date/cinema/movie/payment filters.
- Revenue, ticket count, AOV and booking states.
- Daily chart.
- Top movies and cinemas.
- Events CRUD.
- Articles CRUD.
- Media CRUD.
- Existing combo/voucher/review/user/booking/movie/cinema/showtime management retained.

## REST API / JWT
- GET /api/movies/now-showing
- GET /api/movies/coming-soon
- GET /api/movies/{id}
- GET /api/movies/{id}/showtimes
- GET /api/movies/{id}/reviews
- GET /api/showtimes/{id}/seats
- GET /api/snacks
- GET /api/me
- GET /api/me/bookings
- GET /api/payment-status/{bookingCode}
- POST /api/bookings/checkout

## Upgrade compatibility
- Hibernate update keeps V2 database usable.
- One-time legacy seat migration adds COUPLE rows and recalculates showtime seat prices.
- Demo membership points are initialized when upgrading an untouched demo account.

## Stability / hardening
- Registration validation for name/email/password.
- Configurable secure JWT cookie (`COOKIE_SECURE`).
- Friendly business error page.
- Prevent booking past/hidden showtimes and limit 10 seats per booking.
- Admin showtime edit preserves movie/room consistency.
