import java.util.List;

public class JsonUtil {

    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    public static String user(User u) {
        return "{\"id\":" + u.getId()
                + ",\"name\":\"" + escape(u.getName())
                + "\",\"email\":\"" + escape(u.getEmail())
                + "\",\"phone\":\"" + escape(u.getPhone())
                + "\",\"createdAt\":\"" + escape(u.getCreatedAt()) + "\"}";
    }

    public static String car(Car c) {
        return "{\"id\":" + c.getId()
                + ",\"name\":\"" + escape(c.getName())
                + "\",\"brand\":\"" + escape(c.getBrand())
                + "\",\"type\":\"" + escape(c.getType())
                + "\",\"pricePerDay\":" + c.getPricePerDay()
                + ",\"seats\":" + c.getSeats()
                + ",\"transmission\":\"" + escape(c.getTransmission())
                + "\",\"available\":" + c.isAvailable() + "}";
    }

    public static String cars(List<Car> cars) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < cars.size(); i++) {
            if (i > 0) b.append(",");
            b.append(car(cars.get(i)));
        }
        return b.append("]").toString();
    }

    public static String booking(Booking b) {
        return "{\"id\":" + b.getId()
                + ",\"userId\":" + b.getUserId()
                + ",\"carId\":" + b.getCarId()
                + ",\"carName\":\"" + escape(b.getCarName())
                + "\",\"pickupDate\":\"" + escape(b.getPickupDate())
                + "\",\"returnDate\":\"" + escape(b.getReturnDate())
                + "\",\"totalPrice\":" + b.getTotalPrice()
                + ",\"status\":\"" + escape(b.getStatus())
                + "\",\"createdAt\":\"" + escape(b.getCreatedAt()) + "\"}";
    }

    public static String bookings(List<Booking> bookings) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < bookings.size(); i++) {
            if (i > 0) b.append(",");
            b.append(booking(bookings.get(i)));
        }
        return b.append("]").toString();
    }

    public static String message(String message) {
        return "{\"message\":\"" + escape(message) + "\"}";
    }

    public static String error(String message) {
        return "{\"error\":\"" + escape(message) + "\"}";
    }
}
