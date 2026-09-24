DRIVEEASE - JAVA + MYSQL BACKEND
================================

This backend uses:
- Plain Java (Java 17+)
- MySQL
- JDBC
- Java's built-in HttpServer
- No Spring / Spring Boot
- No Maven

1. INSTALL
----------
Install:
- JDK 17 or newer
- MySQL Server

2. MYSQL SETUP
--------------
Open MySQL Workbench or the MySQL command line and run:

    database/driveease.sql

This creates the driveease database and the required tables.

3. MYSQL PASSWORD
-----------------
Open:

    src/DBConnection.java

Change:

    private static final String PASSWORD = "CHANGE_ME";

to your MySQL root password.

If your MySQL username is not root, change USER too.

4. MYSQL JDBC DRIVER
--------------------
Download MySQL Connector/J from the official MySQL website.

Put the JAR file in:

    lib/

For example:

    lib/mysql-connector-j-9.x.x.jar

5. COMPILE ON WINDOWS
---------------------
Open Command Prompt in the project folder.

Run:

    mkdir out
    javac -cp "lib\mysql-connector-j-9.x.x.jar" -d out src\*.java src\model\*.java src\dao\*.java src\handler\*.java src\util\*.java

Replace the JAR filename with your actual Connector/J filename.

Then run:

    java -cp "out;lib\mysql-connector-j-9.x.x.jar" Main

6. COMPILE ON MAC/LINUX
-----------------------
Run:

    mkdir -p out
    javac -cp "lib/mysql-connector-j-9.x.x.jar" -d out src/*.java src/model/*.java src/dao/*.java src/handler/*.java src/util/*.java

Then:

    java -cp "out:lib/mysql-connector-j-9.x.x.jar" Main

7. SERVER
---------
The server starts at:

    http://localhost:8080

API:
    POST /api/auth/register
    POST /api/auth/login
    GET  /api/auth/user/{id}

    GET  /api/cars
    GET  /api/cars/available
    GET  /api/cars/{id}

    POST /api/bookings
    GET  /api/bookings/user/{id}
    DELETE /api/bookings/{id}

    GET  /api/profile/{id}
    PUT  /api/profile/{id}

8. FRONTEND EXAMPLE
-------------------
Login:

fetch("http://localhost:8080/api/auth/login", {
    method: "POST",
    headers: {"Content-Type": "application/json"},
    body: JSON.stringify({
        email: email,
        password: password
    })
})
.then(r => r.json())
.then(user => {
    localStorage.setItem("userId", user.id);
    localStorage.setItem("userName", user.name);
    localStorage.setItem("userEmail", user.email);
    window.location.href = "dashboard.htm";
});

Register:

fetch("http://localhost:8080/api/auth/register", {
    method: "POST",
    headers: {"Content-Type": "application/json"},
    body: JSON.stringify({
        name: name,
        email: email,
        phone: phone,
        password: password
    })
});

Get cars:

fetch("http://localhost:8080/api/cars")
    .then(r => r.json())
    .then(cars => console.log(cars));

Create booking:

fetch("http://localhost:8080/api/bookings", {
    method: "POST",
    headers: {"Content-Type": "application/json"},
    body: JSON.stringify({
        userId: Number(localStorage.getItem("userId")),
        carId: carId,
        pickupDate: pickupDate,
        returnDate: returnDate
    })
});

9. IMPORTANT
------------
This is a student/project backend suitable for connecting to the DriveEase frontend.

For a production application, add:
- proper authentication/session or JWT
- password hashing
- authorization checks
- HTTPS
- stronger input validation
- admin authentication
- rate limiting
- database connection pooling

The current demo keeps the API simple so it can be connected easily to your HTML/CSS/JS frontend.
