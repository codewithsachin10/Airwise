package com.airwise.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "weather_records", indexes = {
    @Index(name = "idx_weather_airport", columnList = "airport_id")
})
public class WeatherRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "airport_id", nullable = false)
    private Airport airport;

    @Column(nullable = false)
    private LocalDateTime recordedAt = LocalDateTime.now();

    @Column(nullable = false, length = 60)
    private String condition = "Clear"; // Clear, Rainy, Storm, Thunderstorm, Foggy, Strong Winds

    private double temperatureC = 28.0;
    private double rainfallMm = 0.0;
    private double windSpeedKmh = 14.0;
    private double visibilityKm = 10.0;
    private boolean thunderstorm = false;

    @Column(nullable = false, length = 20)
    private String disruptionLevel = "LOW"; // LOW, MEDIUM, HIGH, CRITICAL

    public WeatherRecord() {}

    public WeatherRecord(Airport airport, String condition, double temperatureC, double rainfallMm,
                         double windSpeedKmh, double visibilityKm, boolean thunderstorm, String disruptionLevel) {
        this.airport = airport;
        this.recordedAt = LocalDateTime.now();
        this.condition = condition;
        this.temperatureC = temperatureC;
        this.rainfallMm = rainfallMm;
        this.windSpeedKmh = windSpeedKmh;
        this.visibilityKm = visibilityKm;
        this.thunderstorm = thunderstorm;
        this.disruptionLevel = disruptionLevel;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Airport getAirport() { return airport; }
    public void setAirport(Airport airport) { this.airport = airport; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public double getTemperatureC() { return temperatureC; }
    public void setTemperatureC(double temperatureC) { this.temperatureC = temperatureC; }

    public double getRainfallMm() { return rainfallMm; }
    public void setRainfallMm(double rainfallMm) { this.rainfallMm = rainfallMm; }

    public double getWindSpeedKmh() { return windSpeedKmh; }
    public void setWindSpeedKmh(double windSpeedKmh) { this.windSpeedKmh = windSpeedKmh; }

    public double getVisibilityKm() { return visibilityKm; }
    public void setVisibilityKm(double visibilityKm) { this.visibilityKm = visibilityKm; }

    public boolean isThunderstorm() { return thunderstorm; }
    public void setThunderstorm(boolean thunderstorm) { this.thunderstorm = thunderstorm; }

    public String getDisruptionLevel() { return disruptionLevel; }
    public void setDisruptionLevel(String disruptionLevel) { this.disruptionLevel = disruptionLevel; }
}
