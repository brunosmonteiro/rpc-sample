package com.example.grpc;

import com.example.grpc.proto.HotelRequest;
import com.example.grpc.proto.HotelResponse;
import com.example.grpc.proto.HotelServiceGrpc;

import io.grpc.stub.StreamObserver;

public class HotelServiceImpl extends HotelServiceGrpc.HotelServiceImplBase {
    
    @Override
    public void bookHotel(final HotelRequest request, final StreamObserver<HotelResponse> responseObserver) {
        final int days = request.getDays();
        final double price = days * 100.0;
        final var response = HotelResponse.newBuilder().setPrice(price).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
