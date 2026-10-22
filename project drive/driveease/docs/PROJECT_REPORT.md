# DriveEase — Mini Project Design Notes

## 1. Problem statement

Manual car rental management can make it difficult to check vehicle details, track reservations, validate rental dates, and prevent two customers from reserving the same car at overlapping times. DriveEase provides a small web-based system for customers to browse available cars, register/log in, create and view bookings, cancel future bookings, and manage account details.

## 2. Objectives

- Provide a clear interface to browse and sort rental cars.
- Store user, vehicle, booking, and session records in MySQL.
- Validate user input on the Java backend rather than trusting browser values.
- Calculate booking cost on the server using price per day and rental duration.
- Reject past dates, invalid date ranges, and overlapping confirmed bookings.
- Demonstrate core Java topics from the course: inheritance, collections, exceptions, JDBC, GUI interaction, and multithreading.

## 3. Functional requirements

1. A visitor can browse active cars and car locations.
2. A visitor can create an account and log in.
3. An authenticated user can see their dashboard and profile.
4. An authenticated user can create a booking for a valid car/date/location selection.
5. The system computes the price and rejects conflicting confirmed bookings.
6. A user can list and view only their own bookings.
7. A user can cancel an eligible future booking.
8. A user can update profile information or change their password.
9. A user can log out; the session is invalidated in the database.

## 4. Non-functional requirements

- Use prepared statements for SQL containing user input.
- Hash passwords before storing them.
- Store only hashed session tokens in MySQL.
- Use a transaction and lock the selected car row while checking/creating a booking.
- Return clear HTTP status codes and error messages for invalid requests.
- Keep the demo server bound to localhost.

## 5. Architecture

The browser loads the pages from `frontend/` through the same Java HTTP server that handles `/api/...` JSON requests. `Main` registers routes and uses a fixed thread pool. API handlers validate requests and use `Ctx` to access a per-request JDBC connection. `Db` initializes the schema and seeds sample cars. `Models` converts database records into the JSON structure used by the frontend.

## 6. OOP and Java concepts to explain

### Inheritance

`Models.Car` extends the abstract `Vehicle` class. `Vehicle` contains shared vehicle identity/description data (`id`, `name`, `category`, `description`), while `Car` adds car-specific fields such as seats, transmission, fuel, daily price, and popularity. The `ApiException` class also has specialized subclasses for validation errors, missing resources, and conflicts.

### Collections

`ArrayList`/`List` are used to accumulate cars and bookings; `Map`/`LinkedHashMap` are used to construct JSON objects; `ConcurrentHashMap` stores login-attempt tracking across concurrent requests.

### Exception handling

Validation and route handlers throw `ApiException` subclasses; `Main` catches them and returns an HTTP error response. SQL exceptions are logged on the server and mapped to a safe generic error for the browser. Try-with-resources closes JDBC statements and result sets.

### JDBC CRUD

The app performs INSERT, SELECT, UPDATE, and DELETE operations using `PreparedStatement`. Examples include account creation/reading/profile changes, booking creation/listing/cancellation, and session creation/cleanup.

### GUI

The user interface is a browser GUI implemented using HTML, CSS, and JavaScript. The front end sends JSON requests to the Java API and renders responses. Confirm with the faculty that this satisfies the GUI rubric if they specifically expect Java Swing or JavaFX.

### Threading

`Main` configures `HttpServer` with `Executors.newFixedThreadPool(16)`, allowing multiple requests to be handled concurrently by worker threads. The booking transaction locks the corresponding car row while validating availability.

## 7. Booking algorithm

1. Read the authenticated user's car ID, pickup/return dates, and locations.
2. Validate required values and ensure pickup is today or later and return is after pickup.
3. Reject rentals longer than the configured 30-day limit.
4. Begin a JDBC transaction and lock the selected active car row.
5. Search for confirmed bookings whose date range overlaps the requested date range.
6. If a conflict exists, roll back and return a conflict response.
7. Otherwise calculate `days × price_per_day`, insert a confirmed booking, and commit.
8. Return the saved booking to the browser.

The overlap condition used is `existing.pickup_date < requested.return_date AND existing.return_date > requested.pickup_date`. This models rental intervals as ending at the return date, so two bookings can be adjacent without overlapping.

## 8. Testing and limitations

Run `mvn clean test package`, configure MySQL, then follow the checklist in `README.md`. Tests that depend on a live MySQL database must be performed locally. This project is a lab/demo application: it uses a built-in HTTP server, local-only binding, and has not been security-reviewed for public production deployment.
