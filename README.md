# 1. Introduction

## 1.1 Resources

- **Repository**:
- **Technologies Used**:
    - Java 17;
    - Maven 3.+;
    - gRPC.

## 1.2 Concept Recap

**Remote Procedure Call (RPC)** is a protocol or a design pattern in computing that allows a program to cause a procedure (subroutine) to execute in another address space, either on the same machine or over a network, as if it were a local procedure call. RPC abstracts the underlying communication details, enabling developers to invoke procedures on a remote server without having to explicitly code the network interaction.

RPC is fundamentally a client-server interaction model. It does not have a predefined communication model, but TCP or HTTP are normally used. It can be synchronous or asynchronous, and even broadcast or multicast a request to multiple servers, like an explicit HTTP call made through **`Feign Client`** or **`RestTemplate`**.

### 1.2.1 **RPC Operational Workflow**

1. **Client-Stub Interaction**: The client invokes a procedure as if it's local, but actually calls a client stub.
2. **Message Building**: The client stub serializes the parameters into a message suitable for network transmission.
3. **OS-Level Transmission**: This message is then passed to the client's OS, which handles sending it over the network.
4. **Server OS to Stub**: The server's OS receives this message and passes it to the server stub.
5. **Parameter Unpacking**: The server stub deserializes the parameters from the received message.
6. **Server Processing**: The actual server procedure is called with these unpacked parameters and the necessary operation is performed.
7. **Result Serialization**: The result is serialized back into a message by the server stub.
8. **Return Message**: The server's OS sends this message back to the client's OS.
9. **Client OS to Stub**: The client's OS passes the received message back to the client stub.
10. **Result Unpacking**: The client stub deserializes the result and returns it to the client procedure.

### 1.2.2 **Differences and Design Considerations**

**Parameter Passing**: RPC employs serialization and deserialization, thereby isolating the invoked method from side effects on the actual parameters. Essentially, parameters are passed by value in a deep copy mode, unlike languages like Java, where the value is not a deep copy, rather a copy of the object’s reference.

**Semantics**: The notion that "RPC is like a local call" serves as a simplified model. In reality, a remote procedure call can't modify client-side variables and is subject to network issues, making it fundamentally different from local calls.

**Design Principles**: Adopting a stateless approach is beneficial in distributed systems. Idempotency, immutability, and data encapsulation in objects like DTOs are essential considerations.

## 1.3 Technical Scope

This is a practical exercise to showcase a simple interaction with Remote Procedure Call (RPC). This will be done in two layers:

1. Raw implementation using **`java.net.Socket`** for communication, **`java.io.ObjectInputStream`** and **`java.io.ObjectOutputStream`** for object serialization;
2. Implementation using **`gRPC`** (including automatic file generation using **`protobuf`**) and **`Maven`**.

## 1.4 Business Scope

We will work with the domain of bookings and travels. There will be a client, **`BookingClient`**, that consumes information from **`FlightServer`** and **`HotelServer`**.

- **`FlightServer`** will calculate the price of the flight based on the distance it receives.
- **`HotelServer`** will calculate the price of the hotel booking based on the amount of days the client will spend there.
- **`BookingClient`** will call both and process the total price of the booking.

This is only a simplified scenario to work with the RPC concept, all business logic will be as straightforward as possible.

## 1.5 Data Objects

We will work with a main entity, that will only exist in the **`BookingClient`** component, called **`Booking`**:

```java
public class Booking {
    private Long clientId;
    private String destination;
    private String origin;
    private int distance;
    private int stayingDays;
    private double flightPrice;
    private double hotelPrice;
    private double totalPrice;

// Getters and setters, toString() generation
}
```

Then, we will define our data transfer objects.

For the flight context, let’s define **`FlightRequest`** and **`FlightResponse`**.

```java
public class FlightRequest {
    private int distance;
// Getters and setters
}

public class FlightResponse {
    private double price;
// Getters and setters
}
```

Now, `**HotelRequest**` and `**HotelResponse`.**

```java
public class HotelRequest {
    private Integer days;
// Getters and setters
}

public class HotelRespons {
    private double price;
// Getters and setters
}
```

# 2. Raw Socket Implementation

Our first implementation will showcase RPC in its lowest level, manually defining the **`Socket`** and passing objects through native Java objects. Everything related to this implementation can be found under the **`rpc-socket`** folder.

We need to make sure our transferred objects all implement the **`java.io.Serializable`** class, to make sure they can be handled by the the underlying serialization classes.

## 2.1 Flow of Communication

This is the overall flow of communication in this low level connection.

### 2.1.1 Server Side

