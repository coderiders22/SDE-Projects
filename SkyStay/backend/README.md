# SkyStay Backend — Spring Boot

Java 21 · Spring Boot 3.3 · Spring Security · JPA / Hibernate · PostgreSQL · JWT · OpenAPI.

The frontend in this Lovable workspace runs in a Cloudflare Workers
environment which cannot host a JVM, so the React app ships with a
localStorage-backed mock that mirrors this API's contract. To use the
real backend, run it locally (or on Render / Railway / your own host)
and point the frontend at it:

```bash
# in the project root
echo "VITE_API_URL=http://localhost:8080/api" >> .env
```

Then swap the function bodies in `src/lib/store.ts` for `fetch` calls
against `${API_BASE}/auth/login`, `${API_BASE}/hotels`, etc. — the
TypeScript types in `src/lib/types.ts` already match the DTOs below.

## Run locally

```bash
cd backend
docker compose up -d   # starts Postgres on :5432
./mvnw spring-boot:run # starts API on :8080
```

Swagger UI: <http://localhost:8080/swagger-ui.html>.

## Project layout

```
backend/
  pom.xml
  docker-compose.yml
  Dockerfile
  src/main/java/io/skystay/
    SkyStayApplication.java
    config/
      SecurityConfig.java
      OpenApiConfig.java
      CorsConfig.java
    auth/
      AuthController.java
      AuthService.java
      JwtService.java
      JwtAuthenticationFilter.java
      dto/{LoginRequest,RegisterRequest,AuthResponse}.java
    user/
      User.java
      Role.java
      UserRepository.java
      UserController.java
    hotel/
      Hotel.java
      Room.java
      HotelRepository.java
      RoomRepository.java
      HotelController.java
      HotelService.java
    flight/
      Flight.java
      FlightRepository.java
      FlightController.java
      FlightService.java
    booking/
      Booking.java
      BookingStatus.java
      PaymentStatus.java
      BookingKind.java
      BookingRepository.java
      BookingController.java
      BookingService.java
    payment/
      PaymentService.java          # mocked
    admin/
      AdminController.java
    common/
      GlobalExceptionHandler.java
      ApiError.java
  src/main/resources/
    application.yml
    db/migration/V1__init.sql
```

## API surface (MVP)

| Method | Path | Auth | Body | Notes |
| ------ | ---- | ---- | ---- | ----- |
| POST | `/api/auth/register` | public | `{name,email,password}` | issues JWT |
| POST | `/api/auth/login` | public | `{email,password}` | issues JWT |
| GET  | `/api/users/me` | JWT | — | current user |
| PUT  | `/api/users/me` | JWT | `{name,email}` | update profile |
| GET  | `/api/hotels` | public | `?city=&maxPrice=&minRating=` | search |
| GET  | `/api/hotels/{id}` | public | — | detail with rooms |
| POST | `/api/hotels` | ADMIN | hotel | create |
| PUT  | `/api/hotels/{id}` | ADMIN | hotel | update |
| DELETE | `/api/hotels/{id}` | ADMIN | — | delete |
| POST | `/api/hotels/{id}/rooms` | ADMIN | room | add room |
| GET  | `/api/flights` | public | `?from=&to=&date=` | search |
| POST | `/api/flights` | ADMIN | flight | create |
| PUT  | `/api/flights/{id}` | ADMIN | flight | update |
| DELETE | `/api/flights/{id}` | ADMIN | — | cancel |
| GET  | `/api/bookings` | JWT | — | current user's bookings |
| POST | `/api/bookings/hotel` | JWT | `{hotelId,roomId,checkIn,checkOut}` | books a room (transactional, prevents double booking) |
| POST | `/api/bookings/flight` | JWT | `{flightId}` | books a flight seat |
| DELETE | `/api/bookings/{id}` | JWT | — | cancel own booking |
| GET  | `/api/admin/stats` | ADMIN | — | dashboard totals |

All write endpoints validate input with Jakarta Bean Validation.
`/api/bookings/hotel` runs inside a transaction with a unique constraint
on `(room_id, check_in, check_out)` plus a pessimistic lock so a room
cannot be double-booked.

## Security

- BCrypt password hashing (`PasswordEncoder` bean).
- JWT signed with HS256 (`SKYSTAY_JWT_SECRET` env var, 256 bits).
- `JwtAuthenticationFilter` extracts `Authorization: Bearer …` on every
  request and populates the `SecurityContext`.
- `@PreAuthorize("hasRole('ADMIN')")` gates admin endpoints.
- CORS is restricted to `app.cors.allowed-origins` (set to the frontend
  origin).

## Environment

| Var | Default | Description |
| --- | ------- | ----------- |
| `SKYSTAY_DB_URL` | `jdbc:postgresql://localhost:5432/skystay` | JDBC URL |
| `SKYSTAY_DB_USER` | `skystay` | DB user |
| `SKYSTAY_DB_PASSWORD` | `skystay` | DB password |
| `SKYSTAY_JWT_SECRET` | (required in prod) | base64 256-bit key |
| `SKYSTAY_JWT_TTL_MIN` | `60` | access token lifetime |
| `SKYSTAY_CORS_ORIGINS` | `http://localhost:5173` | comma-separated |