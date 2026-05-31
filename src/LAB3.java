/*import java.io.File;
import java.util.HashMap;
import java.util.Scanner;
class DistrictThread extends Thread {
    private String fileName;
    private HashMap<String, double[]> sharedMap;
    public DistrictThread(String fileName, HashMap<String, double[]> sharedMap) {
        this.fileName = fileName;
        this.sharedMap = sharedMap;
    }
    @Override
    public void run() {
        try {
            System.out.println("Parsing " + fileName + "...");
            File file = new File(fileName);
            Scanner scanner = new Scanner(file);
            if (scanner.hasNextLine()) scanner.nextLine();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] values = line.split(",");
                String date = values[0].trim();
                int passengers = Integer.parseInt(values[3].trim());
                synchronized (sharedMap) {
                    if (!sharedMap.containsKey(date)) {
                        sharedMap.put(date, new double[]{passengers, 1});
                    } else {
                        double[] stats = sharedMap.get(date);
                        stats[0] += passengers;
                        stats[1] += 1;
                    }
                }
            }
            scanner.close();
        } catch (Exception e) {}
    }
}
public class LAB3 {
    public static void main(String[] args) throws InterruptedException {
        String[] files = {"Karabaglar.csv", "Konak.csv", "Gaziemir.csv", "Bornova.csv",
                "Urla.csv", "Karsiyaka.csv", "Balcova.csv", "Seferihisar.csv"};
        HashMap<String, double[]> statsMap = new HashMap<>();
        Thread[] threads = new Thread[files.length];
        for (int i = 0; i < files.length; i++) {
            threads[i] = new DistrictThread(files[i], statsMap);
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();
        }
        System.out.println("\nThreads are complete. Statistics:");
        for (String date : statsMap.keySet()) {
            double[] data = statsMap.get(date);
            double total = data[0];
            double avg = total / data[1];
            System.out.println(date + " " + (int)total + " " + avg);
        }
    }
}*/