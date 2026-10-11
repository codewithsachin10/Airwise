import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;

public class MedicalEmergencyModule {
    public static Node create(DataManager data, PredictionEngine engine) {
        TextField location = Ui.field("Terminal 1");
        TextField response = Ui.field("7");
        var type = Ui.choices("Fainting", "Injury", "Cardiac Emergency", "Breathing Problem", "Accident", "Other");
        var team = Ui.choices("Available", "Unavailable");
        var ambulance = Ui.choices("Available", "Unavailable");
        Label result = Ui.value("Estimate resources required using recorded incident frequency and current availability.");
        var predict = Ui.button("Assess medical resources");
        predict.setOnAction(event -> {
            try {
                int responseTime = Integer.parseInt(response.getText().trim());
                if (responseTime < 0) throw new IllegalArgumentException("Response time cannot be negative.");
                PredictionEngine.MedicalPrediction prediction = engine.predictMedical(data.getEmergencies().size(),
                        true, team.getValue().equals("Available"), ambulance.getValue().equals("Available"));
                Ui.setResult(result, "Emergency risk: " + prediction.risk() + "  |  Required medical staff: "
                        + prediction.requiredStaff() + "  |  Required ambulances: " + prediction.requiredAmbulances()
                        + "  |  Current team: " + team.getValue() + "  |  Ambulance: " + ambulance.getValue()
                        + "  |  Recommendation: " + (team.getValue().equals("Unavailable")
                        || ambulance.getValue().equals("Unavailable") ? "Deploy additional medical resources." : "Maintain on-call coverage.")
                        + "  (" + location.getText() + ", " + type.getValue() + ", response input " + responseTime + " min)");
            } catch (NumberFormatException ex) {
                Ui.setResult(result, "Invalid input: response time must be a whole number of minutes.");
            } catch (IllegalArgumentException ex) {
                Ui.setResult(result, "Invalid input: " + ex.getMessage());
            }
        });
        VBox form = Ui.card("Resource assessment & incident details",
                Ui.formGrid(Ui.labeled("Location", location), Ui.labeled("Emergency type", type),
                        Ui.labeled("Response time (min)", response), Ui.labeled("Medical team", team),
                        Ui.labeled("Ambulance", ambulance)),
                predict, result);

        TableView<MedicalEmergencyData> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(data.getEmergencies()));
        TableColumn<MedicalEmergencyData, String> id = new TableColumn<>("ID");
        id.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().emergencyId()));
        TableColumn<MedicalEmergencyData, String> where = new TableColumn<>("Location");
        where.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().location()));
        TableColumn<MedicalEmergencyData, String> emergency = new TableColumn<>("Type");
        emergency.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().emergencyType()));
        TableColumn<MedicalEmergencyData, String> timing = new TableColumn<>("Response");
        timing.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().responseMinutes() + " min"));
        TableColumn<MedicalEmergencyData, String> outcome = new TableColumn<>("Outcome");
        outcome.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().outcome()));
        table.getColumns().addAll(id, where, emergency, timing, outcome);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(280);
        Map<String, Long> locations = data.getEmergencies().stream().collect(java.util.stream.Collectors.groupingBy(
                MedicalEmergencyData::location, LinkedHashMap::new, java.util.stream.Collectors.counting()));
        Map<String, Long> types = data.getEmergencies().stream().collect(java.util.stream.Collectors.groupingBy(
                MedicalEmergencyData::emergencyType, LinkedHashMap::new, java.util.stream.Collectors.counting()));
        Map<String, Long> responseTimes = new LinkedHashMap<>();
        responseTimes.put("0–5 min", data.getEmergencies().stream().filter(e -> e.responseMinutes() <= 5).count());
        responseTimes.put("6–10 min", data.getEmergencies().stream().filter(e -> e.responseMinutes() > 5 && e.responseMinutes() <= 10).count());
        responseTimes.put(">10 min", data.getEmergencies().stream().filter(e -> e.responseMinutes() > 10).count());
        return Ui.module("Airport Medical Emergency Response Analytics",
                "Monitor sample incidents, response times and baseline medical-resource requirements.",
                form, Ui.charts(Ui.barChart("Emergency hotspots", locations), Ui.barChart("Emergency type frequency", types)),
                Ui.charts(Ui.barChart("Response-time distribution", responseTimes),
                        Ui.card("Sample emergency trend", Ui.value("Incidents recorded: " + data.getEmergencies().size()
                                + "  |  Average response time: " + String.format("%.1f", data.getEmergencies().stream()
                                .mapToInt(MedicalEmergencyData::responseMinutes).average().orElse(0)) + " min"))),
                Ui.card("Emergency records", table));
    }
}
