package com.airwise.prediction;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class FlightDelayPredictionEngine {

    public record FlightPredictionResult(
            double probability,
            int expectedDelayMinutes,
            String riskLevel,
            List<String> contributingFactors,
            double weatherRiskScore,
            double congestionRiskScore
    ) {}

    public FlightPredictionResult predict(int previousDelayMinutes, int turnaroundMinutes, int passengerCount,
                                          double weatherRiskScore, LocalDateTime departureTime, double congestionScore) {
        List<String> factors = new ArrayList<>();
        double score = 0.08;

        if (previousDelayMinutes > 0) {
            double prevImpact = Math.min(previousDelayMinutes / 180.0, 0.28);
            score += prevImpact;
            factors.add("Previous leg inbound delay (+" + previousDelayMinutes + " min)");
        }

        if (turnaroundMinutes > 50) {
            double turnImpact = Math.min((turnaroundMinutes - 50) / 180.0, 0.20);
            score += turnImpact;
            factors.add("Extended aircraft turnaround (" + turnaroundMinutes + " min vs standard 45 min)");
        }

        if (passengerCount > 200) {
            double loadImpact = Math.min((passengerCount - 200) / 1600.0, 0.12);
            score += loadImpact;
            factors.add("High passenger load (" + passengerCount + " passengers)");
        }

        double boundedWeather = Math.clamp(weatherRiskScore, 0.0, 1.0);
        score += boundedWeather * 0.30;
        if (boundedWeather >= 0.35) {
            factors.add("Elevated weather risk index (" + String.format("%.0f%%", boundedWeather * 100) + ")");
        }

        // Peak hour detection: 07:00-09:30 or 17:00-20:30
        int hour = departureTime != null ? departureTime.getHour() : 12;
        boolean isPeakHour = (hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 20);
        if (isPeakHour) {
            score += 0.12;
            factors.add("Peak-hour airport slot departure congestion");
        }

        if (congestionScore > 0.4) {
            score += Math.min(congestionScore * 0.15, 0.15);
            factors.add("Terminal passenger flow above nominal threshold");
        }

        double probability = Math.clamp(score, 0.03, 0.97);
        int expectedMinutes = (int) Math.round(8 + probability * 52);

        String riskLevel = probability >= 0.65 ? "HIGH" : probability >= 0.35 ? "MEDIUM" : "LOW";

        if (factors.isEmpty()) {
            factors.add("Nominal operating parameters with low scheduled disruption probability");
        }

        return new FlightPredictionResult(
                probability,
                expectedMinutes,
                riskLevel,
                Collections.unmodifiableList(factors),
                boundedWeather,
                congestionScore
        );
    }
}
