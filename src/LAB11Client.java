import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class LAB11Client {
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
        try (Socket socket = new Socket(InetAddress.getByName(SERVER_HOST), SERVER_PORT);
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             Scanner scanner = new Scanner(System.in)) {
            for (String file : FILES) {
                out.writeUTF(file);
            }
            out.flush();

            while (true) {
                System.out.print("Send a district name to server to see stats (type 'EXIT' to quit): ");
                String query = scanner.nextLine().trim();
                if (query.isEmpty()) {
                    continue;
                }

                out.writeUTF(query);
                out.flush();

                int payloadLength = in.readInt();
                byte[] payload = new byte[payloadLength];
                in.readFully(payload);

                handleResponse(payload, query);

                if ("EXIT".equalsIgnoreCase(query)) {
                    System.out.println("Exiting...");
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }

    private static void handleResponse(byte[] data, String query) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
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
