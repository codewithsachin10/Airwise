import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.List;

public class PassengerCrowdModule {
    public static Node create(DataManager data, PredictionEngine engine) {
        TextField count = Ui.field("1850");
        TextField arrivals = Ui.field("14");
        TextField departures = Ui.field("18");
        TextField counters = Ui.field("14");
        TextField lanes = Ui.field("7");
        CheckBox peak = new CheckBox("Peak hour");
        peak.setSelected(true);
        CheckBox holiday = new CheckBox("Holiday");
        Label result = Ui.value("Estimate terminal crowd and queue times.");
        var calculate = Ui.button("Predict crowd & queues");
        calculate.setOnAction(event -> {
            try {
                int passengerCount = Integer.parseInt(count.getText().trim());
                int arrivalCount = Integer.parseInt(arrivals.getText().trim());
                int departureCount = Integer.parseInt(departures.getText().trim());
                int counterCount = Integer.parseInt(counters.getText().trim());
                int laneCount = Integer.parseInt(lanes.getText().trim());
                if (passengerCount < 0 || arrivalCount < 0 || departureCount < 0 || counterCount < 1 || laneCount < 1) {
                    throw new IllegalArgumentException("Counts cannot be negative; counters and lanes must be at least one.");
                }
                PredictionEngine.CrowdPrediction prediction = engine.predictCrowd(passengerCount, arrivalCount,
                        departureCount, peak.isSelected(), holiday.isSelected(), counterCount, laneCount);
                Ui.setResult(result, "Expected passengers: " + String.format("%,d", prediction.expectedPassengers())
                        + "  |  Crowd: " + prediction.level() + "  |  Security queue: "
                        + prediction.securityQueueMinutes() + " min  |  Check-in: " + prediction.checkInQueueMinutes()
                        + " min  |  Recommendation: " + (prediction.extraLanes() == 0
                        ? "Maintain current lane staffing." : "Open " + prediction.extraLanes() + " additional security lane(s)."));
            } catch (NumberFormatException ex) {
                Ui.setResult(result, "Invalid input: enter whole numbers for passengers, flights, counters and lanes.");
            } catch (IllegalArgumentException ex) {
                Ui.setResult(result, "Invalid input: " + ex.getMessage());
            }
        });
        VBox form = Ui.card("Terminal inputs & crowd prediction",
                Ui.formGrid(Ui.labeled("Passenger count", count), Ui.labeled("Flight arrivals", arrivals),
                        Ui.labeled("Flight departures", departures), Ui.labeled("Active counters", counters),
                        Ui.labeled("Security lanes", lanes), Ui.labeled("Operating period", peak), Ui.labeled("Calendar", holiday)),
                calculate, result);
        List<String> trendLabels = data.getPassengers().stream().limit(12)
                .map(p -> p.timestamp().toLocalTime().toString().substring(0, 5)).toList();
        List<Integer> trendValues = data.getPassengers().stream().limit(12).map(PassengerData::passengerCount).toList();
        List<Integer> queues = data.getPassengers().stream().limit(12).map(p -> engine.predictCrowd(p.passengerCount(),
                p.arrivals(), p.departures(), p.timestamp().getHour() >= 16 && p.timestamp().getHour() <= 20,
                p.holiday(), p.activeCounters(), p.securityLanes()).securityQueueMinutes()).toList();
        return Ui.module("Airport Passenger Crowd & Queue Prediction",
                "Forecast terminal pressure from passenger volumes, flight movements, peak periods and service capacity.",
                form, Ui.charts(Ui.lineChart("Passenger volume trend", trendLabels, trendValues),
                        Ui.lineChart("Estimated security queue (minutes)", trendLabels, queues)),
                Ui.card("Terminal congestion indicator",
                        Ui.value("Queue estimates are capacity-based indicators from simulated sample records. Use the form above to test counter and lane changes.")));
    }
}
