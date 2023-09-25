package com.example.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class FlightServer {
    public static void main(String[] args) throws Exception {
        final Server server = ServerBuilder
            .forPort(8001)
            .addService(new FlightServiceImpl())
            .build();
        server.start();
        server.awaitTermination();
    }
}