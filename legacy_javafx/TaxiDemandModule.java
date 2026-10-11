import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.List;

public class TaxiDemandModule {
    public static Node create(DataManager data, PredictionEngine engine) {
        TextField arrivals = Ui.field("16");
        TextField passengers = Ui.field("1450");
        TextField available = Ui.field("780");
        CheckBox peak = new CheckBox("Peak hour (18:00–20:00)");
        peak.setSelected(true);
        CheckBox holiday = new CheckBox("Holiday");
        var weather = Ui.choices("Clear", "Cloudy", "Rain", "Storm");
        Label result = Ui.value("Estimate taxi demand, shortage and passenger waiting time.");
        var calculate = Ui.button("Predict taxi demand");
        calculate.setOnAction(event -> {
            try {
                int flights = Integer.parseInt(arrivals.getText().trim());
                int passengerCount = Integer.parseInt(passengers.getText().trim());
                int taxis = Integer.parseInt(available.getText().trim());
                if (flights < 0 || passengerCount < 0 || taxis < 0) {
                    throw new IllegalArgumentException("Values cannot be negative.");
                }
                PredictionEngine.TaxiPrediction prediction = engine.predictTaxi(flights, passengerCount,
                        peak.isSelected(), holiday.isSelected(), weather.getValue(), taxis);
                Ui.setResult(result, "Expected demand: " + String.format("%,d", prediction.demand())
                        + "  |  Available taxis: " + String.format("%,d", taxis)
                        + "  |  Shortage: " + String.format("%,d", prediction.shortage())
                        + "  |  Demand risk: " + prediction.risk()
                        + "  |  Passenger wait: ~" + prediction.waitMinutes() + " min"
                        + (prediction.shortage() > 0 ? "  |  Arrange additional taxi supply." : "  |  Current supply covers demand."));
            } catch (NumberFormatException ex) {
                Ui.setResult(result, "Invalid input: enter whole numbers for arrivals, passengers and available taxis.");
            } catch (IllegalArgumentException ex) {
                Ui.setResult(result, "Invalid input: " + ex.getMessage());
            }
        });
        VBox form = Ui.card("Arrival and ground-transport inputs",
                Ui.formGrid(Ui.labeled("Flight arrivals", arrivals), Ui.labeled("Passenger count", passengers),
                        Ui.labeled("Available taxis", available), Ui.labeled("Weather condition", weather),
                        Ui.labeled("Time period", peak), Ui.labeled("Calendar", holiday)),
                calculate, result);
        List<String> labels = data.getTaxis().stream().limit(12)
                .map(t -> t.timestamp().toLocalTime().toString().substring(0, 5)).toList();
        List<Integer> demand = data.getTaxis().stream().limit(12).map(t -> engine.predictTaxi(t.arrivals(),
                t.passengerCount(), t.timestamp().getHour() >= 16 && t.timestamp().getHour() <= 20,
                t.holiday(), t.weatherCondition(), t.availableTaxis()).demand()).toList();
        List<Integer> supply = data.getTaxis().stream().limit(12).map(TaxiData::availableTaxis).toList();
        return Ui.module("Airport Taxi Demand Prediction",
                "Estimate landside ground-transport demand and the potential passenger wait using baseline rules.",
                form, Ui.charts(Ui.lineChart("Taxi demand trend", labels, demand),
                        Ui.lineChart("Taxi availability trend", labels, supply)),
                Ui.card("Demand vs supply", Ui.value("Peak supply planning should prioritize the busiest arrival windows; the charted records are simulated.")));
    }
}
