package driveease;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DriveEase backend: JDK built-in HTTP server + JDBC (MySQL). No frameworks.
 * Serves the frontend and the JSON API from one address.
 */
public final class Main {

    @FunctionalInterface
    interface Handler {
        void handle(Ctx c) throws Exception;
    }

    private record Route(String method, Pattern pattern, boolean requiresLogin, Handler handler) {}

    private static final List<Route> ROUTES = new ArrayList<>();

    private static void route(String method, String regex, boolean requiresLogin, Handler handler) {
        ROUTES.add(new Route(method, Pattern.compile(regex), requiresLogin, handler));
    }

    private static void registerRoutes() {
        // accounts
        route("POST", "/api/register", false, UserApi::register);
        route("POST", "/api/login", false, UserApi::login);
        route("POST", "/api/logout", false, UserApi::logout);
        route("GET", "/api/me", true, UserApi::me);
        route("PUT", "/api/me", true, UserApi::updateMe);
        route("PUT", "/api/me/password", true, UserApi::changePassword);
        // cars (public)
        route("GET", "/api/cars", false, CarApi::list);
        route("GET", "/api/cars/(\\d{1,9})", false, CarApi::get);
        route("GET", "/api/locations", false, CarApi::locations);
        // bookings
        route("POST", "/api/bookings", true, BookingApi::create);
        route("GET", "/api/bookings", true, BookingApi::list);
        route("GET", "/api/bookings/(\\d{1,9})", true, BookingApi::get);
        route("POST", "/api/bookings/(\\d{1,9})/cancel", true, BookingApi::cancel);
        route("GET", "/api/dashboard", true, BookingApi::dashboard);
    }

    /** Routes one request. Exposed (package-private) so it can be tested without a database. */
    static void dispatch(Ctx c) throws Exception {
        if (!c.path.startsWith("/api/")) {
            StaticFiles.serve(c);
            return;
        }

        // Simple CSRF defence: state-changing calls must be JSON, which cross-site HTML forms can't send.
        if (List.of("POST", "PUT", "PATCH", "DELETE").contains(c.method) && !c.isJson())
            throw new ApiException(415, "Content-Type must be application/json");

        boolean pathMatched = false;
        for (Route r : ROUTES) {
            Matcher m = r.pattern().matcher(c.path);
            if (!m.matches()) continue;
            pathMatched = true;
            if (!r.method().equals(c.method)) continue;

            List<String> groups = new ArrayList<>();
            for (int i = 1; i <= m.groupCount(); i++) groups.add(m.group(i));
            c.groups = groups;

            if (r.requiresLogin()) {
                c.user = Auth.currentUser(c);
                if (c.user == null) throw new ApiException(401, "Please log in to continue");
            }
            r.handler().handle(c);
            return;
        }
        throw pathMatched ? new ApiException(405, "Method not allowed") : ApiException.notFound("Not found");
    }

    static HttpServer createServer(int port) throws IOException {
        registerRoutes();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", exchange -> {
            try (Ctx c = new Ctx(exchange)) {
                try {
                    dispatch(c);
                } catch (ApiException e) {
                    c.json(e.status, Map.of("error", e.getMessage()));
                } catch (SQLException e) {
                    System.err.println("Database error on " + c.method + " " + c.path + ": " + e);
                    c.json(500, Map.of("error", "Database error. Is MySQL running?"));
                } catch (Throwable t) {
                    t.printStackTrace();
                    c.json(500, Map.of("error", "Internal server error"));
                }
            } catch (Throwable t) {
                // the response could not be written (client went away) - nothing more to do
            }
        });
        server.setExecutor(Executors.newFixedThreadPool(16));
        return server;
    }

    public static void main(String[] args) {
        try {
            Config.load();
        } catch (Exception e) {
            System.err.println("Could not read config.properties: " + e.getMessage());
            System.exit(1);
        }

        try {
            Db.init();
            System.out.println("MySQL connected, tables ready.");
        } catch (SQLException e) {
            System.err.println("\nCould not connect to MySQL: " + e.getMessage());
            System.err.println("Check that MySQL is running and that db.user / db.password in config.properties are right.");
            if (String.valueOf(e.getMessage()).contains("No suitable driver"))
                System.err.println("The MySQL driver jar is missing - put mysql-connector-j-*.jar in the lib/ folder.");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Could not read schema file " + Config.schemaFile + ": " + e.getMessage());
            System.exit(1);
        }

        try {
            HttpServer server = createServer(Config.port);
            server.start();
            System.out.println("DriveEase running at http://localhost:" + Config.port);
        } catch (IOException e) {
            System.err.println("Could not start server on port " + Config.port + ": " + e.getMessage());
            System.exit(1);
        }
    }
}
