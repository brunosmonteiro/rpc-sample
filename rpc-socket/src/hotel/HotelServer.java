package hotel;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;

public class HotelServer {
    public static void main(final String[] args) {
        try (final ServerSocket serverSocket = new ServerSocket(8002);
             final Socket socket = serverSocket.accept();
             final var output = new ObjectOutputStream(socket.getOutputStream())) {

            // Flush the ObjectOutputStream to make sure the header is sent
            /*
             * When you instantiate an ObjectOutputStream in Java, the constructor writes a serialization stream header to
             * the underlying stream. This header is essential for the receiving ObjectInputStream to understand the 
             * incoming serialized objects.

             * The flush() method is used to flush the underlying output stream. It writes any buffered data to the 
             * underlying stream, which, in this case, would be the serialization header. In some scenarios, if you don't 
             * flush this header out, the other end's ObjectInputStream may block indefinitely, waiting for this header 
             * information to arrive. So, flushing the ObjectOutputStream clears this header from the internal buffers, 
             * ensuring it gets sent over the network immediately.
             */
            output.flush();
            
            // Now we can safely create the ObjectInputStream
            final var input = new ObjectInputStream(socket.getInputStream());

            final var hotelRequest = (HotelRequest) input.readObject();

            output.writeObject(new HotelResponse(hotelRequest.getDays() * 100));

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
