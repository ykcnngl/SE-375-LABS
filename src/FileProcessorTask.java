import java.io.*;
import java.util.*;
import java.util.concurrent.locks.Lock;

public class FileProcessorTask implements Runnable {
    private final File csvFile;
    private final Map<String, LAB12.DayStats> statistics;
    private final Lock lock;
    
    public FileProcessorTask(File csvFile, Map<String, LAB12.DayStats> statistics, Lock lock) {
        this.csvFile = csvFile;
        this.statistics = statistics;
        this.lock = lock;
    }
    
    @Override
    public void run() {
        System.out.println("Parsing " + csvFile.getName() + "...");
        
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 4) {
                    continue;
                }
                
                String date = parts[0].trim();
                String dayOfWeek = parts[1].trim();
                long numPassengers = Long.parseLong(parts[3].trim());
                
                lock.lock();
                try {
                    LAB12.DayStats stats = statistics.get(dayOfWeek);
                    if (stats != null) {
                        stats.totalPassengers += numPassengers;
                        stats.uniqueDates.add(date);
                    }
                } finally {
                    lock.unlock();
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file " + csvFile.getName() + ": " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Error parsing number in file " + csvFile.getName() + ": " + e.getMessage());
        }
    }
}
