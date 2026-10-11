import java.time.LocalDate;

public record WeatherData(
        LocalDate date, double rainfallMm, double windSpeedKph, double visibilityKm,
        double temperatureC, boolean thunderstorm) {
}
