package com.r3.developers.samples.persistence.schema;

import net.corda.v5.base.annotations.CordaSerializable;

import javax.persistence.*;
import java.util.List;

/**
 * JPA Entity for saving insurance details to the database table
 */
@Entity
@Table(name = "INSURANCE_DETAIL")
@CordaSerializable
public class PersistentInsurance {
    @Id
    @Column(name = "policy_number")
    private final String policyNumber;
    @Column(name = "insured_value")
    private final Long insuredValue;
    @Column private final Integer duration;
    @Column private final Integer premium;

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumns({
            @JoinColumn(name = "vehicleId", referencedColumnName = "vehicleid"),
            @JoinColumn(name = "registrationNumber", referencedColumnName = "registration_number"),
    })
    private final PersistentVehicle vehicle;

    @OneToMany(cascade = CascadeType.PERSIST)
    @JoinColumn(name="policyNumber")
    private List<PersistentClaim> claims;

    /**
     * Default constructor required by Hibernate
     */
    public PersistentInsurance() {
        this.policyNumber = null;
        this.insuredValue = null;
        this.duration = null;
        this.premium = null;
        this.vehicle = null;
        this.claims = null;
    }

    public PersistentInsurance(String policyNumber, Long insuredValue, Integer duration, Integer premium, PersistentVehicle vehicle,
                               List<PersistentClaim> claims) {
        this.policyNumber = policyNumber;
        this.insuredValue = insuredValue;
        this.duration = duration;
        this.premium = premium;
        this.vehicle = vehicle;
        this.claims = claims;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public Long getInsuredValue() {
        return insuredValue;
    }

    public Integer getPremium() {
        return premium;
    }

    public Integer getDuration() {
        return duration;
    }

    public PersistentVehicle getVehicle() {
        return vehicle;
    }

    public List<PersistentClaim> getClaims() {
        return claims;
    }
}
