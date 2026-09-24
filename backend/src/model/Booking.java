public class Booking {
    private int id;
    private int userId;
    private int carId;
    private String carName;
    private String pickupDate;
    private String returnDate;
    private double totalPrice;
    private String status;
    private String createdAt;

    public Booking() {}

    public Booking(int id, int userId, int carId, String carName,
                   String pickupDate, String returnDate,
                   double totalPrice, String status, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.carId = carId;
        this.carName = carName;
        this.pickupDate = pickupDate;
        this.returnDate = returnDate;
        this.totalPrice = totalPrice;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public int getCarId() { return carId; }
    public String getCarName() { return carName; }
    public String getPickupDate() { return pickupDate; }
    public String getReturnDate() { return returnDate; }
    public double getTotalPrice() { return totalPrice; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }

    public void setId(int id) { this.id = id; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setCarId(int carId) { this.carId = carId; }
    public void setCarName(String carName) { this.carName = carName; }
    public void setPickupDate(String pickupDate) { this.pickupDate = pickupDate; }
    public void setReturnDate(String returnDate) { this.returnDate = returnDate; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
