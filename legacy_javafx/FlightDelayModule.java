import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class FlightDelayModule {
    public static Node create(DataManager data, PredictionEngine engine) {
        TextField flight = Ui.field("AI245");
        TextField airline = Ui.field("Air India");
        TextField departure = Ui.field("DEL");
        TextField arrival = Ui.field("BOM");
        TextField scheduledDeparture = Ui.field("18:30");
        TextField scheduledArrival = Ui.field("20:35");
        TextField previousDelay = Ui.field("18");
        TextField turnaround = Ui.field("72");
        TextField passengers = Ui.field("268");
        TextField weather = Ui.field("0.68");
        Label result = Ui.value("Enter flight details and calculate a baseline estimate.");
        Label factors = Ui.small("Rule-based estimate using weighted operational factors.");
        var calculate = Ui.button("Calculate delay risk");
        calculate.setOnAction(event -> {
            try {
                int delay = Integer.parseInt(previousDelay.getText().trim());
                int turn = Integer.parseInt(turnaround.getText().trim());
                int load = Integer.parseInt(passengers.getText().trim());
                double weatherRisk = Double.parseDouble(weather.getText().trim());
                if (delay < 0 || turn < 0 || load < 0 || weatherRisk < 0 || weatherRisk > 1) {
                    throw new IllegalArgumentException("Use non-negative values and weather risk from 0 to 1.");
                }
                PredictionEngine.Prediction prediction = engine.predictFlight(delay, turn, load, weatherRisk, true);
                Ui.setResult(result, "Flight " + flight.getText() + "  |  Delay probability: "
                        + Math.round(prediction.probability() * 100) + "%  |  Expected delay: "
                        + prediction.expectedMinutes() + " min  |  Risk: " + prediction.risk());
                factors.setText("Contributing factors: " + String.join(" • ", prediction.factors())
                        + "   |   " + airline.getText() + " · " + departure.getText() + " → "
                        + arrival.getText() + " · " + scheduledDeparture.getText() + "–" + scheduledArrival.getText());
            } catch (NumberFormatException ex) {
                Ui.setResult(result, "Invalid input: enter numeric values for delay, turnaround, passengers and weather risk.");
            } catch (IllegalArgumentException ex) {
                Ui.setResult(result, "Invalid input: " + ex.getMessage());
            }
        });
        VBox form = Ui.card("Flight details & baseline prediction",
                Ui.formGrid(Ui.labeled("Flight number", flight), Ui.labeled("Airline", airline),
                        Ui.labeled("Departure airport", departure), Ui.labeled("Arrival airport", arrival),
                        Ui.labeled("Scheduled departure", scheduledDeparture), Ui.labeled("Scheduled arrival", scheduledArrival),
                        Ui.labeled("Previous delay (min)", previousDelay), Ui.labeled("Turnaround (min)", turnaround),
                        Ui.labeled("Passenger count", passengers), Ui.labeled("Weather risk (0–1)", weather)),
                calculate, result, factors);

        Map<String, Double> delayByAirline = data.getFlights().stream().collect(Collectors.groupingBy(
                FlightData::airline, LinkedHashMap::new,
                Collectors.averagingInt(FlightData::previousDelayMinutes)));
        Map<String, Long> severity = data.getFlights().stream().collect(Collectors.groupingBy(
                f -> f.previousDelayMinutes() < 15 ? "Low <15m" : f.previousDelayMinutes() < 45 ? "Moderate" : "High ≥45m",
                LinkedHashMap::new, Collectors.counting()));
        Map<String, Double> delayByTime = data.getFlights().stream().collect(Collectors.groupingBy(
                f -> String.format("%02d:00", f.scheduledDeparture().getHour()),
                LinkedHashMap::new, Collectors.averagingInt(FlightData::previousDelayMinutes)));
        return Ui.module("Flight Delay Prediction & Analysis",
                "Rule-based operational estimate. Demonstration / simulated data; not a trained machine-learning model.",
                form, Ui.charts(Ui.barChart("Average delay by airline (minutes)", delayByAirline),
                        Ui.barChart("Delay-risk distribution", severity)),
                Ui.card("Delay pattern by scheduled departure hour", Ui.barChart("Average delay by time", delayByTime)));
    }
}
