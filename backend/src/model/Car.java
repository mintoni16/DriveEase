public class Car {
    private int id;
    private String name;
    private String brand;
    private String type;
    private double pricePerDay;
    private int seats;
    private String transmission;
    private boolean available;

    public Car() {}

    public Car(int id, String name, String brand, String type,
               double pricePerDay, int seats, String transmission, boolean available) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.type = type;
        this.pricePerDay = pricePerDay;
        this.seats = seats;
        this.transmission = transmission;
        this.available = available;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getBrand() { return brand; }
    public String getType() { return type; }
    public double getPricePerDay() { return pricePerDay; }
    public int getSeats() { return seats; }
    public String getTransmission() { return transmission; }
    public boolean isAvailable() { return available; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setBrand(String brand) { this.brand = brand; }
    public void setType(String type) { this.type = type; }
    public void setPricePerDay(double pricePerDay) { this.pricePerDay = pricePerDay; }
    public void setSeats(int seats) { this.seats = seats; }
    public void setTransmission(String transmission) { this.transmission = transmission; }
    public void setAvailable(boolean available) { this.available = available; }
}
