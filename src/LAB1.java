import java.io.File;
import java.util.HashMap;
import java.util.Scanner;

public class LAB1 {

    public static void main(String[] args) {

        String[] districts = new String[8];
        districts[0] = "Konak";
        districts[1] = "Karsiyaka";
        districts[2] = "Seferihisar";
        districts[3] = "Balcova";
        districts[4] = "Urla";
        districts[5] = "Gaziemir";
        districts[6] = "Karabaglar";
        districts[7] = "Bornova";

        HashMap<String, double[]> districtStatsMap = new HashMap<>();

        for (String district : districts) {

            String fileName = district + ".csv";

            double totalPassengers = 0;
            double totalDays = 0;

            try {
                File file = new File(fileName);
                Scanner scanner = new Scanner(file);

                boolean hasFirstLine = scanner.hasNextLine();

                if (hasFirstLine) {
                    String headerLine = scanner.nextLine();
                }

                while (scanner.hasNextLine()) {

                    String line = scanner.nextLine();
                    String[] values = line.split(",");
                    String passengerString = values[3];
                    passengerString = passengerString.trim();

                    double passengers = Double.parseDouble(passengerString);
                    totalPassengers = totalPassengers + passengers;
                    totalDays = totalDays + 1;
                }

                scanner.close();

                if (totalDays > 0) {

                    double averagePassengers = totalPassengers / totalDays;

                    double[] stats = new double[2];
                    stats[0] = totalPassengers;
                    stats[1] = averagePassengers;

                    districtStatsMap.put(district, stats);
                }

            } catch (Exception e) {
            }
        }

        System.out.println("Here are the statistics for the number of passengers:");
        System.out.println("");

        String head1 = "District";
        String head2 = "Total Passengers";
        String head3 = "Avg Passengers";

        String head = head1 + "        " + head2 + "   " + head3;
        System.out.println(head);

        String line1 = "--------";
        String line2 = "----------------";
        String line3 = "--------------";

        String dividerRow = line1 + "       " + line2 + "   " + line3;
        System.out.println(dividerRow);

        for (String district : districts) {

            boolean hasDistrict = districtStatsMap.containsKey(district);

            if (hasDistrict) {

                double[] stats = districtStatsMap.get(district);

                String outputLine = distInfo(district, stats);

                System.out.println(outputLine);
            }
        }
    }

    public static String distInfo(String district, double[] stats) {
        double totalDouble = stats[0];
        double avgDouble = stats[1];

        int totalInt = (int) totalDouble;

        double avgMultiplied = avgDouble * 100.0;
        double avgRounded = Math.round(avgMultiplied);
        double finalAvg = avgRounded / 100.0;

        String districtName = district;

        int nameLength = districtName.length();

        String space = "            ";

        if (nameLength > 7) {
            space = "           ";
        }

        String space2 = "           ";

        String outputLine = districtName + space + totalInt + space2 + finalAvg;
        return outputLine;
    }
}