1. **Server Initialization**: The server initializes a **`ServerSocket`** on a specific port (e.g., 8001) and starts listening for incoming client connections.
2. **Accepting Connection**: The server enters a blocking state by calling the **`accept()`** method on the **`ServerSocket`**. It waits for a client to initiate a connection.
3. **Establish Connection**: Once a client initiates a connection, **`accept()`** returns and provides a **`Socket`** instance, representing the connection to that specific client.
4. **Stream Initialization**: Two types of streams are initialized:
    - **`ObjectOutputStream`** is set up first to write serialized objects to the client.
    - The output stream is then flushed to ensure that serialization headers are sent, preparing the way for the client to safely initialize its **`ObjectInputStream`**.
    - **`ObjectInputStream`** is set up to read serialized objects from the client.
5. **Data Transfer**: The server reads a request object from the client, processes it, and then sends a response object back to the client using these streams.
6. **Connection Termination**: After the data exchange is complete, the streams and socket connection are closed, either explicitly or automatically by a try-with-resources block.

### 2.1.2 Client Side

1. **Client Initialization**: The client initializes a **`Socket`** with the server's hostname and port number, automatically attempting to establish a connection.
2. **Stream Initialization**: Similar to the server, the client sets up **`ObjectInputStream`** and **`ObjectOutputStream`** for the newly created **`Socket`**.
3. **Send Request**: The client writes a serialized object representing the request and sends it to the server using **`ObjectOutputStream`**.
4. **Receive Response**: The client then reads the server's serialized response object using **`ObjectInputStream`**.
5. **Connection Termination**: The streams and the client's socket connection are closed once the data transfer is complete.

## 2.2 Flight Server

We only need a simple main class to make things work, in these following steps.

### 2.2.1 Defining Resources

```java
try (final ServerSocket serverSocket = new ServerSocket(8001);
     final Socket socket = serverSocket.accept();
     final var output = new ObjectOutputStream(socket.getOutputStream())) {
```

- **`ServerSocket serverSocket = new ServerSocket(8001)`**: Initializes a new **`ServerSocket`** listening on port 8001.
- **`Socket socket = serverSocket.accept()`**: Listens for a connection to be made to this socket and accepts it.
- **`ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())`**: Initializes an **`ObjectOutputStream`** to serialize objects and send them to the client via **`socket.getOutputStream()`**.

### 2.2.2 Flushing the Output Stream

```java
output.flush();
```

When **`flush()`** is called on the **`ObjectOutputStream`**, it flushes the internal data buffer and forces any buffered output bytes to be written out to the underlying output stream (in this case, **`socket.getOutputStream()`**). This ensures that the serialization stream header is completely sent over the network, reaching the client. If this step is not performed, you might run into stream corruption issues or **`EOFException`** (End of File Exception) when the client attempts to initialize the **`ObjectInputStream`**.

The reason you typically see the **`flush()`** method called right after initializing an **`ObjectOutputStream`** but before creating an **`ObjectInputStream`** is to ensure that:

1. The serialization header from the **`ObjectOutputStream`** is completely sent to the receiving end.
2. The **`ObjectInputStream`** at the receiving end reads this header, so it's correctly configured to deserialize the incoming objects.

If the header isn't correctly sent and read, the entire object serialization/deserialization process would fail, causing exceptions and making the communication unreliable.

### 2.2.3 Creating and Reading the Input

```java
final var input = new ObjectInputStream(socket.getInputStream());
final var flightRequest = (FlightRequest) input.readObject();
```

Initializes an **`ObjectInputStream`** for deserializing incoming objects from the client. Reads the client's **`FlightRequest`** object and performs logic to determine the cost of the flight.

### 2.2.4 Writing Back to the Client

```java
output.writeObject(new FlightResponse(flightRequest.getDistance() * 50.0));
```

Writes the **`FlightResponse`** object back to the client using **`output.writeObject()`**. The price calculation is simply for demonstration purposes.

### 2.2.5 Final View

```java
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
            output.flush();
            final var input = new ObjectInputStream(socket.getInputStream());
            final var flightRequest = (FlightRequest) input.readObject();
            output.writeObject(new FlightResponse(flightRequest.getDistance() * 50.0));
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
```

## 2.3 Hotel Server

With the hotel server, the idea is the same. Let’s just make sure we start the server in a different port than the other one.

```java
public class HotelServer {
    public static void main(final String[] args) {
        try (final ServerSocket serverSocket = new ServerSocket(8002);
             final Socket socket = serverSocket.accept();
             final var output = new ObjectOutputStream(socket.getOutputStream())) {
            output.flush();
            final var input = new ObjectInputStream(socket.getInputStream());
            final var hotelRequest = (HotelRequest) input.readObject();
            output.writeObject(new HotelResponse(hotelRequest.getDays() * 100.0));
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
```

