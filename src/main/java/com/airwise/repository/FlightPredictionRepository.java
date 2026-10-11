package com.airwise.repository;

import com.airwise.entity.Flight;
import com.airwise.entity.FlightPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FlightPredictionRepository extends JpaRepository<FlightPrediction, Long> {
    Optional<FlightPrediction> findTopByFlightOrderByPredictedAtDesc(Flight flight);
    Optional<FlightPrediction> findTopByFlightIdOrderByPredictedAtDesc(Long flightId);
}
