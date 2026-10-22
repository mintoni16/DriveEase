package driveease;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/** Plain data holders. toMap() produces the exact JSON the frontend expects. */
final class Models {
    private Models() {}

    record User(int id, String name, String email, String phone, String createdAt) {
        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            m.put("name", name);
            m.put("email", email);
            m.put("phone", phone);
            m.put("createdAt", createdAt);
            return m;
        }
    }

    static final class Car extends Vehicle {
        private final int seats;
        private final String transmission;
        private final String fuel;
        private final int pricePerDay;
        private final boolean popular;

        Car(int id, String name, String category, String description, int seats,
            String transmission, String fuel, int pricePerDay, boolean popular) {
            super(id, name, category, description);
            this.seats = seats;
            this.transmission = transmission;
            this.fuel = fuel;
            this.pricePerDay = pricePerDay;
            this.popular = popular;
        }

        int seats() { return seats; }
        String transmission() { return transmission; }
        String fuel() { return fuel; }
        int pricePerDay() { return pricePerDay; }
        boolean popular() { return popular; }

        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id());
            m.put("name", name());
            m.put("category", category());
            m.put("description", description());
            m.put("seats", seats);
            m.put("transmission", transmission);
            m.put("fuel", fuel);
            m.put("pricePerDay", pricePerDay);
            m.put("popular", popular);
            return m;
        }
    }

    record Booking(int id, Car car, LocalDate pickupDate, LocalDate returnDate, String pickupLocation,
                   String dropLocation, int totalPrice, String status, String createdAt) {

        /** The DB only stores confirmed/cancelled; "completed" is derived once the return date has passed. */
        String effectiveStatus() {
            if (status.equals("confirmed") && returnDate.isBefore(LocalDate.now())) return "completed";
            return status;
        }

        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            m.put("reference", String.format("DE-%06d", id));
            m.put("status", effectiveStatus());
            m.put("car", car.toMap());
            m.put("pickupDate", pickupDate.toString());
            m.put("returnDate", returnDate.toString());
            m.put("pickupLocation", pickupLocation);
            m.put("dropLocation", dropLocation);
            m.put("days", ChronoUnit.DAYS.between(pickupDate, returnDate));
            m.put("totalPrice", totalPrice);
            m.put("createdAt", createdAt);
            return m;
        }
    }
}


/**
 * Common parent for rentable vehicles. Car inherits the shared identity and
 * descriptive fields, while keeping car-specific features in Models.Car.
 * This is the project's explicit OOP inheritance demonstration.
 */
abstract class Vehicle {
    private final int id;
    private final String name;
    private final String category;
    private final String description;

    protected Vehicle(int id, String name, String category, String description) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
    }

    int id() { return id; }
    String name() { return name; }
    String category() { return category; }
    String description() { return description; }
}
