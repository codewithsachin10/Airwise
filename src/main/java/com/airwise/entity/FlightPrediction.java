package com.airwise.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "flight_predictions", indexes = {
    @Index(name = "idx_flight_pred_flight", columnList = "flight_id")
})
public class FlightPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column(nullable = false)
    private LocalDateTime predictedAt = LocalDateTime.now();

    @Column(nullable = false)
    private double delayProbability; // e.g. 0.65 (65%)

    @Column(nullable = false)
    private int expectedDelayMinutes;

    @Column(nullable = false, length = 20)
    private String riskLevel = "LOW"; // LOW, MEDIUM, HIGH

    @Column(length = 500)
    private String contributingFactors;

    private double weatherRiskScore = 0.0;
    private double congestionRiskScore = 0.0;

    @Column(nullable = false)
    private boolean demoData = true;

    public FlightPrediction() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Flight getFlight() { return flight; }
    public void setFlight(Flight flight) { this.flight = flight; }

    public LocalDateTime getPredictedAt() { return predictedAt; }
    public void setPredictedAt(LocalDateTime predictedAt) { this.predictedAt = predictedAt; }

    public double getDelayProbability() { return delayProbability; }
    public void setDelayProbability(double delayProbability) { this.delayProbability = delayProbability; }

    public int getExpectedDelayMinutes() { return expectedDelayMinutes; }
    public void setExpectedDelayMinutes(int expectedDelayMinutes) { this.expectedDelayMinutes = expectedDelayMinutes; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getContributingFactors() { return contributingFactors; }
    public void setContributingFactors(String contributingFactors) { this.contributingFactors = contributingFactors; }

    public double getWeatherRiskScore() { return weatherRiskScore; }
    public void setWeatherRiskScore(double weatherRiskScore) { this.weatherRiskScore = weatherRiskScore; }

    public double getCongestionRiskScore() { return congestionRiskScore; }
    public void setCongestionRiskScore(double congestionRiskScore) { this.congestionRiskScore = congestionRiskScore; }

    public boolean isDemoData() { return demoData; }
    public void setDemoData(boolean demoData) { this.demoData = demoData; }
}
