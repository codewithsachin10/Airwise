import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DataManager {
    private final List<FlightData> flights = new ArrayList<>();
    private final List<WeatherData> weather = new ArrayList<>();
    private final List<PassengerData> passengers = new ArrayList<>();
    private final List<TaxiData> taxis = new ArrayList<>();
    private final List<MedicalEmergencyData> emergencies = new ArrayList<>();
    private final List<ComplaintData> complaints = new ArrayList<>();

    public DataManager() {
        generateSampleData();
    }

    private void generateSampleData() {
        Random random = new Random(245);
        String[] airlines = {"Air India", "IndiGo", "Vistara", "Akasa Air", "SpiceJet"};
        String[] locations = {"Terminal 1", "Terminal 2", "Security", "Gate Area", "Baggage Hall"};
        String[] types = {"Fainting", "Injury", "Cardiac Emergency", "Breathing Problem", "Accident", "Other"};
        String[] complaintTexts = {
                "My baggage has not arrived and I have waited two hours.",
                "The flight was delayed and I missed my connection.",
                "The security queue was too long and slow.",
                "The restroom was dirty and needs cleaning.",
                "A staff member was rude when I asked for help.",
                "The food at the terminal was cold and poor.",
                "Parking was expensive and hard to find.",
                "The taxi shuttle was late. I waited a long time.",
                "Thank you, the staff were helpful and friendly.",
                "The check-in service was quick and excellent."
        };
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 50; i++) {
            int delay = random.nextInt(75);
            flights.add(new FlightData("AI" + (210 + i), airlines[i % airlines.length], "DEL", "BOM",
                    now.plusHours(i - 18), delay, 30 + random.nextInt(75),
                    120 + random.nextInt(200), random.nextDouble() * 0.9,
                    delay > 45 ? "Delayed" : "On time"));
            weather.add(new WeatherData(LocalDate.now().minusDays(i),
                    random.nextDouble() * 35, 5 + random.nextDouble() * 80,
                    0.5 + random.nextDouble() * 8, 18 + random.nextDouble() * 20, random.nextInt(10) == 0));
            passengers.add(new PassengerData(now.minusHours(i * 2L), 3 + random.nextInt(18),
                    4 + random.nextInt(20), 400 + random.nextInt(1800), 8 + random.nextInt(13),
                    4 + random.nextInt(9), random.nextInt(7) == 0));
            taxis.add(new TaxiData(now.minusHours(i * 2L), 2 + random.nextInt(20),
                    300 + random.nextInt(1500), 250 + random.nextInt(900),
                    random.nextInt(7) == 0, new String[]{"Clear", "Rain", "Cloudy", "Storm"}[random.nextInt(4)]));
            complaints.add(new ComplaintData("C-" + (1001 + i), complaintTexts[i % complaintTexts.length],
                    now.minusDays(random.nextInt(30)).minusHours(random.nextInt(24))));
        }
        for (int i = 0; i < 30; i++) {
            emergencies.add(new MedicalEmergencyData("M-" + (501 + i), locations[i % locations.length],
                    now.minusDays(random.nextInt(20)).minusHours(random.nextInt(24)), types[i % types.length],
                    3 + random.nextInt(15), random.nextInt(5) != 0, random.nextInt(4) != 0,
                    new String[]{"Treated", "Stabilized", "Transferred"}[random.nextInt(3)]));
        }
    }

    public List<FlightData> getFlights() { return List.copyOf(flights); }
    public List<WeatherData> getWeather() { return List.copyOf(weather); }
    public List<PassengerData> getPassengers() { return List.copyOf(passengers); }
    public List<TaxiData> getTaxis() { return List.copyOf(taxis); }
    public List<MedicalEmergencyData> getEmergencies() { return List.copyOf(emergencies); }
    public List<ComplaintData> getComplaints() { return List.copyOf(complaints); }
}
