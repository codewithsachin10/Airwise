import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AnalyticsEngine {
    public Map<String, Long> flightsByAirline(DataManager data) {
        return data.getFlights().stream().collect(Collectors.groupingBy(FlightData::airline, Collectors.counting()));
    }

    public Map<String, Long> emergenciesByLocation(DataManager data) {
        return data.getEmergencies().stream()
                .collect(Collectors.groupingBy(MedicalEmergencyData::location, Collectors.counting()));
    }

    public Map<String, Long> complaintsByCategory(DataManager data, ComplaintNLPAnalyzer analyzer) {
        return data.getComplaints().stream().map(c -> analyzer.analyze(c.text()).category())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    public double averageFlightDelay(DataManager data) {
        return data.getFlights().stream().mapToInt(FlightData::previousDelayMinutes).average().orElse(0);
    }

    public double averageResponseTime(DataManager data) {
        return data.getEmergencies().stream().mapToInt(MedicalEmergencyData::responseMinutes).average().orElse(0);
    }
}
