package driveease;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** Cookie sessions stored in MySQL, plus a small login-attempt throttle. */
final class Auth {
    static final String COOKIE = "de_session";
    private static final int SESSION_SECONDS = 24 * 3600;        // browser-session login: valid up to 1 day
    private static final int REMEMBER_SECONDS = 30 * 24 * 3600;  // "Remember me": 30 days

    private Auth() {}

    /** The logged-in user for this request, or null. */
    static Models.User currentUser(Ctx c) throws SQLException {
        String token = c.cookie(COOKIE);
        if (token == null || token.isBlank()) return null;
        String sql = "SELECT u.id, u.name, u.email, u.phone, "
                + "DATE_FORMAT(u.created_at, '%Y-%m-%d %H:%i:%s') AS created_at "
                + "FROM sessions s JOIN users u ON u.id = s.user_id "
                + "WHERE s.token_hash = ? AND s.expires_at > UTC_TIMESTAMP()";
        try (PreparedStatement ps = c.db().prepareStatement(sql)) {
            ps.setString(1, Security.sha256Hex(token));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Db.readUser(rs) : null;
            }
        }
    }

    /** Starts a fresh session (replacing any existing one) and sets the cookie. */
    static void startSession(Ctx c, int userId, boolean remember) throws SQLException {
        deleteCurrentToken(c);
        Connection db = c.db();
        try (PreparedStatement ps = db.prepareStatement("DELETE FROM sessions WHERE expires_at < UTC_TIMESTAMP()")) {
            ps.executeUpdate();
        }
        String token = Security.newToken();
        int seconds = remember ? REMEMBER_SECONDS : SESSION_SECONDS;
        try (PreparedStatement ps = db.prepareStatement(
                "INSERT INTO sessions (token_hash, user_id, expires_at) "
                        + "VALUES (?, ?, DATE_ADD(UTC_TIMESTAMP(), INTERVAL ? SECOND))")) {
            ps.setString(1, Security.sha256Hex(token));
            ps.setInt(2, userId);
            ps.setInt(3, seconds);
            ps.executeUpdate();
        }
        // No Max-Age unless "remember me": the cookie then disappears when the browser closes.
        c.addHeader("Set-Cookie", cookieHeader(token, remember ? REMEMBER_SECONDS : -1));
    }

    /** Deletes the current session (if any) and clears the cookie. */
    static void endSession(Ctx c) throws SQLException {
        deleteCurrentToken(c);
        c.addHeader("Set-Cookie", cookieHeader("", 0));
    }

    private static void deleteCurrentToken(Ctx c) throws SQLException {
        String token = c.cookie(COOKIE);
        if (token == null || token.isBlank()) return;
        try (PreparedStatement ps = c.db().prepareStatement("DELETE FROM sessions WHERE token_hash = ?")) {
            ps.setString(1, Security.sha256Hex(token));
            ps.executeUpdate();
        }
    }

    private static String cookieHeader(String value, int maxAge) {
        StringBuilder sb = new StringBuilder(COOKIE).append('=').append(value)
                .append("; Path=/; HttpOnly; SameSite=Lax");
        if (maxAge >= 0) sb.append("; Max-Age=").append(maxAge);
        if (Config.production) sb.append("; Secure");
        return sb.toString();
    }

    // ------------------------------------------------------------ login throttle (in memory)
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 5 * 60 * 1000L;
    private static final ConcurrentHashMap<String, List<Long>> FAILURES = new ConcurrentHashMap<>();

    private static String key(Ctx c, String email) { return c.remoteIp() + "|" + email; }

    static void checkThrottle(Ctx c, String email) {
        List<Long> attempts = FAILURES.get(key(c, email));
        if (attempts == null) return;
        long now = System.currentTimeMillis();
        synchronized (attempts) {
            attempts.removeIf(t -> now - t > WINDOW_MS);
            if (attempts.size() >= MAX_ATTEMPTS)
                throw new ApiException(429, "Too many failed attempts. Please try again in a few minutes.");
        }
    }

    static void recordFailure(Ctx c, String email) {
        List<Long> attempts = FAILURES.computeIfAbsent(key(c, email), k -> new ArrayList<>());
        synchronized (attempts) {
            attempts.add(System.currentTimeMillis());
        }
    }

    static void clearFailures(Ctx c, String email) {
        FAILURES.remove(key(c, email));
    }
}
