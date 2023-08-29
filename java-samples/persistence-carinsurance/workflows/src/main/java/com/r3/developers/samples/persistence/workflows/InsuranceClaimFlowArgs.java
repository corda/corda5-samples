package com.r3.developers.samples.persistence.workflows;

import net.corda.v5.base.annotations.CordaSerializable;

@CordaSerializable
public class InsuranceClaimFlowArgs {
    private String claimNumber;
    private String claimDescription;
    private int claimAmount;
    private String policyNumber;

    public InsuranceClaimFlowArgs() {
    }

    public InsuranceClaimFlowArgs(String claimNumber, String claimDescription, int claimAmount, String policyNumber) {
        this.claimNumber = claimNumber;
        this.claimDescription = claimDescription;
        this.claimAmount = claimAmount;
        this.policyNumber = policyNumber;
    }

    public String getClaimNumber() {
        return claimNumber;
    }

    public void setClaimNumber(String claimNumber) {
        this.claimNumber = claimNumber;
    }

    public String getClaimDescription() {
        return claimDescription;
    }

    public void setClaimDescription(String claimDescription) {
        this.claimDescription = claimDescription;
    }

    public int getClaimAmount() {
        return claimAmount;
    }

    public void setClaimAmount(int claimAmount) {
        this.claimAmount = claimAmount;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }
}
