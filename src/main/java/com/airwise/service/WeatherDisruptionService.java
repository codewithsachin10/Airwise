package com.airwise.service;

import com.airwise.entity.Alert;
import com.airwise.entity.AlertSeverity;
import com.airwise.entity.Airport;
import com.airwise.entity.WeatherRecord;
import com.airwise.prediction.WeatherDisruptionEngine;
import com.airwise.prediction.WeatherDisruptionEngine.WeatherDisruptionResult;
import com.airwise.repository.AlertRepository;
import com.airwise.repository.WeatherRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WeatherDisruptionService {

    private final WeatherRecordRepository weatherRecordRepository;
    private final WeatherDisruptionEngine weatherDisruptionEngine;
    private final AlertRepository alertRepository;

    public WeatherDisruptionService(WeatherRecordRepository weatherRecordRepository,
                                    WeatherDisruptionEngine weatherDisruptionEngine,
                                    AlertRepository alertRepository) {
        this.weatherRecordRepository = weatherRecordRepository;
        this.weatherDisruptionEngine = weatherDisruptionEngine;
        this.alertRepository = alertRepository;
    }

    public List<WeatherRecord> getAllWeatherRecords() {
        return weatherRecordRepository.findAllByOrderByRecordedAtDesc();
    }

    public Optional<WeatherRecord> getLatestForAirport(Airport airport) {
        return weatherRecordRepository.findTopByAirportOrderByRecordedAtDesc(airport);
    }

    @Transactional
    public WeatherRecord recordObservation(Airport airport, String condition, double tempC, double rainMm,
                                          double windKmh, double visKm, boolean thunderstorm) {
        WeatherDisruptionResult eval = weatherDisruptionEngine.evaluate(rainMm, windKmh, visKm, tempC, thunderstorm);

        WeatherRecord record = new WeatherRecord(
                airport, condition, tempC, rainMm, windKmh, visKm, thunderstorm, eval.disruptionLevel()
        );
        WeatherRecord saved = weatherRecordRepository.save(record);

        if ("HIGH".equals(eval.disruptionLevel())) {
            Alert alert = new Alert(
                    "Weather Disruption Warning: " + airport.getName() + " (" + airport.getCode() + ")",
                    eval.operationalImpact() + " [Visibility: " + visKm + "km, Wind: " + windKmh + "km/h, Rain: " + rainMm + "mm]",
                    eval.groundStopRisk() ? AlertSeverity.CRITICAL : AlertSeverity.WARNING,
                    "WEATHER",
                    airport
            );
            alertRepository.save(alert);
        }

        return saved;
    }
}
