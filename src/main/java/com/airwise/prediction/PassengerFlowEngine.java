package com.airwise.prediction;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PassengerFlowEngine {

    public record PassengerFlowResult(
            int expectedPassengers,
            int securityQueueMinutes,
            int checkinQueueMinutes,
            int recommendedLanes,
            int activeLanes,
            String congestionLevel
    ) {}

    public PassengerFlowResult evaluate(int flightMovements, int activeSecurityLanes, LocalDateTime time) {
        int hour = time != null ? time.getHour() : 12;
        boolean peak = (hour >= 6 && hour <= 9) || (hour >= 17 && hour <= 21);

        int expectedPassengers = Math.max(300, flightMovements * 165 + (peak ? 550 : 80));
        int activeLanes = Math.max(1, activeSecurityLanes);

        // Standard lane capacity: ~140 passengers / hour
        int securityQueueMinutes = Math.max(3, (int) Math.round(expectedPassengers / (double) (activeLanes * 130) * 25));
        int checkinQueueMinutes = Math.max(2, (int) Math.round(securityQueueMinutes * 0.65));

        // Recommend extra lanes if queue exceeds 15 minutes
        int recommendedLanes = activeLanes;
        if (securityQueueMinutes > 15) {
            int extraNeeded = (int) Math.ceil((securityQueueMinutes - 15) / 5.0);
            recommendedLanes = activeLanes + extraNeeded;
        }

        String congestionLevel = securityQueueMinutes >= 25 ? "CRITICAL"
                : securityQueueMinutes >= 18 ? "HIGH"
                : securityQueueMinutes >= 10 ? "MODERATE" : "LOW";

        return new PassengerFlowResult(
                expectedPassengers,
                securityQueueMinutes,
                checkinQueueMinutes,
                recommendedLanes,
                activeLanes,
                congestionLevel
        );
    }
}
