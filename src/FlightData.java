import java.time.LocalDateTime;

public record FlightData(
        String flightNumber, String airline, String departureAirport, String arrivalAirport,
        LocalDateTime scheduledDeparture, int previousDelayMinutes, int turnaroundMinutes,
        int passengerCount, double weatherRisk, String status) {
}
