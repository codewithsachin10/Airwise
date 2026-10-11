package com.airwise.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "taxi_demand_predictions")
public class TaxiDemandPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "airport_id", nullable = false)
    private Airport airport;

    @Column(nullable = false, length = 30)
    private String terminal = "Terminal 2";

    @Column(nullable = false)
    private LocalDateTime timeSlotStart;

    @Column(nullable = false)
    private LocalDateTime timeSlotEnd;

    private int predictedDemand = 120;
    private int availableTaxis = 85;
    private int reservedTaxis = 30;
    private int unmetDemand = 5;

    @Column(nullable = false, length = 20)
    private String demandLevel = "MEDIUM"; // LOW, MEDIUM, HIGH

    private int recommendedVehicles = 15;

    @Column(nullable = false)
    private boolean demoData = true;

    public TaxiDemandPrediction() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Airport getAirport() { return airport; }
    public void setAirport(Airport airport) { this.airport = airport; }

    public String getTerminal() { return terminal; }
    public void setTerminal(String terminal) { this.terminal = terminal; }

    public LocalDateTime getTimeSlotStart() { return timeSlotStart; }
    public void setTimeSlotStart(LocalDateTime timeSlotStart) { this.timeSlotStart = timeSlotStart; }

    public LocalDateTime getTimeSlotEnd() { return timeSlotEnd; }
    public void setTimeSlotEnd(LocalDateTime timeSlotEnd) { this.timeSlotEnd = timeSlotEnd; }

    public int getPredictedDemand() { return predictedDemand; }
    public void setPredictedDemand(int predictedDemand) { this.predictedDemand = predictedDemand; }

    public int getAvailableTaxis() { return availableTaxis; }
    public void setAvailableTaxis(int availableTaxis) { this.availableTaxis = availableTaxis; }

    public int getReservedTaxis() { return reservedTaxis; }
    public void setReservedTaxis(int reservedTaxis) { this.reservedTaxis = reservedTaxis; }

    public int getUnmetDemand() { return unmetDemand; }
    public void setUnmetDemand(int unmetDemand) { this.unmetDemand = unmetDemand; }

    public String getDemandLevel() { return demandLevel; }
    public void setDemandLevel(String demandLevel) { this.demandLevel = demandLevel; }

    public int getRecommendedVehicles() { return recommendedVehicles; }
    public void setRecommendedVehicles(int recommendedVehicles) { this.recommendedVehicles = recommendedVehicles; }

    public boolean isDemoData() { return demoData; }
    public void setDemoData(boolean demoData) { this.demoData = demoData; }
}
