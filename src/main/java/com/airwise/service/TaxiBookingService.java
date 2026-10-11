package com.airwise.service;

import com.airwise.entity.*;
import com.airwise.repository.FlightRepository;
import com.airwise.repository.TaxiBookingRepository;
import com.airwise.repository.TaxiRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class TaxiBookingService {

    private final TaxiBookingRepository taxiBookingRepository;
    private final TaxiRepository taxiRepository;
    private final FlightRepository flightRepository;
    private final NotificationService notificationService;
    private final Random random = new Random();

    public TaxiBookingService(TaxiBookingRepository taxiBookingRepository,
                              TaxiRepository taxiRepository,
                              FlightRepository flightRepository,
                              NotificationService notificationService) {
        this.taxiBookingRepository = taxiBookingRepository;
        this.taxiRepository = taxiRepository;
        this.flightRepository = flightRepository;
        this.notificationService = notificationService;
    }

    public List<TaxiBooking> getUserBookings(User user) {
        return taxiBookingRepository.findByUserOrderByPickupTimeDesc(user);
    }

    public List<TaxiBooking> getAllBookings() {
        return taxiBookingRepository.findAllByOrderByPickupTimeDesc();
    }

    public List<TaxiBooking> getOperatorBookings(TaxiOperator operator) {
        return taxiBookingRepository.findByOperator(operator);
    }

    public Optional<TaxiBooking> getBookingByReference(String reference) {
        return taxiBookingRepository.findByBookingReference(reference);
    }

    public Optional<TaxiBooking> getBookingById(Long id) {
        return taxiBookingRepository.findById(id);
    }

    /**
     * Advance Booking Workflow with Transactional Overbooking Prevention
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public TaxiBooking createBooking(User user, Long flightId, TaxiType taxiType, String terminal,
                                     String destination, int passengers, int luggage,
                                     LocalDateTime pickupTime, String notes) {

        // Validate time
        if (pickupTime.isBefore(LocalDateTime.now().minusMinutes(10))) {
            throw new IllegalArgumentException("Pickup time cannot be in the past.");
        }

        // Capacity validation: Check existing active bookings in the 1-hour window
        LocalDateTime windowStart = pickupTime.minusMinutes(30);
        LocalDateTime windowEnd = pickupTime.plusMinutes(30);
        long activeBookings = taxiBookingRepository.countActiveBookingsInWindow(windowStart, windowEnd);

        // Available fleet capacity
        long totalFleet = taxiRepository.countByStatus(TaxiStatus.AVAILABLE);
        long maxCapacityPerWindow = Math.max(5, totalFleet);

        if (activeBookings >= maxCapacityPerWindow) {
            throw new IllegalStateException("Capacity limit reached for this pickup window (" +
                    pickupTime.toLocalTime() + "). Please choose an alternative slot or taxi type.");
        }

        // Connect flight if selected
        Flight flight = null;
        if (flightId != null) {
            flight = flightRepository.findById(flightId).orElse(null);
        }

        // Calculate estimated fare
        double baseRate = switch (taxiType) {
            case SEDAN -> 24.0;
            case SUV -> 34.0;
            case PREMIUM -> 52.0;
            case ELECTRIC -> 28.0;
            case VAN -> 45.0;
        };
        // Simulated airport transfer distance ~ 25km + base airport toll
        double estimatedDistanceKm = 22.0 + (random.nextInt(15));
        double fare = Math.round((estimatedDistanceKm * baseRate + 150.0) * 100.0) / 100.0;

        // Generate booking reference e.g. AW-TX-74921
        String reference = "AW-TX-" + (10000 + random.nextInt(90000));

        TaxiBooking booking = new TaxiBooking();
        booking.setBookingReference(reference);
        booking.setUser(user);
        booking.setFlight(flight);
        booking.setRequestedTaxiType(taxiType);
        booking.setPickupTerminal(terminal != null ? terminal : "Terminal 2");
        booking.setDestination(destination);
        booking.setPassengerCount(passengers);
        booking.setLuggageCount(luggage);
        booking.setPickupTime(pickupTime);
        booking.setEstimatedFare(fare);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setNotes(notes);

        // Auto-assign available vehicle if one matches
        List<Taxi> eligibleTaxis = taxiRepository.findByTaxiTypeAndStatus(taxiType, TaxiStatus.AVAILABLE);
        if (!eligibleTaxis.isEmpty()) {
            booking.setTaxi(eligibleTaxis.get(0));
            booking.setBookingStatus(BookingStatus.DRIVER_ASSIGNED);
        }

        TaxiBooking saved = taxiBookingRepository.save(booking);

        // Create in-app notification
        notificationService.createNotification(
                user,
                "Taxi Booking Confirmed: " + reference,
                "Your ride to " + destination + " is booked for " + pickupTime + ". Reference: " + reference,
                "TAXI",
                "/taxis/bookings/" + reference
        );

        return saved;
    }

    @Transactional
    public void handleFlightDelay(Flight flight) {
        List<TaxiBooking> affectedBookings = taxiBookingRepository.findActiveBookingsByFlightId(flight.getId());
        for (TaxiBooking booking : affectedBookings) {
            booking.setFlightDelayedFlag(true);
            booking.setBookingStatus(BookingStatus.RESCHEDULE_REQUIRED);
            booking.setFlightDelayNotice("Flight " + flight.getFlightNumber() + " is delayed by " +
                    flight.getDelayMinutes() + " min. New estimated landing: " + flight.getEstimatedArrival());

            taxiBookingRepository.save(booking);

            notificationService.createNotification(
                    booking.getUser(),
                    "Flight Delayed — Taxi Reschedule Needed",
                    "Flight " + flight.getFlightNumber() + " is delayed by " + flight.getDelayMinutes() +
                            " min. Please review and confirm your new taxi pickup time.",
                    "FLIGHT",
                    "/taxis/bookings/" + booking.getBookingReference()
            );
        }
    }

    @Transactional
    public TaxiBooking rescheduleBooking(Long bookingId, LocalDateTime newPickupTime) {
        TaxiBooking booking = taxiBookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        booking.setPickupTime(newPickupTime);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setFlightDelayedFlag(false);
        booking.setFlightDelayNotice("Rescheduled successfully to " + newPickupTime);

        TaxiBooking saved = taxiBookingRepository.save(booking);

        notificationService.createNotification(
                booking.getUser(),
                "Taxi Pickup Rescheduled: " + booking.getBookingReference(),
                "Your updated pickup time is confirmed for " + newPickupTime,
                "TAXI",
                "/taxis/bookings/" + booking.getBookingReference()
        );

        return saved;
    }

    @Transactional
    public TaxiBooking updateStatus(Long bookingId, BookingStatus newStatus) {
        TaxiBooking booking = taxiBookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        booking.setBookingStatus(newStatus);
        TaxiBooking saved = taxiBookingRepository.save(booking);

        notificationService.createNotification(
                booking.getUser(),
                "Taxi Booking Update: " + booking.getBookingReference(),
                "Your booking status is now: " + newStatus,
                "TAXI",
                "/taxis/bookings/" + booking.getBookingReference()
        );

        return saved;
    }

    @Transactional
    public TaxiBooking cancelBooking(Long bookingId, User user) {
        TaxiBooking booking = taxiBookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        // Check ownership if not admin/operator
        if (user != null && !booking.getUser().getId().equals(user.getId()) && user.getRole() == Role.ROLE_PASSENGER) {
            throw new SecurityException("Unauthorized access to booking cancellation");
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        TaxiBooking saved = taxiBookingRepository.save(booking);

        notificationService.createNotification(
                booking.getUser(),
                "Taxi Booking Cancelled",
                "Your taxi reservation " + booking.getBookingReference() + " has been cancelled.",
                "TAXI",
                "/taxis/bookings"
        );

        return saved;
    }

    public long countTotalBookings() {
        return taxiBookingRepository.count();
    }

    public long countActiveBookings() {
        return taxiBookingRepository.countByBookingStatus(BookingStatus.CONFIRMED) +
               taxiBookingRepository.countByBookingStatus(BookingStatus.DRIVER_ASSIGNED);
    }
}
