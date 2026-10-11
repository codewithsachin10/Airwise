package com.airwise.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "taxis", indexes = {
    @Index(name = "idx_taxi_veh_num", columnList = "vehicleNumber", unique = true)
})
public class Taxi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "operator_id", nullable = false)
    private TaxiOperator operator;

    @Column(nullable = false, unique = true, length = 30)
    private String vehicleNumber; // e.g. "MH-02-CB-4019"

    @Column(nullable = false, length = 80)
    private String model; // e.g. "Toyota Camry Hybrid", "Hyundai Ioniq 5"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaxiType taxiType = TaxiType.SEDAN;

    @Column(nullable = false)
    private int passengerCapacity = 4;

    @Column(nullable = false)
    private int luggageCapacity = 3;

    @Column(nullable = false)
    private double baseFarePerKm = 22.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaxiStatus status = TaxiStatus.AVAILABLE;

    @Column(length = 30)
    private String currentTerminal = "Terminal 2";

    public Taxi() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TaxiOperator getOperator() { return operator; }
    public void setOperator(TaxiOperator operator) { this.operator = operator; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public TaxiType getTaxiType() { return taxiType; }
    public void setTaxiType(TaxiType taxiType) { this.taxiType = taxiType; }

    public int getPassengerCapacity() { return passengerCapacity; }
    public void setPassengerCapacity(int passengerCapacity) { this.passengerCapacity = passengerCapacity; }

    public int getLuggageCapacity() { return luggageCapacity; }
    public void setLuggageCapacity(int luggageCapacity) { this.luggageCapacity = luggageCapacity; }

    public double getBaseFarePerKm() { return baseFarePerKm; }
    public void setBaseFarePerKm(double baseFarePerKm) { this.baseFarePerKm = baseFarePerKm; }

    public TaxiStatus getStatus() { return status; }
    public void setStatus(TaxiStatus status) { this.status = status; }

    public String getCurrentTerminal() { return currentTerminal; }
    public void setCurrentTerminal(String currentTerminal) { this.currentTerminal = currentTerminal; }
}
