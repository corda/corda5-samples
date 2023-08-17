package com.r3.developers.samples.persistence.schema;

import net.corda.v5.base.annotations.CordaSerializable;

import javax.persistence.*;
import java.util.UUID;

/**
 * JPA Entity for saving claim details to the database table
 */
@Entity
@Table(name = "CLAIM_DETAIL")
@CordaSerializable
public class PersistentClaim {

    @Id
    private final String id;
    @Column(name = "claim_number")
    private final String claimNumber;
    @Column(name = "claim_description")
    private final String claimDescription;
    @Column(name = "claim_amount")
    private final Integer claimAmount;
    @Column
    private final String policyNumber;

    /**
     * Default constructor required by Hibernate
     */
    public PersistentClaim() {
        this.id = null;
        this.claimNumber = null;
        this.claimDescription = null;
        this.claimAmount = null;
        this.policyNumber = null;
    }

    public PersistentClaim(String claimNumber, String policyNumber, String claimDescription, Integer claimAmount) {
        this.id = UUID.randomUUID().toString();
        this.claimNumber = claimNumber;
        this.policyNumber = policyNumber;
        this.claimDescription = claimDescription;
        this.claimAmount = claimAmount;
    }

    public String getId() {
        return id;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public String getClaimNumber() {
        return claimNumber;
    }

    public String getClaimDescription() {
        return claimDescription;
    }

    public Integer getClaimAmount() {
        return claimAmount;
    }
}
