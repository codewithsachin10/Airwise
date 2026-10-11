package com.airwise.prediction;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TaxiDemandEngine {

    public record TaxiDemandResult(
            int predictedDemand,
            int availableTaxis,
            int reservedTaxis,
            int unmetDemand,
            String demandLevel,
            int recommendedVehicles,
            int estimatedWaitMinutes
    ) {}

    public TaxiDemandResult calculate(int arrivalsCount, int arrivingPassengers, LocalDateTime slotTime,
                                      String weatherCondition, int availableTaxis, int existingReservations) {
        // Time factor
        int hour = slotTime != null ? slotTime.getHour() : 14;
        boolean isPeak = (hour >= 8 && hour <= 10) || (hour >= 18 && hour <= 22);

        // Weather factor: Rain or storms increase taxi demand by 15-25%
        double weatherFactor = 1.0;
        if (weatherCondition != null) {
            String w = weatherCondition.toLowerCase();
            if (w.contains("storm") || w.contains("thunder")) {
                weatherFactor = 1.25;
            } else if (w.contains("rain") || w.contains("fog")) {
                weatherFactor = 1.15;
            }
        }

        // Base demand estimation: approx 28-35% of arriving passengers utilize landside taxis/cabs
        double rawDemand = (arrivalsCount * 14.0 + arrivingPassengers * 0.32 + (isPeak ? 35.0 : 5.0)) * weatherFactor;
        int predictedDemand = (int) Math.round(Math.max(10, rawDemand));

        // Unmet demand = predicted demand - available unreserved supply
        int effectiveAvailable = Math.max(0, availableTaxis);
        int remainingCapacity = Math.max(0, effectiveAvailable - existingReservations);
        int unmetDemand = Math.max(0, predictedDemand - remainingCapacity);

        String demandLevel;
        if (unmetDemand > 40 || predictedDemand > 120) {
            demandLevel = "HIGH";
        } else if (unmetDemand > 10 || predictedDemand > 60) {
            demandLevel = "MEDIUM";
        } else {
            demandLevel = "LOW";
        }

        // Recommended vehicles to mobilize to meet the peak
        int recommendedVehicles = unmetDemand > 0 ? (int) Math.ceil(unmetDemand * 1.1) : 0;
        int waitMinutes = remainingCapacity <= 5 ? 15 + Math.min(unmetDemand / 3, 30) : 5;

        return new TaxiDemandResult(
                predictedDemand,
                availableTaxis,
                existingReservations,
                unmetDemand,
                demandLevel,
                recommendedVehicles,
                waitMinutes
        );
    }
}