## 2.4 Booking Client

Now, let’s implement our client. It will simply create the main **`Booking`** object, create connections with flight and hotel servers, take the response make a total calculation based on the returns. Then, we will print the entire object for a simple check. **`toString()`** was generated for a better visualization.

### 2.4.1 Basic Object Creation

```java
final var booking = Booking.builder()
  .withClientId(1L)
  .withOrigin("Araguari")
  .withDestination("Uberlandia")
  .withDistance(50)
  .withHotelDays(5);
```

### 2.4.2 Calling Both Servers

```java
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
```

The call to both servers will be fairly close, we just need to adjust objects and the port.

```java
final var flightRequest = new FlightRequest(booking.getDistance());
flightOutput.writeObject(flightRequest);
```

will write the request to the server, while

```java
return (FlightResponse) flightInput.readObject();
```

reads the response. Resources are closed after the connection is through.

### 2.4.3 Final View

```java
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
```

# 3. gRPC Implementation

Now that the basics have been showcased, let’s go with a bit more robust implementation. We will leverage the use of `**gRPC**`, a very popular tool to make RPC easier for us. To make dependencies more accessible, we will also use Maven. Everything related to this implementation can be found under the **`gRPC`** folder.

Our project will have three independent modules:

- **`booking-client`**
- **`flight-server`**
- **`hotel-server`**

## 3.1 Dependency Management

For starters, let’s go through our dependencies. This is the pom.xml for booking-client, but it is essentially the same for all modules.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example.grpc</groupId>
    <artifactId>booking-client</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <protobuf.version>3.24.3</protobuf.version>
        <grpc.version>1.58.0</grpc.version>
        <javax.annotation.version>1.3.2</javax.annotation.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.google.protobuf</groupId>
            <artifactId>protobuf-java</artifactId>
            <version>${protobuf.version}</version>
        </dependency>

        <dependency>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-netty-shaded</artifactId>
            <version>${grpc.version}</version>
        </dependency>
        
        <dependency>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-protobuf</artifactId>
            <version>${grpc.version}</version>
        </dependency>
        
        <dependency>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-stub</artifactId>
            <version>${grpc.version}</version>
        </dependency>
        
        <dependency>
            <groupId>javax.annotation</groupId>
            <artifactId>javax.annotation-api</artifactId>
            <version>${javax.annotation.version}</version>
        </dependency>      
    </dependencies>

    <build>
        <defaultGoal>clean generate-sources compile install</defaultGoal>

        <plugins>
            <plugin>
                <groupId>com.github.os72</groupId>
                <artifactId>protoc-jar-maven-plugin</artifactId>
                <version>3.11.4</version>
                <executions>
                    <execution>
                        <phase>generate-sources</phase>
                        <goals>
                            <goal>run</goal>
                        </goals>
                        <configuration>
                            <includeMavenTypes>direct</includeMavenTypes>

                            <inputDirectories>
                                <include>src/main/resources</include>
                            </inputDirectories>

                            <outputTargets>
                                <outputTarget>
                                    <type>java</type>
                                    <outputDirectory>src/main/java</outputDirectory>
                                </outputTarget>
                                <outputTarget>
                                    <type>grpc-java</type>
                                    <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.58.0</pluginArtifact>
                                    <outputDirectory>src/main/java</outputDirectory>
                                </outputTarget>
                            </outputTargets>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

### 3.1.1 Dependencies

**protobuf-java**: This dependency is for Google's Protocol Buffers (protobuf), a language-neutral, platform-neutral extensible mechanism for serializing structured data. It's essentially the Java library for protobuf. Protocol Buffers are highly efficient in terms of both speed and size, making them ideal for microservices or APIs, such as the one you are trying to build with gRPC

```xml
<dependency>
    <groupId>com.google.protobuf</groupId>
    <artifactId>protobuf-java</artifactId>
    <version>${protobuf.version}</version>
</dependency>
```

**grpc-netty-shaded**: Netty is a non-blocking I/O client-server framework for building network applications. The **`grpc-netty-shaded`** dependency is a packaged version of Netty that comes with gRPC and is suitable for running gRPC clients and servers. Being "shaded" means that this version of Netty has been relocated to a different Java package to avoid conflicts with any other version of Netty that might be on the classpath.

```xml
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-netty-shaded</artifactId>
    <version>${grpc.version}</version>
</dependency>
```

**grpc-protobuf**: This dependency essentially bridges gRPC with Protocol Buffers. It contains the runtime libraries needed to convert messages between protobuf and gRPC formats. This library ensures that you can use protobuf serialized objects as both the request and response types for gRPC calls. This keeps the coupling between your serialization logic and network logic low, making the system more maintainable and scalable.

