package com.r3.developers.samples.persistence.states;

import net.corda.v5.base.annotations.CordaSerializable;

/**
 * Simple POJO class for the claim details.
 */
@CordaSerializable
public class Claim {

    private final String claimNumber;
    private final String claimDescription;
    private final int claimAmount;

    public Claim(String claimNumber, String claimDescription, int claimAmount) {
        this.claimNumber = claimNumber;
        this.claimDescription = claimDescription;
        this.claimAmount = claimAmount;
    }

    public String getClaimNumber() {
        return claimNumber;
    }

    public String getClaimDescription() {
        return claimDescription;
    }

    public int getClaimAmount() {
        return claimAmount;
    }
}
