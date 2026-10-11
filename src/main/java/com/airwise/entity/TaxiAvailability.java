package com.airwise.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "taxi_availability", indexes = {
    @Index(name = "idx_avail_slot", columnList = "timeSlotStart, timeSlotEnd")
})
public class TaxiAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "taxi_id", nullable = false)
    private Taxi taxi;

    @Column(nullable = false)
    private LocalDateTime timeSlotStart;

    @Column(nullable = false)
    private LocalDateTime timeSlotEnd;

    @Column(nullable = false, length = 30)
    private String terminal = "Terminal 2";

    @Column(nullable = false)
    private boolean available = true;

    public TaxiAvailability() {}

    public TaxiAvailability(Taxi taxi, LocalDateTime timeSlotStart, LocalDateTime timeSlotEnd, String terminal, boolean available) {
        this.taxi = taxi;
        this.timeSlotStart = timeSlotStart;
        this.timeSlotEnd = timeSlotEnd;
        this.terminal = terminal;
        this.available = available;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Taxi getTaxi() { return taxi; }
    public void setTaxi(Taxi taxi) { this.taxi = taxi; }

    public LocalDateTime getTimeSlotStart() { return timeSlotStart; }
    public void setTimeSlotStart(LocalDateTime timeSlotStart) { this.timeSlotStart = timeSlotStart; }

    public LocalDateTime getTimeSlotEnd() { return timeSlotEnd; }
    public void setTimeSlotEnd(LocalDateTime timeSlotEnd) { this.timeSlotEnd = timeSlotEnd; }

    public String getTerminal() { return terminal; }
    public void setTerminal(String terminal) { this.terminal = terminal; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
