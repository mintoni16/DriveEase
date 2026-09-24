import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class ProfileHandler implements HttpHandler {
    private final UserDAO dao = new UserDAO();

    @Override public void handle(HttpExchange ex) throws IOException {
        Main.cors(ex);
        try {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { Main.send(ex,204,""); return; }

            String path = ex.getRequestURI().getPath();
            int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));

            if ("GET".equalsIgnoreCase(ex.getRequestMethod())) {
                User u = dao.findById(id);
                if (u == null) Main.send(ex,404,JsonUtil.error("User not found."));
                else Main.send(ex,200,JsonUtil.user(u));
                return;
            }

            if ("PUT".equalsIgnoreCase(ex.getRequestMethod())) {
                User u = dao.findById(id);
                if (u == null) { Main.send(ex,404,JsonUtil.error("User not found.")); return; }
                Map<String,String> d = Main.parseJson(Main.read(ex));
                if (d.containsKey("name")) u.setName(d.get("name"));
                if (d.containsKey("phone")) u.setPhone(d.get("phone"));
                dao.update(u);
                Main.send(ex,200,JsonUtil.user(u));
                return;
            }

            Main.send(ex,405,JsonUtil.error("Method not allowed."));
        } catch (SQLException | NumberFormatException e) {
            Main.send(ex,400,JsonUtil.error(e.getMessage()));
        }
    }
}
