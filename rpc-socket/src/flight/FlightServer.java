package flight;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;

public class FlightServer {
    public static void main(final String[] args) {
        try (final ServerSocket serverSocket = new ServerSocket(8001);
             final Socket socket = serverSocket.accept();
             final var output = new ObjectOutputStream(socket.getOutputStream())) {
             
            // Flush the ObjectOutputStream to make sure the header is sent
            output.flush();

            // Now we can safely create the ObjectInputStream
            final var input = new ObjectInputStream(socket.getInputStream());

            // Read and modify the booking
            final var flightRequest = (FlightRequest) input.readObject();

            // Send the modified booking back
            output.writeObject(new FlightResponse(flightRequest.getDistance() * 50.0));

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
