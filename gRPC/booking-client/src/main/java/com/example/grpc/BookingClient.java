package com.example.grpc;

import com.example.grpc.proto.FlightRequest;
import com.example.grpc.proto.FlightResponse;
import com.example.grpc.proto.FlightServiceGrpc;
import com.example.grpc.proto.HotelRequest;
import com.example.grpc.proto.HotelResponse;
import com.example.grpc.proto.HotelServiceGrpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class BookingClient {
    public static void main(final String[] args) {
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

    private static FlightResponse getFlightResponse(final Booking booking) {
        final ManagedChannel flightStub = ManagedChannelBuilder
            .forAddress("localhost", 8001)
            .usePlaintext()
            .build();

        final FlightServiceGrpc.FlightServiceBlockingStub stub = FlightServiceGrpc.newBlockingStub(flightStub);
        
        final FlightRequest flightRequest = FlightRequest.newBuilder().setDistance(booking.getDistance()).build();
        final FlightResponse flightResponse = stub.bookFlight(flightRequest);

        flightStub.shutdown();

        return flightResponse;
    }

    private static HotelResponse getHotelResponse(final Booking booking) {
        final ManagedChannel hotelStub = ManagedChannelBuilder
            .forAddress("localhost", 8002)
            .usePlaintext()
            .build();

        final HotelServiceGrpc.HotelServiceBlockingStub stub = HotelServiceGrpc.newBlockingStub(hotelStub);
        
        final HotelRequest hotelRequest = HotelRequest.newBuilder().setDays(booking.getHotelDays()).build();
        final HotelResponse hotelResponse = stub.bookHotel(hotelRequest);

        hotelStub.shutdown();

        return hotelResponse;
    }
}
