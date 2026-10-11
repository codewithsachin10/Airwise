package com.airwise.service;

import com.airwise.entity.*;
import com.airwise.repository.TaxiAvailabilityRepository;
import com.airwise.repository.TaxiOperatorRepository;
import com.airwise.repository.TaxiRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TaxiOperatorService {

    private final TaxiOperatorRepository taxiOperatorRepository;
    private final TaxiRepository taxiRepository;
    private final TaxiAvailabilityRepository taxiAvailabilityRepository;

    public TaxiOperatorService(TaxiOperatorRepository taxiOperatorRepository,
                               TaxiRepository taxiRepository,
                               TaxiAvailabilityRepository taxiAvailabilityRepository) {
        this.taxiOperatorRepository = taxiOperatorRepository;
        this.taxiRepository = taxiRepository;
        this.taxiAvailabilityRepository = taxiAvailabilityRepository;
    }

    public Optional<TaxiOperator> getOperatorForUser(User user) {
        return taxiOperatorRepository.findByUser(user);
    }

    public List<Taxi> getTaxisForOperator(TaxiOperator operator) {
        return taxiRepository.findByOperator(operator);
    }

    public List<Taxi> getAllTaxis() {
        return taxiRepository.findAllByOrderByVehicleNumberAsc();
    }

    public Optional<Taxi> getTaxiById(Long id) {
        return taxiRepository.findById(id);
    }

    @Transactional
    public Taxi registerTaxi(TaxiOperator operator, String vehicleNumber, String model,
                             TaxiType type, int passengerCap, int luggageCap, double farePerKm, String terminal) {
        Taxi taxi = new Taxi();
        taxi.setOperator(operator);
        taxi.setVehicleNumber(vehicleNumber);
        taxi.setModel(model);
        taxi.setTaxiType(type);
        taxi.setPassengerCapacity(passengerCap);
        taxi.setLuggageCapacity(luggageCap);
        taxi.setBaseFarePerKm(farePerKm);
        taxi.setStatus(TaxiStatus.AVAILABLE);
        taxi.setCurrentTerminal(terminal != null ? terminal : "Terminal 2");
        return taxiRepository.save(taxi);
    }

    @Transactional
    public Taxi updateTaxiStatus(Long taxiId, TaxiStatus status, String terminal) {
        Taxi taxi = taxiRepository.findById(taxiId)
                .orElseThrow(() -> new IllegalArgumentException("Taxi not found"));
        taxi.setStatus(status);
        if (terminal != null && !terminal.isBlank()) {
            taxi.setCurrentTerminal(terminal);
        }
        return taxiRepository.save(taxi);
    }

    @Transactional
    public TaxiAvailability setAvailabilitySlot(Taxi taxi, LocalDateTime start, LocalDateTime end, String terminal, boolean available) {
        TaxiAvailability avail = new TaxiAvailability(taxi, start, end, terminal, available);
        return taxiAvailabilityRepository.save(avail);
    }

    public long countAvailableTaxis() {
        return taxiRepository.countByStatus(TaxiStatus.AVAILABLE);
    }

    public long countBusyTaxis() {
        return taxiRepository.countByStatus(TaxiStatus.BUSY);
    }
}
