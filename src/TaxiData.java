import java.time.LocalDateTime;

public record TaxiData(
        LocalDateTime timestamp, int arrivals, int passengerCount, int availableTaxis,
        boolean holiday, String weatherCondition) {
}
