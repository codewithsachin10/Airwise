import java.time.LocalDateTime;

public record PassengerData(
        LocalDateTime timestamp, int arrivals, int departures, int passengerCount,
        int activeCounters, int securityLanes, boolean holiday) {
}
