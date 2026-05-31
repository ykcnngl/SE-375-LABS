import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LAB10Server {
    private static final int SERVER_PORT = 5000;
    private static final int FILE_COUNT = 8;

    private static class Stats {
        final int totalPassengers;
        final int totalTrips;
        final double avgPassengersPerTrip;

        Stats(int totalPassengers, int totalTrips) {
            this.totalPassengers = totalPassengers;
            this.totalTrips = totalTrips;
            this.avgPassengersPerTrip = totalTrips == 0 ? 0.0 : (double) totalPassengers / totalTrips;
        }
    }

    public static void main(String[] args) {
        Map<String, Stats> statsByDistrict = new HashMap<>();

        try (DatagramSocket socket = new DatagramSocket(SERVER_PORT)) {
            byte[] buffer = new byte[1024];

            for (int i = 0; i < FILE_COUNT; i++) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                System.out.println("Waiting for a packet...");
                socket.receive(packet);

                String fileName = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();
                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                System.out.println("Received packet from: " + address + ":" + port + ", Message: " + fileName);

                String district = stripExtension(fileName);
                Stats stats = readStatsFromCsv(fileName);
                statsByDistrict.put(district, stats);
            }

            System.out.println("Processing done! Waiting for a query...");

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String query = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();
                System.out.println("Received query: " + query);

                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();

                if ("EXIT".equalsIgnoreCase(query)) {
                    sendExitAck(socket, clientAddress, clientPort);
                    System.out.println("Received EXIT. Exiting...");
                    break;
                }

                Stats stats = statsByDistrict.get(query);
                if (stats == null) {
                    sendError(socket, clientAddress, clientPort, "District doesn't exist!");
                } else {
                    sendStats(socket, clientAddress, clientPort, stats);
                }
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static Stats readStatsFromCsv(String fileName) throws IOException {
        int totalTrips = 0;
        int totalPassengers = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 4) {
                    continue;
                }
                int trips = Integer.parseInt(parts[2].trim());
                int passengers = Integer.parseInt(parts[3].trim());
                totalTrips += trips;
                totalPassengers += passengers;
            }
        }

        return new Stats(totalPassengers, totalTrips);
    }

    private static void sendStats(DatagramSocket socket, InetAddress address, int port, Stats stats) throws IOException {
        ByteBuffer payload = ByteBuffer.allocate(1 + 4 + 4 + 8);
        payload.put((byte) 0);
        payload.putInt(stats.totalPassengers);
        payload.putInt(stats.totalTrips);
        payload.putDouble(stats.avgPassengersPerTrip);

        byte[] data = payload.array();
        DatagramPacket response = new DatagramPacket(data, data.length, address, port);
        socket.send(response);
    }

    private static void sendError(DatagramSocket socket, InetAddress address, int port, String message) throws IOException {
        byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
        ByteBuffer payload = ByteBuffer.allocate(1 + 4 + msgBytes.length);
        payload.put((byte) 1);
        payload.putInt(msgBytes.length);
        payload.put(msgBytes);

        byte[] data = payload.array();
        DatagramPacket response = new DatagramPacket(data, data.length, address, port);
        socket.send(response);
    }

    private static void sendExitAck(DatagramSocket socket, InetAddress address, int port) throws IOException {
        ByteBuffer payload = ByteBuffer.allocate(1);
        payload.put((byte) 2);
        byte[] data = payload.array();
        DatagramPacket response = new DatagramPacket(data, data.length, address, port);
        socket.send(response);
    }
}
