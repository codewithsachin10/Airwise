package com.airwise.prediction;

import org.springframework.stereotype.Component;

@Component
public class WeatherDisruptionEngine {

    public record WeatherDisruptionResult(
            double disruptionScore,
            String disruptionLevel,
            String operationalImpact,
            boolean groundStopRisk
    ) {}

    public WeatherDisruptionResult evaluate(double rainfallMm, double windSpeedKmh, double visibilityKm,
                                            double temperatureC, boolean thunderstorm) {
        double rainImpact = Math.min(Math.max(rainfallMm, 0.0) / 40.0, 0.30);
        double windImpact = Math.min(Math.max(windSpeedKmh, 0.0) / 100.0, 0.28);
        double visImpact = Math.max(0.0, (10.0 - visibilityKm) / 10.0) * 0.30;
        double tempImpact = (temperatureC < 2.0 || temperatureC > 42.0) ? 0.08 : 0.0;
        double stormImpact = thunderstorm ? 0.35 : 0.0;

        double score = Math.clamp(rainImpact + windImpact + visImpact + tempImpact + stormImpact, 0.02, 0.99);

        String level;
        String impact;
        boolean groundStop = false;

        if (score >= 0.70) {
            level = "HIGH";
            impact = "Severe disruption: high probability of holding patterns, runway reassignments, and flight cancellations";
            groundStop = thunderstorm || visibilityKm < 1.0 || windSpeedKmh > 75.0;
        } else if (score >= 0.40) {
            level = "MEDIUM";
            impact = "Moderate disruption: extended taxi times, schedule padding, and minor arrival/departure delays";
        } else {
            level = "LOW";
            impact = "Optimal conditions: standard airport throughput and normal visual approach flight rules";
        }

        return new WeatherDisruptionResult(score, level, impact, groundStop);
    }
}
