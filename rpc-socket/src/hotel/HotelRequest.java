package hotel;

import java.io.Serializable;

public class HotelRequest implements Serializable{
    private static final long serialVersionUID = 1L;
    private Integer days;

    public HotelRequest(final Integer days) {
        this.days = days;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(final Integer days) {
        this.days = days;
    }
}
