package com.example.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class HotelServer {
    public static void main(String[] args) throws Exception {
        final Server server = ServerBuilder
            .forPort(8002)
            .addService(new HotelServiceImpl())
            .build();
        server.start();
        server.awaitTermination();
    }
}