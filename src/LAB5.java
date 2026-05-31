import java.io.File;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class DistrictTask implements Runnable {
    private String fileName;
    private HashMap<String, Integer> districtMap;
    private HashMap<String, Integer> dateMap;
    private Lock lock1, lock2;
    private boolean lockTogether;

    public DistrictTask(String fileName, HashMap<String, Integer> dMap, HashMap<String, Integer> tMap, Lock l1, Lock l2, boolean lockTogether) {
        this.fileName = fileName;
        this.districtMap = dMap;
        this.dateMap = tMap;
        this.lock1 = l1;
        this.lock2 = l2;
        this.lockTogether = lockTogether;
    }

    @Override
    public void run() {
        try {
            File file = new File(fileName);
            Scanner scanner = new Scanner(file);
            if (scanner.hasNextLine()) scanner.nextLine();

            String districtName = fileName.replace(".csv", "");

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] values = line.split(",");
                String date = values[0].trim();
                int passengers = Integer.parseInt(values[3].trim());

                if (lockTogether) {
                    lock1.lock();
                    lock2.lock();
                    try {
                        districtMap.put(districtName, districtMap.getOrDefault(districtName, 0) + passengers);
                        dateMap.put(date, dateMap.getOrDefault(date, 0) + passengers);
                    } finally {
                        lock2.unlock();
                        lock1.unlock();
                    }
                } else {
                    lock1.lock();
                    try {
                        districtMap.put(districtName, districtMap.getOrDefault(districtName, 0) + passengers);
                    } finally {
                        lock1.unlock();
                    }

                    lock2.lock();
                    try {
                        dateMap.put(date, dateMap.getOrDefault(date, 0) + passengers);
                    } finally {
                        lock2.unlock();
                    }
                }
            }
            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

public class LAB5 {
    static String[] fileNames = {"Konak.csv", "Gaziemir.csv", "Balcova.csv", "Bornova.csv", "Karabaglar.csv", "Karsiyaka.csv", "Seferihisar.csv", "Urla.csv"};

    static class RunResult {
        long elapsedNs;
        HashMap<String, Integer> districtMap;
        HashMap<String, Integer> dateMap;

        RunResult(long elapsedNs, HashMap<String, Integer> districtMap, HashMap<String, Integer> dateMap) {
            this.elapsedNs = elapsedNs;
            this.districtMap = districtMap;
            this.dateMap = dateMap;
        }
    }

    public static RunResult executeTest(boolean lockTogether) throws InterruptedException {
        HashMap<String, Integer> districtMap = new HashMap<>();
        HashMap<String, Integer> dateMap = new HashMap<>();
        Lock l1 = new ReentrantLock();
        Lock l2 = new ReentrantLock();

        ExecutorService executor = Executors.newFixedThreadPool(10);

        long startTime = System.nanoTime();

        for (String file : fileNames) {
            executor.execute(new DistrictTask(file, districtMap, dateMap, l1, l2, lockTogether));
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        long endTime = System.nanoTime();
        return new RunResult(endTime - startTime, districtMap, dateMap);
    }

    public static void main(String[] args) throws InterruptedException {
        int iterations = 50;
        long totalV1 = 0;
        long totalV2 = 0;

        for (int i = 0; i < iterations; i++) {
            totalV1 += executeTest(true).elapsedNs;
            totalV2 += executeTest(false).elapsedNs;
        }

        double avgMsV1 = (totalV1 / (double) iterations) / 1_000_000.0;  // Convert to milliseconds
        double avgMsV2 = (totalV2 / (double) iterations) / 1_000_000.0;

        System.out.println("Average Time (Locking Together): " + avgMsV1 + " ms");
        System.out.println("Average Time (Locking Separately): " + avgMsV2 + " ms");

        RunResult sample = executeTest(false);
        System.out.println("\nThreads are complete. District Statistics:");
        for (String district : sample.districtMap.keySet()) {
            System.out.println(district + " " + sample.districtMap.get(district));
        }
    }
}