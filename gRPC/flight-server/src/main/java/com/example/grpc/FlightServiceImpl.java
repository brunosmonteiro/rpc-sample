package com.example.grpc;

import com.example.grpc.proto.FlightRequest;
import com.example.grpc.proto.FlightResponse;
import com.example.grpc.proto.FlightServiceGrpc;

import io.grpc.stub.StreamObserver;

public class FlightServiceImpl extends FlightServiceGrpc.FlightServiceImplBase {

    @Override
    public void bookFlight(final FlightRequest request, final StreamObserver<FlightResponse> responseObserver) {
        final int distance = request.getDistance();
        final double price = distance * 50.0;
        final var response = FlightResponse.newBuilder().setPrice(price).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
