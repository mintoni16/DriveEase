# DriveEase — Car Rental Management System

DriveEase is a Java + MySQL lab mini project with a browser-based HTML/CSS/JavaScript interface. The Java backend uses the JDK's built-in HTTP server and JDBC; it does not require Spring Boot.

## Features

- Account registration, login/logout, password change, and profile updates
- Car listing, filtering, sorting, and featured cars
- Booking creation, viewing, cancellation, and dashboard statistics
- Date validation and overlapping-booking checks
- Cookie-based server-side sessions stored in MySQL
- Prepared statements, transactions, Java collections, exception handling, inheritance, and a fixed thread pool

## Requirements

- JDK 17 or later
- MySQL Server 8.0 or later
- Apache Maven 3.8 or later (for downloading the JDBC dependency, running tests, and packaging)

## Run in VS Code (Windows)

1. Extract the complete `driveease` folder.
2. Start MySQL Server and ensure your MySQL user can create/use the `driveease` database and create tables.
3. Copy `config.properties.example` to `config.properties`.
4. Open `config.properties` and set `db.user` and `db.password` to your local MySQL credentials. Do not commit or share your real password.
5. Open the project folder (the folder containing `pom.xml`) in VS Code.
6. In the integrated terminal, run:

   ```powershell
   mvn clean test package
   ```

7. Start the application:

   ```powershell
   java -jar target/driveease-1.0.0.jar
   ```

8. Open <http://localhost:5000> in your browser.

The application reads `database/schema.sql` during startup, creates the tables, and seeds sample cars if the `cars` table is empty. The JDBC URL creates the database if your configured MySQL user has permission. Keep the terminal open while using the website; press `Ctrl+C` to stop the server.

### If Maven is not installed

Install Maven and reopen the VS Code terminal. The MySQL Connector/J library is declared in `pom.xml`, so you do not need to download a JAR manually. If you choose not to use Maven, you must separately add MySQL Connector/J to the runtime classpath.

## Configuration

Settings may be placed in `config.properties`; environment variables override them.

| Property | Default/example | Purpose |
|---|---|---|
| `db.url` | local MySQL URL in the example | JDBC connection URL |
| `db.user` | `root` | MySQL username |
| `db.password` | set locally | MySQL password |
| `port` | `5000` | Local HTTP server port |
| `frontend.dir` | `frontend` | Browser files directory |
| `schema.file` | `database/schema.sql` | SQL table definitions |
| `production` | `false` | Enables the `Secure` session-cookie flag; requires HTTPS in a real HTTPS deployment |

The HTTP server intentionally binds to `127.0.0.1`, making this a local lab/demo application rather than a public deployment. Do not expose it directly to the internet without a proper deployment/security review.

## Project structure

```text
driveease/
├── database/schema.sql
├── docs/
│   ├── PROJECT_REPORT.md
│   ├── ER_DIAGRAM.mmd
│   └── BOOKING_FLOW.mmd
├── frontend/                 # .htm pages, style.css, script.js
├── src/main/java/driveease/  # Java backend
├── src/test/java/driveease/  # JUnit tests
├── config.properties.example
└── pom.xml
```

## Quick smoke-test checklist

After starting the server, test the following in a fresh browser session:

1. Open the home page and browse the seeded cars.
2. Register a new account with a valid email and a password of at least 8 characters.
3. Log out, then log in with that account.
4. Open the dashboard and profile page.
5. Create a booking for a future date range, then inspect it in My Bookings.
6. Try booking the same car for an overlapping date range; the second booking should be rejected.
7. Cancel a future booking, then confirm its status changes to cancelled.
8. Try invalid dates and invalid form values; the API should return readable validation errors.
9. Restart the server and confirm your account/bookings remain in MySQL.

Use a dedicated test account and test data. Do not use personal passwords.

## Rubric mapping

| Rubric concept | Where to demonstrate it |
|---|---|
| Problem analysis/design | `docs/PROJECT_REPORT.md`, ER diagram, booking-flow diagram |
| Inheritance | `Vehicle` base class and `Models.Car extends Vehicle` in `Models.java`; `ApiException` specialized subclasses |
| Collections | `List`, `Map`, `ArrayList`, and `ConcurrentHashMap` in API/auth code |
| Exception handling | `ApiException` hierarchy, validation exceptions, request error handling in `Main.java` |
| JDBC CRUD | SQL in `UserApi.java`, `CarApi.java`, `BookingApi.java`, and `Auth.java` |
| GUI | `frontend/` pages, `style.css`, and `script.js` (browser GUI) |
| Thread | Fixed-size request executor in `Main.java` |

**Important:** The supplied evaluation plan labels the Mini Project as 10 marks, but the listed Java-concept subitems add up to 7 of the 8 marks allocated to that section. Confirm the unlisted mark and whether the faculty accepts a browser-based GUI rather than Swing/JavaFX.
