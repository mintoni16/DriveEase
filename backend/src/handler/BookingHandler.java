import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class BookingHandler implements HttpHandler {
    private final BookingDAO bookings = new BookingDAO();
    private final CarDAO cars = new CarDAO();

    @Override public void handle(HttpExchange ex) throws IOException {
        Main.cors(ex);
        try {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { Main.send(ex,204,""); return; }

            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();

            if ("POST".equalsIgnoreCase(method) && path.endsWith("/bookings")) {
                Map<String,String> d = Main.parseJson(Main.read(ex));
                int userId = Integer.parseInt(d.get("userId"));
                int carId = Integer.parseInt(d.get("carId"));
                String pickup = d.get("pickupDate");
                String ret = d.get("returnDate");

                LocalDate p = LocalDate.parse(pickup);
                LocalDate r = LocalDate.parse(ret);
                if (!r.isAfter(p)) {
                    Main.send(ex,400,JsonUtil.error("Return date must be after pickup date."));
                    return;
                }

                Car car = cars.findById(carId);
                if (car == null) { Main.send(ex,404,JsonUtil.error("Car not found.")); return; }

                if (bookings.hasOverlap(carId,pickup,ret)) {
                    Main.send(ex,409,JsonUtil.error("Car is already booked for these dates."));
                    return;
                }

                long days = ChronoUnit.DAYS.between(p,r);
                double total = days * car.getPricePerDay();
                int id = bookings.create(userId,carId,pickup,ret,total);

                Main.send(ex,201,JsonUtil.message("Booking created. ID: " + id));
                return;
            }

            if ("GET".equalsIgnoreCase(method) && path.matches(".*/bookings/user/\\d+")) {
                int userId = Integer.parseInt(path.substring(path.lastIndexOf('/')+1));
                Main.send(ex,200,JsonUtil.bookings(bookings.findByUser(userId)));
                return;
            }

            if ("DELETE".equalsIgnoreCase(method) && path.matches(".*/bookings/\\d+")) {
                int id = Integer.parseInt(path.substring(path.lastIndexOf('/')+1));
                if (bookings.cancel(id)) Main.send(ex,200,JsonUtil.message("Booking cancelled."));
                else Main.send(ex,404,JsonUtil.error("Booking not found or already cancelled."));
                return;
            }

            Main.send(ex,404,JsonUtil.error("Endpoint not found."));
        } catch (Exception e) {
            Main.send(ex,400,JsonUtil.error(e.getMessage()));
        }
    }
}
