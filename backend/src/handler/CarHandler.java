import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.SQLException;

public class CarHandler implements HttpHandler {
    private final CarDAO dao = new CarDAO();

    @Override public void handle(HttpExchange ex) throws IOException {
        Main.cors(ex);
        try {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { Main.send(ex,204,""); return; }
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                Main.send(ex,405,JsonUtil.error("Method not allowed.")); return;
            }

            String path = ex.getRequestURI().getPath();
            if (path.endsWith("/available")) {
                Main.send(ex,200,JsonUtil.cars(dao.findAvailable()));
            } else if (path.matches(".*/cars/\\d+")) {
                int id = Integer.parseInt(path.substring(path.lastIndexOf('/')+1));
                Car c = dao.findById(id);
                if (c == null) Main.send(ex,404,JsonUtil.error("Car not found."));
                else Main.send(ex,200,JsonUtil.car(c));
            } else {
                Main.send(ex,200,JsonUtil.cars(dao.findAll()));
            }
        } catch (SQLException e) {
            Main.send(ex,500,JsonUtil.error(e.getMessage()));
        }
    }
}
