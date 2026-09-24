import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class AuthHandler implements HttpHandler {
    private final UserDAO dao = new UserDAO();

    @Override public void handle(HttpExchange ex) throws IOException {
        Main.cors(ex);
        try {
            String method = ex.getRequestMethod();
            if ("OPTIONS".equalsIgnoreCase(method)) { Main.send(ex, 204, ""); return; }

            String path = ex.getRequestURI().getPath();

            if ("POST".equalsIgnoreCase(method) && path.endsWith("/register")) {
                Map<String,String> d = Main.parseJson(Main.read(ex));
                String name = d.get("name"), email = d.get("email"), phone = d.getOrDefault("phone","");
                String password = d.get("password");
                if (blank(name) || blank(email) || blank(password)) {
                    Main.send(ex, 400, JsonUtil.error("Name, email and password are required."));
                    return;
                }
                if (dao.findByEmail(email) != null) {
                    Main.send(ex, 409, JsonUtil.error("Email already registered."));
                    return;
                }
                User u = new User(0,name,email,phone,password,null);
                int id = dao.create(u);
                u.setId(id);
                Main.send(ex, 201, JsonUtil.user(u));
                return;
            }

            if ("POST".equalsIgnoreCase(method) && path.endsWith("/login")) {
                Map<String,String> d = Main.parseJson(Main.read(ex));
                User u = dao.findByEmail(d.getOrDefault("email",""));
                if (u == null || !u.getPassword().equals(d.getOrDefault("password",""))) {
                    Main.send(ex, 401, JsonUtil.error("Invalid email or password."));
                    return;
                }
                Main.send(ex, 200, JsonUtil.user(u));
                return;
            }

            if ("GET".equalsIgnoreCase(method) && path.matches(".*/user/\\d+")) {
                int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
                User u = dao.findById(id);
                if (u == null) Main.send(ex,404,JsonUtil.error("User not found."));
                else Main.send(ex,200,JsonUtil.user(u));
                return;
            }

            Main.send(ex,404,JsonUtil.error("Endpoint not found."));
        } catch (SQLException e) {
            Main.send(ex,500,JsonUtil.error(e.getMessage()));
        }
    }

    private boolean blank(String s) { return s == null || s.trim().isEmpty(); }
}
