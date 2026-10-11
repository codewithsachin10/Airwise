package com.airwise.repository;

import com.airwise.entity.Taxi;
import com.airwise.entity.TaxiAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaxiAvailabilityRepository extends JpaRepository<TaxiAvailability, Long> {
    List<TaxiAvailability> findByTaxi(Taxi taxi);
    List<TaxiAvailability> findByTimeSlotStartLessThanEqualAndTimeSlotEndGreaterThanEqualAndAvailableTrue(
            LocalDateTime start, LocalDateTime end
    );
    long countByTimeSlotStartLessThanEqualAndTimeSlotEndGreaterThanEqualAndAvailableTrue(
            LocalDateTime start, LocalDateTime end
    );
}
