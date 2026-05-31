import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LAB11Server {
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
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Server listening on port " + SERVER_PORT + "...");
            while (true) {
                Socket client = serverSocket.accept();
                Thread handler = new Thread(() -> handleClient(client));
                handler.start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    private static void handleClient(Socket client) {
        Map<String, Stats> statsByDistrict = new HashMap<>();
        Lock mapLock = new ReentrantLock();

        try (Socket socket = client;
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            List<String> fileNames = new ArrayList<>();
            for (int i = 0; i < FILE_COUNT; i++) {
                fileNames.add(in.readUTF());
            }

            System.out.println("Received connection from: "
                    + socket.getInetAddress() + ":" + socket.getPort());
            System.out.println("File names: " + String.join(", ", fileNames));

            List<Thread> workers = new ArrayList<>();
            for (String fileName : fileNames) {
                Thread worker = new Thread(() -> {
                    try {
                        Stats stats = readStatsFromCsv(fileName);
                        String district = stripExtension(fileName);
                        mapLock.lock();
                        try {
                            statsByDistrict.put(district, stats);
                        } finally {
                            mapLock.unlock();
                        }
                    } catch (IOException e) {
                        System.out.println("Failed to read " + fileName + ": " + e.getMessage());
                    }
                });
                worker.start();
                workers.add(worker);
            }

            for (Thread worker : workers) {
                try {
                    worker.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            System.out.println("Processing done! Waiting for queries...");

            while (true) {
                String query;
                try {
                    query = in.readUTF().trim();
                } catch (EOFException e) {
                    break;
                }

                System.out.println("Received query: " + query);

                if ("EXIT".equalsIgnoreCase(query)) {
                    sendPayload(out, buildExitAck());
                    System.out.println("Received EXIT. Closing client.");
                    break;
                }

                Stats stats = statsByDistrict.get(query);
                if (stats == null) {
                    sendPayload(out, buildError("District doesn't exist!"));
                } else {
                    sendPayload(out, buildStats(stats));
                }
            }
        } catch (IOException e) {
            System.out.println("Client handler error: " + e.getMessage());
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

    private static byte[] buildStats(Stats stats) {
        ByteBuffer payload = ByteBuffer.allocate(1 + 4 + 4 + 8);
        payload.put((byte) 0);
        payload.putInt(stats.totalPassengers);
        payload.putInt(stats.totalTrips);
        payload.putDouble(stats.avgPassengersPerTrip);
        return payload.array();
    }

    private static byte[] buildError(String message) {
        byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
        ByteBuffer payload = ByteBuffer.allocate(1 + 4 + msgBytes.length);
        payload.put((byte) 1);
        payload.putInt(msgBytes.length);
        payload.put(msgBytes);
        return payload.array();
    }

    private static byte[] buildExitAck() {
        ByteBuffer payload = ByteBuffer.allocate(1);
        payload.put((byte) 2);
        return payload.array();
    }

    private static void sendPayload(DataOutputStream out, byte[] payload) throws IOException {
        out.writeInt(payload.length);
        out.write(payload);
        out.flush();
    }
}
