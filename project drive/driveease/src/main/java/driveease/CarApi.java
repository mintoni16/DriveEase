package driveease;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** /api/cars, /api/cars/{id}, /api/locations  (all public) */
final class CarApi {
    static final List<String> LOCATIONS = List.of("Pune", "Mumbai", "Nashik", "Bengaluru");

    /** ORDER BY fragments come only from this whitelist, never from user input. */
    private static final Map<String, String> SORTS = Map.of(
            "recommended", "popular DESC, id ASC",
            "price_asc", "price_per_day ASC, id ASC",
            "price_desc", "price_per_day DESC, id ASC",
            "name", "name ASC");

    private CarApi() {}

    static void list(Ctx c) throws IOException, SQLException {
        String category = c.query.getOrDefault("category", "").strip();
        String sortKey = c.query.getOrDefault("sort", "recommended");
        String orderBy = SORTS.get(sortKey);
        if (orderBy == null) throw ApiException.badRequest("Invalid sort option");

        boolean filter = !category.isEmpty()
                && !category.equalsIgnoreCase("all") && !category.equalsIgnoreCase("all cars");
        String sql = "SELECT * FROM cars WHERE active = 1" + (filter ? " AND category = ?" : "")
                + " ORDER BY " + orderBy;

        List<Object> cars = new ArrayList<>();
        try (PreparedStatement ps = c.db().prepareStatement(sql)) {
            if (filter) ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) cars.add(Db.readCar(rs).toMap());
            }
        }
        c.json(200, Map.of("cars", cars));
    }

    static void get(Ctx c) throws IOException, SQLException {
        int id = parseId(c.groups.get(0), "Car not found");
        try (PreparedStatement ps = c.db().prepareStatement("SELECT * FROM cars WHERE id = ? AND active = 1")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw ApiException.notFound("Car not found");
                c.json(200, Map.of("car", Db.readCar(rs).toMap()));
            }
        }
    }

    static void locations(Ctx c) throws IOException {
        c.json(200, Map.of("locations", LOCATIONS));
    }

    static int parseId(String text, String notFoundMessage) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw ApiException.notFound(notFoundMessage);
        }
    }
}
