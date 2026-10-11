import java.util.ArrayList;
import java.util.List;

public class RecommendationEngine {
    public List<String> generate(DataManager data, PredictionEngine predictor,
                                 ComplaintNLPAnalyzer complaintAnalyzer) {
        List<String> recommendations = new ArrayList<>();
        PassengerData passenger = data.getPassengers().get(0);
        PredictionEngine.CrowdPrediction crowd = predictor.predictCrowd(passenger.passengerCount(),
                passenger.arrivals(), passenger.departures(), true, passenger.holiday(),
                passenger.activeCounters(), passenger.securityLanes());
        if (crowd.extraLanes() > 0) recommendations.add("Open " + crowd.extraLanes()
                + " additional security lane(s) to reduce the predicted " + crowd.securityQueueMinutes() + "-minute queue.");
        TaxiData taxi = data.getTaxis().get(0);
        PredictionEngine.TaxiPrediction taxiPrediction = predictor.predictTaxi(taxi.arrivals(), taxi.passengerCount(),
                true, taxi.holiday(), taxi.weatherCondition(), taxi.availableTaxis());
        if (taxiPrediction.shortage() > 0) recommendations.add("Arrange " + taxiPrediction.shortage()
                + " additional taxis for the predicted arrival demand.");
        long unavailable = data.getEmergencies().stream().filter(e -> !e.medicalTeamAvailable()).count();
        if (unavailable > 5) recommendations.add("Review medical shift coverage; " + unavailable
                + " sampled incidents recorded a team availability gap.");
        WeatherData weather = data.getWeather().get(0);
        PredictionEngine.WeatherPrediction weatherPrediction = predictor.predictWeather(weather.rainfallMm(),
                weather.windSpeedKph(), weather.visibilityKm(), weather.thunderstorm());
        if (weatherPrediction.probability() >= 0.4) recommendations.add("Prepare weather contingency procedures: "
                + weatherPrediction.impact().toLowerCase() + ".");
        long baggage = data.getComplaints().stream()
                .filter(c -> complaintAnalyzer.analyze(c.text()).category().equals("Baggage")).count();
        if (baggage > 0) recommendations.add("Prioritize baggage-service review; " + baggage
                + " baggage cases appear in the sample.");
        FlightData flight = data.getFlights().get(0);
        PredictionEngine.Prediction flightPrediction = predictor.predictFlight(flight.previousDelayMinutes(),
                flight.turnaroundMinutes(), flight.passengerCount(), flight.weatherRisk(), true);
        if (flightPrediction.probability() >= 0.45) recommendations.add("Monitor " + flight.flightNumber()
                + " and similar high-risk departures; estimated risk is "
                + Math.round(flightPrediction.probability() * 100) + "%.");
        if (recommendations.isEmpty()) recommendations.add("Continue routine monitoring; no major thresholds are currently exceeded.");
        return List.copyOf(recommendations);
    }
}
