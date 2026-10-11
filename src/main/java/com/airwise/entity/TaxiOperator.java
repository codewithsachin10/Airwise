package com.airwise.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "taxi_operators")
public class TaxiOperator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 120)
    private String companyName;

    @Column(nullable = false, length = 60)
    private String licenseNumber;

    @Column(length = 30)
    private String contactPhone;

    private int fleetSize = 15;

    public TaxiOperator() {}

    public TaxiOperator(User user, String companyName, String licenseNumber, String contactPhone, int fleetSize) {
        this.user = user;
        this.companyName = companyName;
        this.licenseNumber = licenseNumber;
        this.contactPhone = contactPhone;
        this.fleetSize = fleetSize;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public int getFleetSize() { return fleetSize; }
    public void setFleetSize(int fleetSize) { this.fleetSize = fleetSize; }
}
