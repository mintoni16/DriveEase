package driveease;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** MySQL access: opening connections, creating tables from schema.sql, seeding cars. */
final class Db {
    private Db() {}

    static Connection open() throws SQLException {
        return DriverManager.getConnection(Config.dbUrl, Config.dbUser, Config.dbPassword);
    }

    /** Creates the database/tables if missing and inserts the starting cars on a fresh database. */
    static void init() throws SQLException, IOException {
        try (Connection c = open()) {
            try (Statement st = c.createStatement()) {
                for (String sql : readStatements()) {
                    // The CREATE DATABASE / USE lines are only for manual runs; the JDBC URL already
                    // selects (and creates) the database.
                    String upper = sql.toUpperCase();
                    if (upper.startsWith("CREATE DATABASE") || upper.startsWith("USE ")) continue;
                    st.execute(sql);
                }
            }
            seedCars(c);
        }
    }

    private static List<String> readStatements() throws IOException {
        StringBuilder cleaned = new StringBuilder();
        for (String line : Files.readAllLines(Config.schemaFile)) {
            int comment = line.indexOf("--");
            if (comment >= 0) line = line.substring(0, comment);
            cleaned.append(line).append('\n');
        }
        List<String> statements = new ArrayList<>();
        for (String part : cleaned.toString().split(";")) {
            if (!part.isBlank()) statements.add(part.strip());
        }
        return statements;
    }

    private static void seedCars(Connection c) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM cars")) {
            rs.next();
            if (rs.getInt(1) > 0) return;
        }
        String sql = "INSERT INTO cars (name, category, description, seats, transmission, fuel, price_per_day, popular) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Object[][] cars = {
            {"Hyundai Creta", "SUV", "Comfortable SUV suitable for city drives and long journeys.", 5, "Automatic", "Petrol", 1500, 1},
            {"Honda City", "Sedan", "Refined sedan offering a smooth and comfortable driving experience.", 5, "Automatic", "Petrol", 1300, 0},
            {"Tata Altroz", "Hatchback", "Compact and practical car for everyday city travel.", 5, "Manual", "Petrol", 900, 0},
            {"Kia Seltos", "SUV", "Stylish SUV with a spacious interior and modern features.", 5, "Automatic", "Diesel", 1700, 0},
            {"Skoda Slavia", "Sedan", "Spacious sedan designed for comfortable longer journeys.", 5, "Automatic", "Petrol", 1600, 0},
            {"BMW 3 Series", "Luxury", "Premium sedan for a sophisticated and comfortable journey.", 5, "Automatic", "Petrol", 4500, 0},
        };
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (Object[] car : cars) {
                for (int i = 0; i < car.length; i++) ps.setObject(i + 1, car[i]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ---- row mappers shared by several handlers ----

    static final String USER_COLUMNS =
            "id, name, email, phone, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS created_at";

    static Models.User readUser(ResultSet rs) throws SQLException {
        return new Models.User(rs.getInt("id"), rs.getString("name"), rs.getString("email"),
                rs.getString("phone"), rs.getString("created_at"));
    }

    static Models.Car readCar(ResultSet rs) throws SQLException {
        return new Models.Car(rs.getInt("id"), rs.getString("name"), rs.getString("category"),
                rs.getString("description"), rs.getInt("seats"), rs.getString("transmission"),
                rs.getString("fuel"), rs.getInt("price_per_day"), rs.getInt("popular") == 1);
    }
}
