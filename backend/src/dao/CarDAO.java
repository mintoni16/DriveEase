import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CarDAO {

    public List<Car> findAll() throws SQLException {
        return query("SELECT * FROM cars ORDER BY id");
    }

    public List<Car> findAvailable() throws SQLException {
        return query("SELECT * FROM cars WHERE available=TRUE ORDER BY id");
    }

    public Car findById(int id) throws SQLException {
        String sql = "SELECT * FROM cars WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) return map(r);
            }
        }
        return null;
    }

    private List<Car> query(String sql) throws SQLException {
        List<Car> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql);
             ResultSet r = p.executeQuery()) {
            while (r.next()) list.add(map(r));
        }
        return list;
    }

    private Car map(ResultSet r) throws SQLException {
        return new Car(
                r.getInt("id"),
                r.getString("name"),
                r.getString("brand"),
                r.getString("type"),
                r.getDouble("price_per_day"),
                r.getInt("seats"),
                r.getString("transmission"),
                r.getBoolean("available")
        );
    }
}
