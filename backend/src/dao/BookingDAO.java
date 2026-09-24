import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookingDAO {

    public int create(int userId, int carId, String pickup, String returnDate, double total) throws SQLException {
        String sql = "INSERT INTO bookings(user_id,car_id,pickup_date,return_date,total_price,status) VALUES(?,?,?,?,?,'CONFIRMED')";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setInt(1, userId);
            p.setInt(2, carId);
            p.setDate(3, Date.valueOf(pickup));
            p.setDate(4, Date.valueOf(returnDate));
            p.setDouble(5, total);
            p.executeUpdate();
            try (ResultSet r = p.getGeneratedKeys()) {
                if (r.next()) return r.getInt(1);
            }
        }
        return 0;
    }

    public List<Booking> findByUser(int userId) throws SQLException {
        String sql = """
            SELECT b.*, c.name AS car_name
            FROM bookings b JOIN cars c ON b.car_id=c.id
            WHERE b.user_id=? ORDER BY b.id DESC
            """;
        List<Booking> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, userId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) list.add(map(r));
            }
        }
        return list;
    }

    public boolean cancel(int id) throws SQLException {
        String sql = "UPDATE bookings SET status='CANCELLED' WHERE id=? AND status='CONFIRMED'";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, id);
            return p.executeUpdate() > 0;
        }
    }

    public boolean hasOverlap(int carId, String pickup, String returnDate) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM bookings
            WHERE car_id=? AND status='CONFIRMED'
            AND pickup_date < ? AND return_date > ?
            """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, carId);
            p.setDate(2, Date.valueOf(returnDate));
            p.setDate(3, Date.valueOf(pickup));
            try (ResultSet r = p.executeQuery()) {
                r.next();
                return r.getInt(1) > 0;
            }
        }
    }

    private Booking map(ResultSet r) throws SQLException {
        return new Booking(
                r.getInt("id"),
                r.getInt("user_id"),
                r.getInt("car_id"),
                r.getString("car_name"),
                r.getDate("pickup_date").toString(),
                r.getDate("return_date").toString(),
                r.getDouble("total_price"),
                r.getString("status"),
                r.getTimestamp("created_at").toLocalDateTime().toString()
        );
    }
}
