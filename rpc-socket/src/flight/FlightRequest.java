package flight;

import java.io.Serializable;

public class FlightRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer distance;

    public FlightRequest(Integer distance) {
        this.distance = distance;
    }

    public Integer getDistance() {
        return distance;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }
}
