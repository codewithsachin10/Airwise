package com.airwise.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "flights", indexes = {
    @Index(name = "idx_flight_number", columnList = "flightNumber"),
    @Index(name = "idx_flight_dep_date", columnList = "scheduledDeparture")
})
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String flightNumber;

    @Column(nullable = false, length = 80)
    private String airline;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "departure_airport_id", nullable = false)
    private Airport departureAirport;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "arrival_airport_id", nullable = false)
    private Airport arrivalAirport;

    @Column(nullable = false)
    private LocalDateTime scheduledDeparture;

    @Column(nullable = false)
    private LocalDateTime scheduledArrival;

    private LocalDateTime estimatedDeparture;
    private LocalDateTime estimatedArrival;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FlightStatus status = FlightStatus.ON_TIME;

    @Column(nullable = false)
    private int delayMinutes = 0;

    @Column(length = 40)
    private String aircraftType = "Airbus A320neo";

    @Column(nullable = false)
    private int passengerCount = 180;

    @Column(length = 20)
    private String terminal = "Terminal 2";

    @Column(length = 20)
    private String gate = "Gate 14";

    private int previousDelayMinutes = 0;
    private int turnaroundMinutes = 45;

    @Column(length = 255)
    private String confirmedDelayReason; // Operational confirmed reason if known

    public Flight() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }

    public String getAirline() { return airline; }
    public void setAirline(String airline) { this.airline = airline; }

    public Airport getDepartureAirport() { return departureAirport; }
    public void setDepartureAirport(Airport departureAirport) { this.departureAirport = departureAirport; }

    public Airport getArrivalAirport() { return arrivalAirport; }
    public void setArrivalAirport(Airport arrivalAirport) { this.arrivalAirport = arrivalAirport; }

    public LocalDateTime getScheduledDeparture() { return scheduledDeparture; }
    public void setScheduledDeparture(LocalDateTime scheduledDeparture) { this.scheduledDeparture = scheduledDeparture; }

    public LocalDateTime getScheduledArrival() { return scheduledArrival; }
    public void setScheduledArrival(LocalDateTime scheduledArrival) { this.scheduledArrival = scheduledArrival; }

    public LocalDateTime getEstimatedDeparture() { return estimatedDeparture != null ? estimatedDeparture : scheduledDeparture; }
    public void setEstimatedDeparture(LocalDateTime estimatedDeparture) { this.estimatedDeparture = estimatedDeparture; }

    public LocalDateTime getEstimatedArrival() { return estimatedArrival != null ? estimatedArrival : scheduledArrival; }
    public void setEstimatedArrival(LocalDateTime estimatedArrival) { this.estimatedArrival = estimatedArrival; }

    public FlightStatus getStatus() { return status; }
    public void setStatus(FlightStatus status) { this.status = status; }

    public int getDelayMinutes() { return delayMinutes; }
    public void setDelayMinutes(int delayMinutes) { this.delayMinutes = delayMinutes; }

    public String getAircraftType() { return aircraftType; }
    public void setAircraftType(String aircraftType) { this.aircraftType = aircraftType; }

    public int getPassengerCount() { return passengerCount; }
    public void setPassengerCount(int passengerCount) { this.passengerCount = passengerCount; }

    public String getTerminal() { return terminal; }
    public void setTerminal(String terminal) { this.terminal = terminal; }

    public String getGate() { return gate; }
    public void setGate(String gate) { this.gate = gate; }

    public int getPreviousDelayMinutes() { return previousDelayMinutes; }
    public void setPreviousDelayMinutes(int previousDelayMinutes) { this.previousDelayMinutes = previousDelayMinutes; }

    public int getTurnaroundMinutes() { return turnaroundMinutes; }
    public void setTurnaroundMinutes(int turnaroundMinutes) { this.turnaroundMinutes = turnaroundMinutes; }

    public String getConfirmedDelayReason() { return confirmedDelayReason; }
    public void setConfirmedDelayReason(String confirmedDelayReason) { this.confirmedDelayReason = confirmedDelayReason; }
}
