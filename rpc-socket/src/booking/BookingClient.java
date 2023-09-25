package booking;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import flight.FlightRequest;
import flight.FlightResponse;
import hotel.HotelRequest;
import hotel.HotelResponse;

public class BookingClient {
    public static void main(String[] args) throws Exception {
        final var booking = Booking.builder()
            .withClientId(1L)
            .withOrigin("Araguari")
            .withDestination("Uberlandia")
            .withDistance(50)
            .withHotelDays(5);

        booking.setFlightPrice(getFlightResponse(booking).getPrice());
        booking.setHotelPrice(getHotelResponse(booking).getPrice());
        booking.setTotalPrice(booking.getFlightPrice() + booking.getHotelPrice());

        System.out.println(booking);
    }

    private static FlightResponse getFlightResponse(final Booking booking) throws Exception {
        Socket flightSocket = null;
        ObjectOutputStream flightOutput = null;
        ObjectInputStream flightInput = null;
        try {
            flightSocket = new Socket("localhost", 8001);
            flightOutput = new ObjectOutputStream(flightSocket.getOutputStream());
            flightInput = new ObjectInputStream(flightSocket.getInputStream());

            final var flightRequest = new FlightRequest(booking.getDistance());
            flightOutput.writeObject(flightRequest);
            return (FlightResponse) flightInput.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            closeResources(flightSocket, flightOutput, flightInput);
        }
    }

    private static HotelResponse getHotelResponse(final Booking booking) throws Exception {
        Socket hotelSocket = null;
        ObjectOutputStream hotelOutput = null;
        ObjectInputStream hotelInput = null;
        try {
            hotelSocket = new Socket("localhost", 8002);
            hotelOutput = new ObjectOutputStream(hotelSocket.getOutputStream());
            hotelInput = new ObjectInputStream(hotelSocket.getInputStream());

            final var hotelRequest = new HotelRequest(booking.getSpendingDays());
            hotelOutput.writeObject(hotelRequest);
            return (HotelResponse) hotelInput.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            closeResources(hotelSocket, hotelOutput, hotelInput);
        }
    }

    private static void closeResources(
            final Socket socket,
            final ObjectOutputStream outputStream,
            final ObjectInputStream inputStream) {
        try {
            if (socket != null) {
                socket.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
