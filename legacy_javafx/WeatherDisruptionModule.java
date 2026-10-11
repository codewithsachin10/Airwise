import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;

public class WeatherDisruptionModule {
    public static Node create(DataManager data, PredictionEngine engine) {
        TextField rain = Ui.field("18");
        TextField wind = Ui.field("54");
        TextField visibility = Ui.field("3.2");
        TextField temperature = Ui.field("31");
        CheckBox thunder = new CheckBox("Thunderstorm active");
        Label result = Ui.value("Enter observed conditions to estimate disruption risk.");
        var calculate = Ui.button("Assess weather risk");
        calculate.setOnAction(event -> {
            try {
                double r = Double.parseDouble(rain.getText().trim());
                double w = Double.parseDouble(wind.getText().trim());
                double v = Double.parseDouble(visibility.getText().trim());
                double t = Double.parseDouble(temperature.getText().trim());
                if (r < 0 || w < 0 || v < 0 || t < -80 || t > 70) {
                    throw new IllegalArgumentException("Weather values are outside the supported range.");
                }
                PredictionEngine.WeatherPrediction prediction = engine.predictWeather(r, w, v, t, thunder.isSelected());
                Ui.setResult(result, "Weather risk: " + prediction.risk() + "  |  Disruption probability: "
                        + Math.round(prediction.probability() * 100) + "%  |  Expected impact: " + prediction.impact());
            } catch (NumberFormatException ex) {
                Ui.setResult(result, "Invalid input: rainfall, wind, visibility and temperature must be numeric.");
            } catch (IllegalArgumentException ex) {
                Ui.setResult(result, "Invalid input: " + ex.getMessage());
            }
        });
        VBox form = Ui.card("Current conditions & disruption estimate",
                Ui.formGrid(Ui.labeled("Rainfall (mm)", rain), Ui.labeled("Wind speed (km/h)", wind),
                        Ui.labeled("Visibility (km)", visibility), Ui.labeled("Temperature (°C)", temperature),
                        Ui.labeled("Thunderstorm", thunder)),
                calculate, result);
        Map<String, Double> rainDelays = new LinkedHashMap<>();
        rainDelays.put("Dry <5mm", averageDelay(data, w -> w.rainfallMm() < 5));
        rainDelays.put("Rain 5–20mm", averageDelay(data, w -> w.rainfallMm() >= 5 && w.rainfallMm() < 20));
        rainDelays.put("Heavy >20mm", averageDelay(data, w -> w.rainfallMm() >= 20));
        Map<String, Double> visibilityDelays = new LinkedHashMap<>();
        visibilityDelays.put("Low <2km", averageDelay(data, w -> w.visibilityKm() < 2));
        visibilityDelays.put("Moderate 2–5km", averageDelay(data, w -> w.visibilityKm() >= 2 && w.visibilityKm() < 5));
        visibilityDelays.put("Good ≥5km", averageDelay(data, w -> w.visibilityKm() >= 5));
        Map<String, Long> risks = new LinkedHashMap<>();
        data.getWeather().forEach(w -> {
            String risk = engine.predictWeather(w.rainfallMm(), w.windSpeedKph(), w.visibilityKm(),
                    w.temperatureC(), w.thunderstorm()).risk();
            risks.merge(risk, 1L, Long::sum);
        });
        Map<String, Double> windDisruptions = new LinkedHashMap<>();
        windDisruptions.put("<30 km/h", disruptionRate(data, engine, 0, 30));
        windDisruptions.put("30–60 km/h", disruptionRate(data, engine, 30, 60));
        windDisruptions.put(">60 km/h", disruptionRate(data, engine, 60, 200));
        return Ui.module("Weather-Based Airport Disruption Prediction",
                "Baseline weather-risk scoring from visibility, wind, rainfall, temperature and thunderstorm conditions.",
                form, Ui.charts(Ui.barChart("Rainfall vs estimated delay (min)", rainDelays),
                        Ui.barChart("Visibility vs estimated delay (min)", visibilityDelays)),
                Ui.charts(Ui.barChart("Wind speed vs disruption probability (%)", windDisruptions),
                        Ui.card("Weather risk distribution", Ui.pieChart("Sample conditions", risks))));
    }

    private static double averageDelay(DataManager data, java.util.function.Predicate<WeatherData> filter) {
        return data.getWeather().stream().filter(filter)
                .mapToDouble(w -> 5 + (40 - w.visibilityKm()) * 1.4 + w.windSpeedKph() * 0.18
                        + w.rainfallMm() * 0.35 + (w.thunderstorm() ? 12 : 0))
                .average().orElse(0);
    }

    private static double disruptionRate(DataManager data, PredictionEngine engine, double min, double max) {
        return data.getWeather().stream().filter(w -> w.windSpeedKph() >= min && w.windSpeedKph() < max)
                .mapToDouble(w -> engine.predictWeather(w.rainfallMm(), w.windSpeedKph(),
                        w.visibilityKm(), w.temperatureC(), w.thunderstorm()).probability() * 100)
                .average().orElse(0);
    }
}