```xml
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-protobuf</artifactId>
    <version>${grpc.version}</version>
</dependency>
```

**grpc-stub**: Stubs are representations of gRPC services. They handle the task of building, sending, and receiving gRPC calls on both the client and server sides. The **`grpc-stub`** dependency provides the classes required to make these stubs work, such as methods for initiating the RPC, setting deadlines, and other functionalities.

```xml
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-stub</artifactId>
    <version>${grpc.version}</version>
</dependency>
```

**javax-annotation**: not required, but avoids compilation errors when the project is first downloaded.

```xml
<dependency>
    <groupId>javax.annotation</groupId>
    <artifactId>javax.annotation-api</artifactId>
    <version>${javax.annotation.version}</version>
</dependency>
```

### 3.1.2 Plugins

```xml
<plugin>
    <groupId>com.github.os72</groupId>
    <artifactId>protoc-jar-maven-plugin</artifactId>
    <version>3.11.4</version>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals>
                <goal>run</goal>
            </goals>
            <configuration>
                <includeMavenTypes>direct</includeMavenTypes>

                <inputDirectories>
                    <include>src/main/resources</include>
                </inputDirectories>

                <outputTargets>
                    <outputTarget>
                        <type>java</type>
                        <outputDirectory>src/main/java</outputDirectory>
                    </outputTarget>
                    <outputTarget>
                        <type>grpc-java</type>
                        <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.58.0</pluginArtifact>
                        <outputDirectory>src/main/java</outputDirectory>
                    </outputTarget>
                </outputTargets>
            </configuration>
        </execution>
    </executions>
</plugin>
```

The **`plugins`** subsection further elaborates on the build process. It incorporates the **`protoc-jar-maven-plugin`**, responsible for compiling Protocol Buffers **`.proto`** files into Java source code, and the **`maven-compiler-plugin`**, responsible for compiling the Java source code. The plugins are mapped to specific build phases such as **`generate-sources`** for protocol buffer compilation and the default compile phase for Java compilation.

## 3.2 Implementing our Servers

### 3.2.1 Proto File

As our **`pom.xml`** suggests, we need **``.proto`** files in the **`src/main/resources`** folder, in all modules. In **`flight-server`**, for instance, we will have the following **`flight.proto`**

```protobuf
syntax = "proto3";
option java_multiple_files = true;
option java_package = "com.example.grpc.proto";

message FlightRequest {
  int32 distance = 1;
}

message FlightResponse {
  double price = 1;
}

service FlightService {
  rpc BookFlight (FlightRequest) returns (FlightResponse);
}
```

gRPC itself is limited to the types it can represent, so, for simplicity, we will use **`double`**` for decimal values.

The service **`FlightService`** defines the endpoint contract up to communication. it receives a **`FlightRequest`** and returns a **`FlightResponse`**.

With our dependencies and plugin in place, these classes don’t need to be created. The java_package tells the output directory for the compiled classes based on our definitions. When we run **`mvn clean install`**, the corresponding classes are created in the specified directory.

### 3.2.2 Service Implementation

Among the classes created, let’s focus on **`FlightServiceGrpc`** We need to create our implementation based on this class, so let’s create a **`FlightServiceImpl`**:

```java
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
```

The business rules are the same as in the first example. **`responseObserver.onNext(response)`** is the response that is returned by the service.

### 3.2.3 Main Class

The last thing we need now is to create our main class.

```java
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
```

Here, we just need to be sure to add our service implementation in the gRPC’s **`Server`** instance.

For the hotel context, it is essentially the same idea.

## 3.3 Defining our Client

### 3.3.1 Proto File

The client’s **`.proto`** file must contain the combination of all **`.proto`** files from the servers it calls. This will be as defined in **`schema.proto`**

```protobuf
syntax = "proto3";
option java_multiple_files = true;
option java_package = "com.example.grpc.proto";

message FlightRequest {
  int32 distance = 1;
}

message FlightResponse {
  double price = 1;
}

service FlightService {
  rpc BookFlight (FlightRequest) returns (FlightResponse);
}

message HotelRequest {
  int32 days = 1;
}

message HotelResponse {
  double price = 1;
}

service HotelService {
  rpc BookHotel (HotelRequest) returns (HotelResponse);
}
```

### 3.3.2 Main Class

As in the raw example, the client calls both servers and makes a little logic application.

```java
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
```

As in the server, we use the **`ServiceGrpc`** classes to perform the calls. **`newBlockingStub`** means a synchronous request-response. We also make use of the generated builders.
