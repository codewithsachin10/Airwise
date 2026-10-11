package com.airwise.repository;

import com.airwise.entity.Airport;
import com.airwise.entity.WeatherRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WeatherRecordRepository extends JpaRepository<WeatherRecord, Long> {
    Optional<WeatherRecord> findTopByAirportOrderByRecordedAtDesc(Airport airport);
    List<WeatherRecord> findByAirportOrderByRecordedAtDesc(Airport airport);
    List<WeatherRecord> findAllByOrderByRecordedAtDesc();
}
