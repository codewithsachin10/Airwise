package com.airwise.service;

import com.airwise.entity.Airport;
import com.airwise.entity.TaxiDemandPrediction;
import com.airwise.entity.TaxiStatus;
import com.airwise.entity.WeatherRecord;
import com.airwise.prediction.TaxiDemandEngine;
import com.airwise.prediction.TaxiDemandEngine.TaxiDemandResult;
import com.airwise.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TaxiDemandPredictionService {

    private final TaxiDemandEngine taxiDemandEngine;
    private final TaxiDemandPredictionRepository taxiDemandPredictionRepository;
    private final TaxiRepository taxiRepository;
    private final TaxiBookingRepository taxiBookingRepository;
    private final WeatherRecordRepository weatherRecordRepository;

    public TaxiDemandPredictionService(TaxiDemandEngine taxiDemandEngine,
                                       TaxiDemandPredictionRepository taxiDemandPredictionRepository,
                                       TaxiRepository taxiRepository,
                                       TaxiBookingRepository taxiBookingRepository,
                                       WeatherRecordRepository weatherRecordRepository) {
        this.taxiDemandEngine = taxiDemandEngine;
        this.taxiDemandPredictionRepository = taxiDemandPredictionRepository;
        this.taxiRepository = taxiRepository;
        this.taxiBookingRepository = taxiBookingRepository;
        this.weatherRecordRepository = weatherRecordRepository;
    }

    public List<TaxiDemandPrediction> getAllPredictions() {
        return taxiDemandPredictionRepository.findAllByOrderByTimeSlotStartAsc();
    }

    @Transactional
    public List<TaxiDemandPrediction> generateHourlyForecasts(Airport airport, String terminal) {
        List<TaxiDemandPrediction> forecasts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);

        int totalAvailableTaxis = (int) taxiRepository.countByStatus(TaxiStatus.AVAILABLE);
        if (totalAvailableTaxis == 0) totalAvailableTaxis = 45; // baseline fleet

        String weatherCond = "Clear";
        Optional<WeatherRecord> weather = weatherRecordRepository.findTopByAirportOrderByRecordedAtDesc(airport);
        if (weather.isPresent()) {
            weatherCond = weather.get().getCondition();
        }

        // Generate forecasts for next 8 hours
        for (int i = 0; i < 8; i++) {
            LocalDateTime slotStart = now.plusHours(i);
            LocalDateTime slotEnd = slotStart.plusHours(1);

            int arrivals = 6 + (int) (Math.sin((slotStart.getHour() - 6) * 0.4) * 4);
            arrivals = Math.max(2, arrivals);
            int arrivingPassengers = arrivals * 175;

            int reservationsInSlot = (int) taxiBookingRepository.countActiveBookingsInWindow(slotStart, slotEnd);

            TaxiDemandResult result = taxiDemandEngine.calculate(
                    arrivals,
                    arrivingPassengers,
                    slotStart,
                    weatherCond,
                    totalAvailableTaxis,
                    reservationsInSlot
            );

            TaxiDemandPrediction pred = new TaxiDemandPrediction();
            pred.setAirport(airport);
            pred.setTerminal(terminal != null ? terminal : "Terminal 2");
            pred.setTimeSlotStart(slotStart);
            pred.setTimeSlotEnd(slotEnd);
            pred.setPredictedDemand(result.predictedDemand());
            pred.setAvailableTaxis(result.availableTaxis());
            pred.setReservedTaxis(reservationsInSlot);
            pred.setUnmetDemand(result.unmetDemand());
            pred.setDemandLevel(result.demandLevel());
            pred.setRecommendedVehicles(result.recommendedVehicles());
            pred.setDemoData(true);

            forecasts.add(pred);
        }

        taxiDemandPredictionRepository.deleteAll();
        return taxiDemandPredictionRepository.saveAll(forecasts);
    }
}
