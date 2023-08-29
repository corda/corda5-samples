package com.r3.developers.samples.persistence.workflows;

import net.corda.v5.base.annotations.CordaSerializable;
import net.corda.v5.base.types.MemberX500Name;

@CordaSerializable
public class IssueInsuranceFlowArgs {

    private VehicleInfo vehicleInfo;
    private String policyNumber;
    private long insuredValue;
    private int duration;
    private int premium;
    private MemberX500Name insuree;

    public IssueInsuranceFlowArgs() {
    }

    public IssueInsuranceFlowArgs(VehicleInfo vehicleInfo, String policyNumber, long insuredValue,
                                  int duration, int premium, MemberX500Name insuree) {
        this.vehicleInfo = vehicleInfo;
        this.policyNumber = policyNumber;
        this.insuredValue = insuredValue;
        this.duration = duration;
        this.premium = premium;
        this.insuree = insuree;
    }

    public VehicleInfo getVehicleInfo() {
        return vehicleInfo;
    }

    public void setVehicleInfo(VehicleInfo vehicleInfo) {
        this.vehicleInfo = vehicleInfo;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public long getInsuredValue() {
        return insuredValue;
    }

    public void setInsuredValue(long insuredValue) {
        this.insuredValue = insuredValue;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getPremium() {
        return premium;
    }

    public void setPremium(int premium) {
        this.premium = premium;
    }

    public MemberX500Name getInsuree() {
        return insuree;
    }

    public void setInsuree(MemberX500Name insuree) {
        this.insuree = insuree;
    }
}
