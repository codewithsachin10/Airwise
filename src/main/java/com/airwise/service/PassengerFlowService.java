package com.airwise.service;

import com.airwise.entity.Airport;
import com.airwise.entity.PassengerFlowRecord;
import com.airwise.repository.PassengerFlowRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PassengerFlowService {

    private final PassengerFlowRecordRepository passengerFlowRecordRepository;

    public PassengerFlowService(PassengerFlowRecordRepository passengerFlowRecordRepository) {
        this.passengerFlowRecordRepository = passengerFlowRecordRepository;
    }

    public List<PassengerFlowRecord> getAllRecords() {
        return passengerFlowRecordRepository.findAllByOrderByRecordedAtDesc();
    }

    public Optional<PassengerFlowRecord> getLatestForAirport(Airport airport) {
        return passengerFlowRecordRepository.findTopByAirportOrderByRecordedAtDesc(airport);
    }
}
