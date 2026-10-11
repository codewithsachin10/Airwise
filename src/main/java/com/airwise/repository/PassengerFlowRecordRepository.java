package com.airwise.repository;

import com.airwise.entity.Airport;
import com.airwise.entity.PassengerFlowRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PassengerFlowRecordRepository extends JpaRepository<PassengerFlowRecord, Long> {
    Optional<PassengerFlowRecord> findTopByAirportOrderByRecordedAtDesc(Airport airport);
    List<PassengerFlowRecord> findByAirportOrderByRecordedAtDesc(Airport airport);
    List<PassengerFlowRecord> findAllByOrderByRecordedAtDesc();
}
