package com.airwise.repository;

import com.airwise.entity.Taxi;
import com.airwise.entity.TaxiOperator;
import com.airwise.entity.TaxiStatus;
import com.airwise.entity.TaxiType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaxiRepository extends JpaRepository<Taxi, Long> {
    List<Taxi> findByOperator(TaxiOperator operator);
    List<Taxi> findByStatus(TaxiStatus status);
    List<Taxi> findByTaxiTypeAndStatus(TaxiType taxiType, TaxiStatus status);
    long countByStatus(TaxiStatus status);
    long countByTaxiType(TaxiType taxiType);
    Optional<Taxi> findByVehicleNumber(String vehicleNumber);
    List<Taxi> findAllByOrderByVehicleNumberAsc();
}
