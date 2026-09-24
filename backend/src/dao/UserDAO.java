import java.sql.*;

public class UserDAO {

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT id,name,email,phone,password,created_at FROM users WHERE email=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, email);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) return map(r);
            }
        }
        return null;
    }

    public User findById(int id) throws SQLException {
        String sql = "SELECT id,name,email,phone,password,created_at FROM users WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, id);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) return map(r);
            }
        }
        return null;
    }

    public int create(User u) throws SQLException {
        String sql = "INSERT INTO users(name,email,phone,password) VALUES(?,?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, u.getName());
            p.setString(2, u.getEmail());
            p.setString(3, u.getPhone());
            p.setString(4, u.getPassword());
            p.executeUpdate();
            try (ResultSet r = p.getGeneratedKeys()) {
                if (r.next()) return r.getInt(1);
            }
        }
        return 0;
    }

    public boolean update(User u) throws SQLException {
        String sql = "UPDATE users SET name=?, phone=? WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, u.getName());
            p.setString(2, u.getPhone());
            p.setInt(3, u.getId());
            return p.executeUpdate() > 0;
        }
    }

    private User map(ResultSet r) throws SQLException {
        Timestamp ts = r.getTimestamp("created_at");
        return new User(
                r.getInt("id"),
                r.getString("name"),
                r.getString("email"),
                r.getString("phone"),
                r.getString("password"),
                ts == null ? "" : ts.toLocalDateTime().toString()
        );
    }
}
