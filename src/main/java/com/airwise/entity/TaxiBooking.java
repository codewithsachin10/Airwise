package com.airwise.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "taxi_bookings", indexes = {
    @Index(name = "idx_booking_ref", columnList = "bookingReference", unique = true),
    @Index(name = "idx_booking_user", columnList = "user_id"),
    @Index(name = "idx_booking_slot", columnList = "pickupTime")
})
public class TaxiBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String bookingReference;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "flight_id")
    private Flight flight;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "taxi_id")
    private Taxi taxi;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaxiType requestedTaxiType = TaxiType.SEDAN;

    @Column(nullable = false, length = 50)
    private String pickupTerminal = "Terminal 2";

    @Column(nullable = false, length = 200)
    private String destination;

    @Column(nullable = false)
    private int passengerCount = 1;

    @Column(nullable = false)
    private int luggageCount = 1;

    @Column(nullable = false)
    private LocalDateTime pickupTime;

    @Column(nullable = false)
    private double estimatedFare;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingStatus bookingStatus = BookingStatus.CONFIRMED;

    private boolean flightDelayedFlag = false;

    @Column(length = 255)
    private String flightDelayNotice;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public TaxiBooking() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Flight getFlight() { return flight; }
    public void setFlight(Flight flight) { this.flight = flight; }

    public Taxi getTaxi() { return taxi; }
    public void setTaxi(Taxi taxi) { this.taxi = taxi; }

    public TaxiType getRequestedTaxiType() { return requestedTaxiType; }
    public void setRequestedTaxiType(TaxiType requestedTaxiType) { this.requestedTaxiType = requestedTaxiType; }

    public String getPickupTerminal() { return pickupTerminal; }
    public void setPickupTerminal(String pickupTerminal) { this.pickupTerminal = pickupTerminal; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public int getPassengerCount() { return passengerCount; }
    public void setPassengerCount(int passengerCount) { this.passengerCount = passengerCount; }

    public int getLuggageCount() { return luggageCount; }
    public void setLuggageCount(int luggageCount) { this.luggageCount = luggageCount; }

    public LocalDateTime getPickupTime() { return pickupTime; }
    public void setPickupTime(LocalDateTime pickupTime) { this.pickupTime = pickupTime; }

    public double getEstimatedFare() { return estimatedFare; }
    public void setEstimatedFare(double estimatedFare) { this.estimatedFare = estimatedFare; }

    public BookingStatus getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(BookingStatus bookingStatus) { this.bookingStatus = bookingStatus; }

    public boolean isFlightDelayedFlag() { return flightDelayedFlag; }
    public void setFlightDelayedFlag(boolean flightDelayedFlag) { this.flightDelayedFlag = flightDelayedFlag; }

    public String getFlightDelayNotice() { return flightDelayNotice; }
    public void setFlightDelayNotice(String flightDelayNotice) { this.flightDelayNotice = flightDelayNotice; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
