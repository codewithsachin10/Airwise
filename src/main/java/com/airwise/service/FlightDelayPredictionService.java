package com.airwise.service;

import com.airwise.entity.Flight;
import com.airwise.entity.FlightPrediction;
import com.airwise.entity.PassengerFlowRecord;
import com.airwise.entity.WeatherRecord;
import com.airwise.prediction.FlightDelayPredictionEngine;
import com.airwise.prediction.FlightDelayPredictionEngine.FlightPredictionResult;
import com.airwise.repository.FlightPredictionRepository;
import com.airwise.repository.PassengerFlowRecordRepository;
import com.airwise.repository.WeatherRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class FlightDelayPredictionService {

    private final FlightDelayPredictionEngine predictionEngine;
    private final FlightPredictionRepository flightPredictionRepository;
    private final WeatherRecordRepository weatherRecordRepository;
    private final PassengerFlowRecordRepository passengerFlowRecordRepository;

    public FlightDelayPredictionService(FlightDelayPredictionEngine predictionEngine,
                                        FlightPredictionRepository flightPredictionRepository,
                                        WeatherRecordRepository weatherRecordRepository,
                                        PassengerFlowRecordRepository passengerFlowRecordRepository) {
        this.predictionEngine = predictionEngine;
        this.flightPredictionRepository = flightPredictionRepository;
        this.weatherRecordRepository = weatherRecordRepository;
        this.passengerFlowRecordRepository = passengerFlowRecordRepository;
    }

    @Transactional
    public FlightPrediction predictForFlight(Flight flight) {
        // Find existing recent prediction if already computed within last 2 hours
        Optional<FlightPrediction> existing = flightPredictionRepository.findTopByFlightOrderByPredictedAtDesc(flight);
        if (existing.isPresent() && existing.get().getPredictedAt().isAfter(LocalDateTime.now().minusHours(2))) {
            return existing.get();
        }

        // Fetch destination weather score
        double weatherRisk = 0.15;
        Optional<WeatherRecord> arrWeather = weatherRecordRepository.findTopByAirportOrderByRecordedAtDesc(flight.getArrivalAirport());
        if (arrWeather.isPresent()) {
            WeatherRecord w = arrWeather.get();
            if ("HIGH".equalsIgnoreCase(w.getDisruptionLevel()) || "CRITICAL".equalsIgnoreCase(w.getDisruptionLevel())) {
                weatherRisk = 0.75;
            } else if ("MEDIUM".equalsIgnoreCase(w.getDisruptionLevel())) {
                weatherRisk = 0.45;
            } else {
                weatherRisk = 0.12;
            }
        }

        // Fetch congestion score
        double congestionScore = 0.20;
        Optional<PassengerFlowRecord> flow = passengerFlowRecordRepository.findTopByAirportOrderByRecordedAtDesc(flight.getDepartureAirport());
        if (flow.isPresent()) {
            String level = flow.get().getCongestionLevel();
            congestionScore = "CRITICAL".equalsIgnoreCase(level) ? 0.85
                    : "HIGH".equalsIgnoreCase(level) ? 0.60
                    : "MODERATE".equalsIgnoreCase(level) ? 0.35 : 0.10;
        }

        FlightPredictionResult result = predictionEngine.predict(
                flight.getPreviousDelayMinutes(),
                flight.getTurnaroundMinutes(),
                flight.getPassengerCount(),
                weatherRisk,
                flight.getScheduledDeparture(),
                congestionScore
        );

        FlightPrediction prediction = new FlightPrediction();
        prediction.setFlight(flight);
        prediction.setPredictedAt(LocalDateTime.now());
        prediction.setDelayProbability(result.probability());
        prediction.setExpectedDelayMinutes(result.expectedDelayMinutes());
        prediction.setRiskLevel(result.riskLevel());
        prediction.setContributingFactors(String.join("; ", result.contributingFactors()));
        prediction.setWeatherRiskScore(result.weatherRiskScore());
        prediction.setCongestionRiskScore(result.congestionRiskScore());
        prediction.setDemoData(true);

        return flightPredictionRepository.save(prediction);
    }

    public Optional<FlightPrediction> getLatestPrediction(Flight flight) {
        return flightPredictionRepository.findTopByFlightOrderByPredictedAtDesc(flight);
    }
}
