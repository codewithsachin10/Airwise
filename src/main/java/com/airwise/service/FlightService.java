package com.airwise.service;

import com.airwise.entity.Flight;
import com.airwise.entity.FlightStatus;
import com.airwise.repository.FlightRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class FlightService {

    private final FlightRepository flightRepository;
    private final TaxiBookingService taxiBookingService;
    private final NotificationService notificationService;

    public FlightService(FlightRepository flightRepository,
                         @Lazy TaxiBookingService taxiBookingService,
                         NotificationService notificationService) {
        this.flightRepository = flightRepository;
        this.taxiBookingService = taxiBookingService;
        this.notificationService = notificationService;
    }

    public List<Flight> getAllFlights() {
        return flightRepository.findAllByOrderByScheduledDepartureDesc();
    }

    public List<Flight> getRecentFlights() {
        return flightRepository.findTop20ByOrderByScheduledDepartureDesc();
    }

    public Optional<Flight> getFlightById(Long id) {
        return flightRepository.findById(id);
    }

    public Optional<Flight> getFlightByNumber(String flightNumber) {
        return flightRepository.findByFlightNumberIgnoreCase(flightNumber);
    }

    public List<Flight> searchFlights(String flightNumber, String airline, Long depAirportId, Long arrAirportId, LocalDate travelDate) {
        LocalDateTime start = null;
        LocalDateTime end = null;
        if (travelDate != null) {
            start = travelDate.atStartOfDay();
            end = travelDate.atTime(LocalTime.MAX);
        }
        String numQuery = (flightNumber != null && !flightNumber.isBlank()) ? flightNumber.trim() : null;
        String airQuery = (airline != null && !airline.isBlank()) ? airline.trim() : null;

        return flightRepository.searchFlights(numQuery, airQuery, depAirportId, arrAirportId, start, end);
    }

    @Transactional
    public Flight saveFlight(Flight flight) {
        return flightRepository.save(flight);
    }

    @Transactional
    public Flight updateFlightStatus(Long id, FlightStatus status, int delayMinutes, String confirmedReason) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Flight not found with ID: " + id));

        FlightStatus oldStatus = flight.getStatus();
        flight.setStatus(status);
        flight.setDelayMinutes(delayMinutes);
        if (confirmedReason != null && !confirmedReason.isBlank()) {
            flight.setConfirmedDelayReason(confirmedReason);
        }

        if (status == FlightStatus.DELAYED && delayMinutes > 0) {
            flight.setEstimatedArrival(flight.getScheduledArrival().plusMinutes(delayMinutes));
            flight.setEstimatedDeparture(flight.getScheduledDeparture().plusMinutes(delayMinutes));

            // Notify connected taxi bookings & trigger rescheduling workflow
            taxiBookingService.handleFlightDelay(flight);
        } else if (status == FlightStatus.ON_TIME) {
            flight.setEstimatedArrival(flight.getScheduledArrival());
            flight.setEstimatedDeparture(flight.getScheduledDeparture());
            flight.setDelayMinutes(0);
        }

        return flightRepository.save(flight);
    }

    public long countTotalFlights() {
        return flightRepository.count();
    }

    public long countDelayedFlights() {
        return flightRepository.countByStatus(FlightStatus.DELAYED);
    }

    public long countCancelledFlights() {
        return flightRepository.countByStatus(FlightStatus.CANCELLED);
    }

    public long countOnTimeFlights() {
        return flightRepository.countByStatus(FlightStatus.ON_TIME);
    }
}
