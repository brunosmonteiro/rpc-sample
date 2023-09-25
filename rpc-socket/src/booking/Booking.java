package booking;

public class Booking {
    private Long clientId;
    private String origin;
    private String destination;
    private int distance;
    private int spendingDays;
    private double flightPrice;
    private double hotelPrice;
    private double totalPrice;

    public Long getClientId() {
        return clientId;
    }

    public Booking withClientId(Long clientId) {
        this.clientId = clientId;
        return this;
    }

    public String getOrigin() {
        return origin;
    }

    public Booking withOrigin(String origin) {
        this.origin = origin;
        return this;
    }

    public String getDestination() {
        return destination;
    }

    public Booking withDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public double getFlightPrice() {
        return flightPrice;
    }

    public void setFlightPrice(double flightPrice) {
        this.flightPrice = flightPrice;
    }

    public double getHotelPrice() {
        return hotelPrice;
    }

    public void setHotelPrice(final double hotelPrice) {
        this.hotelPrice = hotelPrice;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public int getDistance() {
        return distance;
    }

    public Booking withDistance(int distance) {
        this.distance = distance;
        return this;
    }

    public int getSpendingDays() {
        return spendingDays;
    }

    public Booking withHotelDays(int hotelDays) {
        this.spendingDays = hotelDays;
        return this;
    }

    public static Booking builder() {
        return new Booking();
    }

    @Override
    public String toString() {
        return "Booking [clientId=" + clientId + ", origin=" + origin + ", destination=" + destination
                + ", flightPrice=" + flightPrice + ", hotelPrice=" + hotelPrice + ", totalPrice=" + totalPrice + "]";
    }
}
