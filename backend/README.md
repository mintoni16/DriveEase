# DriveEase – Java + MySQL backend (no Spring Boot)

Plain Java: the JDK's built-in HTTP server (`com.sun.net.httpserver`) + JDBC.
No Maven, no Gradle, no frameworks. The only external file is the MySQL driver jar.
The server also serves your frontend, so the site and the API share one address.

## You need
1. **JDK 17 or newer** (`javac -version` should work)
2. **MySQL 8** running locally
3. **MySQL driver jar** → download and put it in `lib/`
   (direct link is in `lib/PUT_MYSQL_DRIVER_HERE.txt`)

## Run
1. Open `config.properties` and set `db.password` (and `db.user` if not `root`).
2. Start it:
   - Linux / macOS: `./run.sh`
   - Windows: `run.bat`
3. Open **http://localhost:5000** (don't open the .htm files straight from disk).

On first start the app creates the `driveease` database, all tables
(from `database/schema.sql`) and the 6 cars. You can also run `schema.sql` by hand
in MySQL Workbench if you prefer.

## Project layout
```
config.properties        DB login + port
database/schema.sql      tables (users, cars, bookings, sessions)
frontend/                your pages + script.js (talks to the API)
src/driveease/
  Main.java              routes + server start
  Ctx.java               one request: body, cookies, JSON replies, DB connection
  Auth.java              login sessions (stored in MySQL) + login throttle
  UserApi.java           register / login / logout / profile / password
  CarApi.java            cars + locations
  BookingApi.java        bookings, cancel, dashboard
  StaticFiles.java       serves frontend/, protects private pages
  Db.java  Models.java  Validate.java  Security.java  Json.java  Config.java  ApiException.java
```

## API
All bodies are JSON; errors look like `{"error": "message"}`.

| Method | Path | Login | Purpose |
|--------|------|:----:|---------|
| POST | `/api/register` | | `{name, email, password, phone?}` – creates account and logs in |
| POST | `/api/login` | | `{email, password, remember?}` |
| POST | `/api/logout` | | |
| GET / PUT | `/api/me` | ✔ | read / update `{name, email, phone}` |
| PUT | `/api/me/password` | ✔ | `{currentPassword, newPassword}` |
| GET | `/api/cars?category=&sort=` | | sort: `recommended`, `price_asc`, `price_desc`, `name` |
| GET | `/api/cars/{id}` | | |
| GET | `/api/locations` | | |
| POST | `/api/bookings` | ✔ | `{carId, pickupDate, returnDate, pickupLocation, dropLocation}` |
| GET | `/api/bookings` | ✔ | `{bookings, upcoming, past}` |
| GET | `/api/bookings/{id}` | ✔ | |
| POST | `/api/bookings/{id}/cancel` | ✔ | only before the pickup date |
| GET | `/api/dashboard` | ✔ | stats, next booking, featured cars |

## Rules enforced on the server
- Total = days × the car's daily rate, computed by the server (the browser's price is never trusted).
- No double-booking: creating a booking locks the car's row (`SELECT … FOR UPDATE`) inside a transaction, then checks for overlapping dates. A return day can be the next pickup day.
- Pickup not in the past, return after pickup, max 30 days, locations limited to Pune / Mumbai / Nashik / Bengaluru.
- Users only see and cancel their own bookings. "Completed" is derived once the return date has passed.

## Security
- Passwords: PBKDF2-HMAC-SHA256, 210,000 iterations, random salt (JDK only).
- Sessions: random token in an `HttpOnly`, `SameSite=Lax` cookie; MySQL stores only its SHA-256 hash. "Remember me" = 30 days, otherwise it ends when the browser closes (max 1 day).
- All SQL uses `PreparedStatement`; the only dynamic SQL (sort order) comes from a fixed whitelist.
- Write requests must be `application/json` (basic CSRF protection).
- 5 failed logins per IP+email in 5 minutes are throttled (in memory).
- `dashboard`, `bookings`, `my_bookings` and `profile` pages redirect to login when signed out.

## Notes
- Each request opens its own MySQL connection – simple and fine for a project; a connection pool would be the next step for heavy traffic.
- Behind HTTPS set `production=true` in `config.properties` (adds the `Secure` cookie flag).
- Config can also come from environment variables: `DRIVEEASE_DB_URL`, `DRIVEEASE_DB_USER`, `DRIVEEASE_DB_PASSWORD`, `PORT`.

## Troubleshooting
| Message | Fix |
|---------|-----|
| `No suitable driver` / driver missing | put `mysql-connector-j-*.jar` in `lib/` |
| `Access denied for user` | fix `db.user` / `db.password` in `config.properties` |
| `Communications link failure` | MySQL isn't running, or the port in `db.url` is wrong |
| `Address already in use` | change `port` in `config.properties` |
