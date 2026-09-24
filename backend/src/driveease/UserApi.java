package driveease;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.Map;

/** /api/register, /api/login, /api/logout, /api/me, /api/me/password */
final class UserApi {
    private UserApi() {}

    static void register(Ctx c) throws IOException, SQLException {
        Map<String, Object> data = c.body();
        String name = Validate.cleanStr(data.get("name"), "Name", 2, 80);
        String email = Validate.cleanEmail(data.get("email"));
        String phone = Validate.cleanPhone(data.get("phone"));
        Object password = data.get("password");
        Validate.checkPassword(password);

        Connection db = c.db();
        if (emailTaken(db, email, -1)) throw ApiException.conflict("An account with this email already exists");

        int id;
        try (PreparedStatement ps = db.prepareStatement(
                "INSERT INTO users (name, email, phone, password_hash, created_at) VALUES (?, ?, ?, ?, UTC_TIMESTAMP())",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setString(4, Security.hashPassword((String) password));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                id = keys.getInt(1);
            }
        } catch (SQLIntegrityConstraintViolationException e) {   // two people registering at once
            throw ApiException.conflict("An account with this email already exists");
        }

        Auth.startSession(c, id, false);
        c.json(201, Map.of("user", loadUser(db, id).toMap()));
    }

    static void login(Ctx c) throws IOException, SQLException {
        Map<String, Object> data = c.body();
        String email = Validate.cleanEmail(data.get("email"));
        if (!(data.get("password") instanceof String password) || password.isEmpty())
            throw ApiException.badRequest("Password is required");

        Auth.checkThrottle(c, email);

        Models.User user = null;
        String storedHash = Security.DUMMY_HASH;
        try (PreparedStatement ps = c.db().prepareStatement(
                "SELECT " + Db.USER_COLUMNS + ", password_hash FROM users WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user = Db.readUser(rs);
                    storedHash = rs.getString("password_hash");
                }
            }
        }
        boolean ok = Security.verifyPassword(password, storedHash);   // runs even for unknown emails
        if (user == null || !ok) {
            Auth.recordFailure(c, email);
            throw new ApiException(401, "Incorrect email or password");
        }

        Auth.clearFailures(c, email);
        Auth.startSession(c, user.id(), Boolean.TRUE.equals(data.get("remember")));
        c.json(200, Map.of("user", user.toMap()));
    }

    static void logout(Ctx c) throws IOException, SQLException {
        Auth.endSession(c);
        c.json(200, Map.of("ok", true));
    }

    static void me(Ctx c) throws IOException {
        c.json(200, Map.of("user", c.user.toMap()));
    }

    static void updateMe(Ctx c) throws IOException, SQLException {
        Map<String, Object> data = c.body();
        String name = Validate.cleanStr(data.getOrDefault("name", c.user.name()), "Name", 2, 80);
        String email = Validate.cleanEmail(data.getOrDefault("email", c.user.email()));
        String phone = data.containsKey("phone") ? Validate.cleanPhone(data.get("phone")) : c.user.phone();

        Connection db = c.db();
        if (emailTaken(db, email, c.user.id()))
            throw ApiException.conflict("That email is already used by another account");

        try (PreparedStatement ps = db.prepareStatement(
                "UPDATE users SET name = ?, email = ?, phone = ? WHERE id = ?")) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setInt(4, c.user.id());
            ps.executeUpdate();
        }
        c.json(200, Map.of("user", loadUser(db, c.user.id()).toMap()));
    }

    static void changePassword(Ctx c) throws IOException, SQLException {
        Map<String, Object> data = c.body();
        Connection db = c.db();

        String storedHash;
        try (PreparedStatement ps = db.prepareStatement("SELECT password_hash FROM users WHERE id = ?")) {
            ps.setInt(1, c.user.id());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                storedHash = rs.getString(1);
            }
        }
        if (!(data.get("currentPassword") instanceof String current) || !Security.verifyPassword(current, storedHash))
            throw new ApiException(403, "Current password is incorrect");

        Object newPassword = data.get("newPassword");
        Validate.checkPassword(newPassword);
        if (newPassword.equals(current))
            throw ApiException.badRequest("New password must be different from the current one");

        try (PreparedStatement ps = db.prepareStatement("UPDATE users SET password_hash = ? WHERE id = ?")) {
            ps.setString(1, Security.hashPassword((String) newPassword));
            ps.setInt(2, c.user.id());
            ps.executeUpdate();
        }
        c.json(200, Map.of("ok", true));
    }

    // ------------------------------------------------------------ helpers
    private static boolean emailTaken(Connection db, String email, int exceptUserId) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("SELECT 1 FROM users WHERE email = ? AND id <> ?")) {
            ps.setString(1, email);
            ps.setInt(2, exceptUserId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static Models.User loadUser(Connection db, int id) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("SELECT " + Db.USER_COLUMNS + " FROM users WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return Db.readUser(rs);
            }
        }
    }
}
