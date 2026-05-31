
import java.io.File;
import java.util.HashMap;
import java.util.Scanner;

class DistrictThread extends Thread {
    String name;
    double total = 0;
    double avg = 0;

    public DistrictThread(String name) {
        this.name = name;
    }

    public void run() {
        try {
            File file = new File(name + ".csv");
            Scanner scanner = new Scanner(file);
            int days = 0;

            if (scanner.hasNextLine()) scanner.nextLine();

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] values = line.split(",");
                total += Double.parseDouble(values[3].trim());
                days++;
            }
            scanner.close();
            avg = total / days;

            System.out.println("Thread finished: " + name + " - Total: " + total + ", Average: " + avg);
        } catch (Exception e) {}
    }
}

public class LAB2 {
    public static void main(String[] args) throws InterruptedException {
        String[] names = {"Konak", "Karsiyaka", "Seferihisar", "Balcova", "Urla", "Gaziemir", "Karabaglar", "Bornova"};
        DistrictThread[] threads = new DistrictThread[names.length];
        HashMap<String, double[]> map = new HashMap<>();

        for (int i = 0; i < names.length; i++) {
            threads[i] = new DistrictThread(names[i]);
            threads[i].start();
        }

        double allTotal = 0;
        double allAvg = 0;

        for (DistrictThread t : threads) {
            t.join();
            map.put(t.name, new double[]{t.total, t.avg});
            allTotal += t.total;
            allAvg += t.avg;
        }

        System.out.println("\nDistrict        Total Passengers    Avg Passengers");
        System.out.println("--------        ----------------    --------------");

        for (String n : names) {
            double[] s = map.get(n);
            System.out.println(n + "\t\t" + (int)s[0] + "\t\t" + s[1]);
        }

        System.out.println("--------------------------------------------------");
        System.out.println("All districts\t" + (int)allTotal + "\t\t" + allAvg);
    }
}

