package com.airwise.repository;

import com.airwise.entity.Flight;
import com.airwise.entity.FlightStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {

    Optional<Flight> findByFlightNumberIgnoreCase(String flightNumber);

    List<Flight> findAllByOrderByScheduledDepartureDesc();

    List<Flight> findTop20ByOrderByScheduledDepartureDesc();

    long countByStatus(FlightStatus status);

    @Query("SELECT f FROM Flight f WHERE " +
           "(:flightNumber IS NULL OR LOWER(f.flightNumber) LIKE LOWER(CONCAT('%', :flightNumber, '%'))) AND " +
           "(:airline IS NULL OR LOWER(f.airline) LIKE LOWER(CONCAT('%', :airline, '%'))) AND " +
           "(:depAirportId IS NULL OR f.departureAirport.id = :depAirportId) AND " +
           "(:arrAirportId IS NULL OR f.arrivalAirport.id = :arrAirportId) AND " +
           "(:startDate IS NULL OR f.scheduledDeparture >= :startDate) AND " +
           "(:endDate IS NULL OR f.scheduledDeparture <= :endDate) " +
           "ORDER BY f.scheduledDeparture ASC")
    List<Flight> searchFlights(
            @Param("flightNumber") String flightNumber,
            @Param("airline") String airline,
            @Param("depAirportId") Long depAirportId,
            @Param("arrAirportId") Long arrAirportId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
