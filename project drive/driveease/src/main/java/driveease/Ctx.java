package driveease;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Everything a handler needs for one request: input helpers, output helpers, a lazy DB connection. */
final class Ctx implements AutoCloseable {
    private static final int MAX_BODY = 1_000_000;

    final HttpExchange ex;
    final String method;
    final String path;
    final Map<String, String> query = new HashMap<>();
    List<String> groups = List.of();      // captured path segments, e.g. the {id} in /api/bookings/{id}
    Models.User user;                     // set for routes that require login

    private Connection connection;
    private Map<String, Object> jsonBody;

    Ctx(HttpExchange ex) {
        this.ex = ex;
        this.method = ex.getRequestMethod().toUpperCase();
        this.path = ex.getRequestURI().getPath();
        String raw = ex.getRequestURI().getRawQuery();
        if (raw != null) {
            for (String pair : raw.split("&")) {
                if (pair.isEmpty()) continue;
                int eq = pair.indexOf('=');
                String k = eq < 0 ? pair : pair.substring(0, eq);
                String v = eq < 0 ? "" : pair.substring(eq + 1);
                query.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
            }
        }
    }

    // ------------------------------------------------------------ input
    /** One DB connection per request, opened only if a handler needs it. */
    Connection db() throws SQLException {
        if (connection == null) connection = Db.open();
        return connection;
    }

    boolean isJson() {
        String type = ex.getRequestHeaders().getFirst("Content-Type");
        return type != null && type.toLowerCase().startsWith("application/json");
    }

    /** The request body as a JSON object; 400 if it is missing or malformed. */
    @SuppressWarnings("unchecked")
    Map<String, Object> body() throws IOException {
        if (jsonBody != null) return jsonBody;
        byte[] bytes = ex.getRequestBody().readNBytes(MAX_BODY + 1);
        if (bytes.length > MAX_BODY) throw new ApiException(413, "Request body too large");
        try {
            Object parsed = Json.parse(new String(bytes, StandardCharsets.UTF_8));
            if (!(parsed instanceof Map)) throw ApiException.badRequest("Invalid JSON body");
            jsonBody = (Map<String, Object>) parsed;
            return jsonBody;
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Invalid JSON body");
        }
    }

    String cookie(String name) {
        List<String> headers = ex.getRequestHeaders().get("Cookie");
        if (headers == null) return null;
        for (String header : headers) {
            for (String part : header.split(";")) {
                String p = part.strip();
                if (p.startsWith(name + "=")) return p.substring(name.length() + 1);
            }
        }
        return null;
    }

    String remoteIp() {
        return ex.getRemoteAddress().getAddress().getHostAddress();
    }

    // ------------------------------------------------------------ output
    void addHeader(String name, String value) {
        ex.getResponseHeaders().add(name, value);
    }

    void json(int status, Object body) throws IOException {
        bytes(status, "application/json; charset=utf-8", Json.stringify(body).getBytes(StandardCharsets.UTF_8));
    }

    void redirect(String location) throws IOException {
        ex.getResponseHeaders().set("Location", location);
        ex.sendResponseHeaders(302, -1);
    }

    void bytes(int status, String contentType, byte[] data) throws IOException {
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        if (method.equals("HEAD")) {
            ex.getResponseHeaders().set("Content-Length", String.valueOf(data.length));
            ex.sendResponseHeaders(status, -1);
            return;
        }
        ex.sendResponseHeaders(status, data.length == 0 ? -1 : data.length);
        if (data.length > 0) {
            try (var out = ex.getResponseBody()) {
                out.write(data);
            }
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            try { connection.close(); } catch (SQLException ignored) { /* nothing to do */ }
        }
        ex.close();
    }
}
