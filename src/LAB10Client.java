import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class LAB10Client {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;

    private static final String[] FILES = {
            "Balcova.csv",
            "Bornova.csv",
            "Gaziemir.csv",
            "Karabaglar.csv",
            "Karsiyaka.csv",
            "Konak.csv",
            "Seferihisar.csv",
            "Urla.csv"
    };

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket();
             Scanner scanner = new Scanner(System.in)) {
            InetAddress serverAddress = InetAddress.getByName(SERVER_HOST);

            for (String file : FILES) {
                byte[] data = file.getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, SERVER_PORT);
                socket.send(packet);
            }

            while (true) {
                System.out.print("Send a district name to server to see stats (type 'EXIT' to quit): ");
                String query = scanner.nextLine().trim();
                if (query.isEmpty()) {
                    continue;
                }

                byte[] queryBytes = query.getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(queryBytes, queryBytes.length, serverAddress, SERVER_PORT);
                socket.send(packet);

                byte[] buffer = new byte[1024];
                DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                socket.receive(response);

                handleResponse(response.getData(), response.getLength(), query);

                if ("EXIT".equalsIgnoreCase(query)) {
                    System.out.println("Exiting...");
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }

    private static void handleResponse(byte[] data, int length, String query) {
        ByteBuffer buffer = ByteBuffer.wrap(data, 0, length);
        byte status = buffer.get();

        if (status == 0) {
            int totalPassengers = buffer.getInt();
            int totalTrips = buffer.getInt();
            double avg = buffer.getDouble();

            System.out.println("Here are the stats for the district " + query + ":");
            System.out.println("Total number of passengers: " + totalPassengers);
            System.out.println("Total number of trips: " + totalTrips);
            System.out.println("Average number of passengers per trip: " + String.format("%.2f", avg));
        } else if (status == 1) {
            int msgLen = buffer.getInt();
            byte[] msgBytes = new byte[msgLen];
            buffer.get(msgBytes);
            String message = new String(msgBytes, StandardCharsets.UTF_8);
            System.out.println(message);
        } else if (status == 2) {
            System.out.println("Server closed the connection.");
        } else {
            System.out.println("Unknown response from server.");
        }
    }
}
