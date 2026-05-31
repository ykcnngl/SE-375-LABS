import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

public class LAB12 {
    private static final String CSV_FILE_PATH = ".";
    private static final String OUTPUT_FILE = "results.enc";
    private static final int THREAD_POOL_SIZE = 10;
    private static final int COL_DAY = 16;
    private static final int COL_TOTAL = 18;
    private static final int COL_COUNT = 16;
    private static final int COL_AVG = 16;
    private static final String[] DISTRICTS = {
        "Konak", "Karsiyaka", "Seferihisar", "Balcova",
        "Urla", "Gaziemir", "Karabaglar", "Bornova"
    };
    private static final String[] DAYS = {
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };
    

    static class DayStats {
        long totalPassengers = 0;
        Set<String> uniqueDates = new HashSet<>();
    }
    
    public static void main(String[] args) throws Exception {
        Map<String, DayStats> statistics = new HashMap<>();
        initializeStatistics(statistics);
        
        Lock lock = new ReentrantLock();
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Future<?>> futures = new ArrayList<>();

        List<File> csvFiles = new ArrayList<>();
        for (String district : DISTRICTS) {
            File csvFile = new File(CSV_FILE_PATH, district + ".csv");
            if (csvFile.exists() && csvFile.isFile()) {
                csvFiles.add(csvFile);
            } else {
                System.out.println("Missing CSV: " + csvFile.getPath());
            }
        }
        
        if (csvFiles.isEmpty()) {
            System.out.println("No CSV files found!");
            executor.shutdown();
            return;
        }
        

        for (File csvFile : csvFiles) {
            Future<?> future = executor.submit(new FileProcessorTask(csvFile, statistics, lock));
            futures.add(future);
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        
        executor.shutdown();
        System.out.println("\nThreads are complete. Here are the statistics for the number of passengers:\n");
        displayTableHeaders();
        displayResults(statistics);
        encryptAndSaveResults(statistics);
        decryptAndDisplayResults();
    }
    
    private static void initializeStatistics(Map<String, DayStats> stats) {
        for (String day : DAYS) {
            stats.put(day, new DayStats());
        }
    }
    
    private static void displayTableHeaders() {
        String header = padRight("Day of the week", COL_DAY)
            + padRight("Total Passengers", COL_TOTAL)
            + padRight("Number of days", COL_COUNT)
            + padRight("Avg Passengers", COL_AVG);
        String line = padRight("---------------", COL_DAY)
            + padRight("----------------", COL_TOTAL)
            + padRight("--------------", COL_COUNT)
            + padRight("--------------", COL_AVG);
        System.out.println(header);
        System.out.println(line);
    }
    
    private static void displayResults(Map<String, DayStats> statistics) {
        for (String day : DAYS) {
            DayStats stats = statistics.get(day);
            double average = stats.uniqueDates.size() > 0 ? 
                (double) stats.totalPassengers / stats.uniqueDates.size() : 0;
            String row = padRight(day, COL_DAY)
                + padLeft(String.valueOf(stats.totalPassengers), COL_TOTAL)
                + padLeft(String.valueOf(stats.uniqueDates.size()), COL_COUNT)
                + padLeft(formatOneDecimal(average), COL_AVG);
            System.out.println(row);
        }
    }
    
    private static void encryptAndSaveResults(Map<String, DayStats> statistics) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey secretKey = keyGen.generateKey();

        StringBuilder content = new StringBuilder();
        content.append("Day of the week\tTotal Passengers\tNumber of days\tAvg Passengers\n");
        content.append("---------------\t----------------\t--------------\t--------------\n");
        
        for (String day : DAYS) {
            DayStats stats = statistics.get(day);
            double average = stats.uniqueDates.size() > 0 ? 
                (double) stats.totalPassengers / stats.uniqueDates.size() : 0;
            
            content.append(day).append("\t")
                   .append(stats.totalPassengers).append("\t")
                   .append(stats.uniqueDates.size()).append("\t")
                   .append(formatOneDecimal(average)).append("\n");
        }
        
        String contentToHash = content.toString().trim();
        
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] messageDigest = digest.digest(contentToHash.getBytes(StandardCharsets.UTF_8));
        String digestHex = bytesToHex(messageDigest);
        
        StringBuilder finalContent = new StringBuilder();
        finalContent.append(contentToHash).append("\n[MESSAGE_DIGEST]\n").append(digestHex);
        
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedContent = cipher.doFinal(finalContent.toString().getBytes(StandardCharsets.UTF_8));
        
        FileOutputStream fos = new FileOutputStream(OUTPUT_FILE);
        fos.write(encryptedContent);
        fos.close();
        
        FileOutputStream keyFos = new FileOutputStream("secret.key");
        keyFos.write(secretKey.getEncoded());
        keyFos.close();
        
        System.out.println("\n\nResults encrypted and saved to " + OUTPUT_FILE);
    }
    
    private static void decryptAndDisplayResults() throws Exception {
        byte[] decodedKey = Files.readAllBytes(Paths.get("secret.key"));
        SecretKey decodedSecretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        
        byte[] encryptedContent = Files.readAllBytes(Paths.get(OUTPUT_FILE));
        
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, decodedSecretKey);
        byte[] decryptedContent = cipher.doFinal(encryptedContent);
        String decryptedText = new String(decryptedContent, StandardCharsets.UTF_8);
        
        int digestIndex = decryptedText.indexOf("[MESSAGE_DIGEST]");
        String contentPart = decryptedText.substring(0, digestIndex).trim();
        String storedDigest = decryptedText.substring(digestIndex + "[MESSAGE_DIGEST]".length()).trim();
        
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] messageDigest = digest.digest(contentPart.getBytes(StandardCharsets.UTF_8));
        String calculatedDigest = bytesToHex(messageDigest);
        
        System.out.println("\n\nDecrypted and displayed results.");
        System.out.println("\nMessage Digest Verification: " + 
            (storedDigest.equals(calculatedDigest) ? "PASSED" : "FAILED"));
    }

    private static String formatOneDecimal(double value) {
        long scaled = Math.round(value * 10.0);
        long intPart = scaled / 10;
        long fracPart = Math.abs(scaled % 10);
        return intPart + "." + fracPart;
    }

    private static String padRight(String text, int width) {
        int pad = width - text.length();
        if (pad <= 0) {
            return text;
        }
        return text + " ".repeat(pad);
    }

    private static String padLeft(String text, int width) {
        int pad = width - text.length();
        if (pad <= 0) {
            return text;
        }
        return " ".repeat(pad) + text;
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
