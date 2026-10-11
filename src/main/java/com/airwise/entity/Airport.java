package com.airwise.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "airports", indexes = {
    @Index(name = "idx_airport_code", columnList = "code", unique = true)
})
public class Airport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String code; // e.g. BOM, DEL, BLR

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String country;

    private Double latitude;
    private Double longitude;

    @Column(length = 100)
    private String terminals; // e.g. "Terminal 1, Terminal 2"

    public Airport() {}

    public Airport(String code, String name, String city, String country, Double latitude, Double longitude, String terminals) {
        this.code = code;
        this.name = name;
        this.city = city;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.terminals = terminals;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getTerminals() { return terminals; }
    public void setTerminals(String terminals) { this.terminals = terminals; }
}
