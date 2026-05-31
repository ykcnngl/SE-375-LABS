import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class LAB0 {

    public static void main(String[] args) {

        HashMap<Integer, Integer> idCounts = new HashMap<>();
        ArrayList<Integer> finalList = new ArrayList<>();

        try (BufferedReader input = new BufferedReader(new FileReader("input.txt"))) {
            String sizeLine = input.readLine();

            for (int i = 0; i < 3; i++) {
                String line = input.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    String[] tokens = line.trim().split("\\s+");
                    for (String token : tokens) {
                        int id = Integer.parseInt(token);
                        idCounts.put(id, idCounts.getOrDefault(id, 0) + 1);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            return;
        }

        for (Integer id : idCounts.keySet()) {
            if (idCounts.get(id) >= 2) {
                finalList.add(id);
            }
        }

        Collections.sort(finalList);

        System.out.println(finalList.size());
        for (int i = 0; i < finalList.size(); i++) {
            System.out.print(finalList.get(i) + (i == finalList.size() - 1 ? "" : " "));
        }
        System.out.println();
    }
}