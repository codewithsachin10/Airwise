package com.airwise.repository;

import com.airwise.entity.BookingStatus;
import com.airwise.entity.TaxiBooking;
import com.airwise.entity.TaxiOperator;
import com.airwise.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaxiBookingRepository extends JpaRepository<TaxiBooking, Long> {

    Optional<TaxiBooking> findByBookingReference(String bookingReference);

    List<TaxiBooking> findByUserOrderByPickupTimeDesc(User user);

    List<TaxiBooking> findAllByOrderByPickupTimeDesc();

    List<TaxiBooking> findByBookingStatus(BookingStatus status);

    long countByBookingStatus(BookingStatus status);

    @Query("SELECT COUNT(b) FROM TaxiBooking b WHERE " +
           "b.pickupTime >= :start AND b.pickupTime <= :end AND " +
           "b.bookingStatus <> 'CANCELLED'")
    long countActiveBookingsInWindow(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT b FROM TaxiBooking b WHERE b.taxi.operator = :operator ORDER BY b.pickupTime DESC")
    List<TaxiBooking> findByOperator(@Param("operator") TaxiOperator operator);

    @Query("SELECT b FROM TaxiBooking b WHERE b.flight.id = :flightId AND b.bookingStatus <> 'CANCELLED'")
    List<TaxiBooking> findActiveBookingsByFlightId(@Param("flightId") Long flightId);
}
