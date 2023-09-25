package flight;

import java.io.Serializable;

public class FlightResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private double price;

    public FlightResponse(final double price) {
        this.price = price;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(final double price) {
        this.price = price;
    }
}
