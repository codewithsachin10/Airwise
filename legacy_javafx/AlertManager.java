import java.util.ArrayList;
import java.util.List;

public class AlertManager {
    public record Alert(String title, String message, String severity) {
    }

    public List<Alert> generate(DataManager data, PredictionEngine predictor,
                                ComplaintNLPAnalyzer complaintAnalyzer) {
        List<Alert> alerts = new ArrayList<>();
        FlightData flight = data.getFlights().get(0);
        PredictionEngine.Prediction flightRisk = predictor.predictFlight(flight.previousDelayMinutes(),
                flight.turnaroundMinutes(), flight.passengerCount(), flight.weatherRisk(), true);
        if (flightRisk.probability() >= 0.6) {
            alerts.add(new Alert("HIGH DELAY RISK", "Flight " + flight.flightNumber() + " has "
                    + Math.round(flightRisk.probability() * 100) + "% delay probability.", "HIGH"));
        }
        WeatherData currentWeather = data.getWeather().get(0);
        PredictionEngine.WeatherPrediction weatherRisk = predictor.predictWeather(currentWeather.rainfallMm(),
                currentWeather.windSpeedKph(), currentWeather.visibilityKm(), currentWeather.thunderstorm());
        if (weatherRisk.probability() >= 0.45) {
            alerts.add(new Alert("WEATHER ALERT", "Low visibility or severe weather may disrupt operations.", weatherRisk.risk()));
        }
        PassengerData latest = data.getPassengers().get(0);
        PredictionEngine.CrowdPrediction crowd = predictor.predictCrowd(latest.passengerCount(), latest.arrivals(),
                latest.departures(), true, latest.holiday(), latest.activeCounters(), latest.securityLanes());
        if (crowd.securityQueueMinutes() > 20) {
            alerts.add(new Alert("CROWD ALERT", "Security queue expected to reach "
                    + crowd.securityQueueMinutes() + " minutes.", crowd.level()));
        }
        TaxiData taxi = data.getTaxis().get(0);
        PredictionEngine.TaxiPrediction taxiRisk = predictor.predictTaxi(taxi.arrivals(), taxi.passengerCount(),
                true, taxi.holiday(), taxi.weatherCondition(), taxi.availableTaxis());
        if (taxiRisk.shortage() > 0) alerts.add(new Alert("TAXI SHORTAGE",
                "Expected demand exceeds supply by " + taxiRisk.shortage() + " taxis.", taxiRisk.risk()));
        long unavailable = data.getEmergencies().stream().filter(e -> !e.ambulanceAvailable()).count();
        if (unavailable > 8) alerts.add(new Alert("MEDICAL RESOURCE ALERT",
                "Historical records indicate frequent ambulance unavailability.", "HIGH"));
        long baggage = data.getComplaints().stream()
                .filter(c -> complaintAnalyzer.analyze(c.text()).category().equals("Baggage")).count();
        if (baggage >= 5) alerts.add(new Alert("CUSTOMER SERVICE ALERT",
                baggage + " baggage complaints detected in the demonstration dataset.", "MODERATE"));
        return List.copyOf(alerts);
    }
}
