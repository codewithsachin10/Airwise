package com.airwise.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "passenger_flow_records", indexes = {
    @Index(name = "idx_flow_airport", columnList = "airport_id")
})
public class PassengerFlowRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "airport_id", nullable = false)
    private Airport airport;

    @Column(nullable = false, length = 30)
    private String terminal = "Terminal 2";

    @Column(nullable = false)
    private LocalDateTime recordedAt = LocalDateTime.now();

    private int passengerVolume = 2400;
    private int securityQueueMinutes = 14;
    private int checkinQueueMinutes = 8;
    private int activeLanes = 6;
    private int recommendedLanes = 6;

    @Column(nullable = false, length = 20)
    private String congestionLevel = "MODERATE"; // LOW, MODERATE, HIGH, CRITICAL

    public PassengerFlowRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Airport getAirport() { return airport; }
    public void setAirport(Airport airport) { this.airport = airport; }

    public String getTerminal() { return terminal; }
    public void setTerminal(String terminal) { this.terminal = terminal; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }

    public int getPassengerVolume() { return passengerVolume; }
    public void setPassengerVolume(int passengerVolume) { this.passengerVolume = passengerVolume; }

    public int getSecurityQueueMinutes() { return securityQueueMinutes; }
    public void setSecurityQueueMinutes(int securityQueueMinutes) { this.securityQueueMinutes = securityQueueMinutes; }

    public int getCheckinQueueMinutes() { return checkinQueueMinutes; }
    public void setCheckinQueueMinutes(int checkinQueueMinutes) { this.checkinQueueMinutes = checkinQueueMinutes; }

    public int getActiveLanes() { return activeLanes; }
    public void setActiveLanes(int activeLanes) { this.activeLanes = activeLanes; }

    public int getRecommendedLanes() { return recommendedLanes; }
    public void setRecommendedLanes(int recommendedLanes) { this.recommendedLanes = recommendedLanes; }

    public String getCongestionLevel() { return congestionLevel; }
    public void setCongestionLevel(String congestionLevel) { this.congestionLevel = congestionLevel; }
}
