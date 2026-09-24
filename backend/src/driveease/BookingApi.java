package driveease;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** /api/bookings (create, list, view, cancel) and /api/dashboard. All require login. */
final class BookingApi {
    static final int MAX_RENTAL_DAYS = 30;

    private static final String BOOKING_SELECT =
            "SELECT b.id, b.car_id, b.pickup_date, b.return_date, b.pickup_location, b.drop_location, "
            + "b.total_price, b.status, DATE_FORMAT(b.created_at, '%Y-%m-%d %H:%i:%s') AS created_at, "
            + "c.name, c.category, c.description, c.seats, c.transmission, c.fuel, c.price_per_day, c.popular "
            + "FROM bookings b JOIN cars c ON c.id = b.car_id ";

    private BookingApi() {}

    // ------------------------------------------------------------ create
    static void create(Ctx c) throws IOException, SQLException {
        Map<String, Object> data = c.body();
        int carId = Validate.toInt(data.get("carId"), "carId is required");
        LocalDate pickup = Validate.parseDate(data.get("pickupDate"), "Pickup date");
        LocalDate ret = Validate.parseDate(data.get("returnDate"), "Return date");
        Object pickupLoc = data.get("pickupLocation");
        Object dropLoc = data.get("dropLocation");
        if (!CarApi.LOCATIONS.contains(pickupLoc)) throw ApiException.badRequest("Please choose a valid pickup location");
        if (!CarApi.LOCATIONS.contains(dropLoc)) throw ApiException.badRequest("Please choose a valid drop-off location");

        if (pickup.isBefore(LocalDate.now())) throw ApiException.badRequest("Pickup date cannot be in the past");
        long days = ChronoUnit.DAYS.between(pickup, ret);
        if (days < 1) throw ApiException.badRequest("Return date must be after the pickup date");
        if (days > MAX_RENTAL_DAYS)
            throw ApiException.badRequest("Bookings are limited to " + MAX_RENTAL_DAYS + " days");

        Connection db = c.db();
        int bookingId;
        db.setAutoCommit(false);
        try {
            // Lock this car's row: two people booking the same car queue up here, so the
            // availability check below can't be fooled by a simultaneous booking.
            int pricePerDay;
            try (PreparedStatement ps = db.prepareStatement(
                    "SELECT price_per_day FROM cars WHERE id = ? AND active = 1 FOR UPDATE")) {
                ps.setInt(1, carId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw ApiException.notFound("Car not found");
                    pricePerDay = rs.getInt(1);
                }
            }

            try (PreparedStatement ps = db.prepareStatement(
                    "SELECT 1 FROM bookings WHERE car_id = ? AND status = 'confirmed' "
                    + "AND pickup_date < ? AND return_date > ? LIMIT 1")) {
                ps.setInt(1, carId);
                ps.setObject(2, ret);
                ps.setObject(3, pickup);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) throw ApiException.conflict(
                            "This car is already booked for those dates. Please choose different dates.");
                }
            }

            int total = (int) days * pricePerDay;   // price is always computed here, never trusted from the browser
            try (PreparedStatement ps = db.prepareStatement(
                    "INSERT INTO bookings (user_id, car_id, pickup_date, return_date, pickup_location, "
                    + "drop_location, total_price, status, created_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, 'confirmed', UTC_TIMESTAMP())",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, c.user.id());
                ps.setInt(2, carId);
                ps.setObject(3, pickup);
                ps.setObject(4, ret);
                ps.setString(5, (String) pickupLoc);
                ps.setString(6, (String) dropLoc);
                ps.setInt(7, total);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    bookingId = keys.getInt(1);
                }
            }
            db.commit();
        } catch (Throwable t) {
            db.rollback();
            throw t;
        } finally {
            db.setAutoCommit(true);
        }

        c.json(201, Map.of("booking", loadOwn(db, bookingId, c.user.id()).toMap()));
    }

    // ------------------------------------------------------------ read
    static void list(Ctx c) throws IOException, SQLException {
        List<Models.Booking> all = loadAll(c.db(), c.user.id());   // newest pickup first

        List<Object> bookings = new ArrayList<>();
        List<Models.Booking> upcoming = new ArrayList<>();
        List<Object> past = new ArrayList<>();
        for (Models.Booking b : all) {
            bookings.add(b.toMap());
            if (b.effectiveStatus().equals("confirmed")) upcoming.add(b);
            else past.add(b.toMap());
        }
        upcoming.sort(Comparator.comparing(Models.Booking::pickupDate));   // soonest first

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("bookings", bookings);
        out.put("upcoming", upcoming.stream().map(Models.Booking::toMap).toList());
        out.put("past", past);
        c.json(200, out);
    }

    static void get(Ctx c) throws IOException, SQLException {
        int id = CarApi.parseId(c.groups.get(0), "Booking not found");
        c.json(200, Map.of("booking", loadOwn(c.db(), id, c.user.id()).toMap()));
    }

    // ------------------------------------------------------------ cancel
    static void cancel(Ctx c) throws IOException, SQLException {
        int id = CarApi.parseId(c.groups.get(0), "Booking not found");
        Connection db = c.db();
        Models.Booking b = loadOwn(db, id, c.user.id());

        if (b.status().equals("cancelled")) throw ApiException.conflict("This booking is already cancelled");
        if (b.effectiveStatus().equals("completed")) throw ApiException.conflict("Completed bookings can't be cancelled");
        if (!b.pickupDate().isAfter(LocalDate.now()))
            throw ApiException.conflict("Bookings can only be cancelled before the pickup date");

        try (PreparedStatement ps = db.prepareStatement(
                "UPDATE bookings SET status = 'cancelled' WHERE id = ? AND user_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, c.user.id());
            ps.executeUpdate();
        }
        c.json(200, Map.of("booking", loadOwn(db, id, c.user.id()).toMap()));
    }

    // ------------------------------------------------------------ dashboard
    static void dashboard(Ctx c) throws IOException, SQLException {
        Connection db = c.db();
        List<Models.Booking> all = loadAll(db, c.user.id());

        int active = 0, completed = 0, cancelled = 0;
        List<Models.Booking> upcoming = new ArrayList<>();
        for (Models.Booking b : all) {
            switch (b.effectiveStatus()) {
                case "confirmed" -> { active++; upcoming.add(b); }
                case "completed" -> completed++;
                case "cancelled" -> cancelled++;
                default -> { }
            }
        }
        upcoming.sort(Comparator.comparing(Models.Booking::pickupDate));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", all.size());
        stats.put("active", active);
        stats.put("completed", completed);
        stats.put("cancelled", cancelled);

        List<Object> featured = new ArrayList<>();
        try (PreparedStatement ps = db.prepareStatement(
                "SELECT * FROM cars WHERE active = 1 ORDER BY popular DESC, id ASC LIMIT 3");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) featured.add(Db.readCar(rs).toMap());
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("user", c.user.toMap());
        out.put("stats", stats);
        out.put("currentBooking", upcoming.isEmpty() ? null : upcoming.get(0).toMap());
        out.put("featuredCars", featured);
        c.json(200, out);
    }

    // ------------------------------------------------------------ queries
    private static List<Models.Booking> loadAll(Connection db, int userId) throws SQLException {
        List<Models.Booking> list = new ArrayList<>();
        try (PreparedStatement ps = db.prepareStatement(
                BOOKING_SELECT + "WHERE b.user_id = ? ORDER BY b.pickup_date DESC, b.id DESC")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(read(rs));
            }
        }
        return list;
    }

    /** Only returns the booking if it belongs to this user - otherwise 404 (we don't reveal it exists). */
    private static Models.Booking loadOwn(Connection db, int id, int userId) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(BOOKING_SELECT + "WHERE b.id = ? AND b.user_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw ApiException.notFound("Booking not found");
                return read(rs);
            }
        }
    }

    private static Models.Booking read(ResultSet rs) throws SQLException {
        Models.Car car = new Models.Car(rs.getInt("car_id"), rs.getString("name"), rs.getString("category"),
                rs.getString("description"), rs.getInt("seats"), rs.getString("transmission"),
                rs.getString("fuel"), rs.getInt("price_per_day"), rs.getInt("popular") == 1);
        return new Models.Booking(rs.getInt("id"), car,
                rs.getObject("pickup_date", LocalDate.class), rs.getObject("return_date", LocalDate.class),
                rs.getString("pickup_location"), rs.getString("drop_location"),
                rs.getInt("total_price"), rs.getString("status"), rs.getString("created_at"));
    }
}
