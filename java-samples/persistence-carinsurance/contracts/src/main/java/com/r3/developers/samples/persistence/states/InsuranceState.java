package com.r3.developers.samples.persistence.states;

import com.r3.developers.samples.persistence.contracts.InsuranceContract;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.utxo.BelongsToContract;
import net.corda.v5.ledger.utxo.ContractState;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.List;

/**
 * Insurance State
 * The state we would persist in a custom schema
 */
@BelongsToContract(InsuranceContract.class)
public class InsuranceState implements ContractState {

    // Represents the asset which is insured.
    // This will be used to demonstrate one-to-one relationship
    private final VehicleDetail vehicleDetail;

    // Fields related to the insurance state.
    private final String policyNumber;
    private final long insuredValue;
    private final int duration;
    private final int premium;

    private final MemberX500Name insurer;
    private final MemberX500Name insuree;

    // Insurance claims made against the insurance policy
    // This will be used to demonstrate one-to-many relationship
    private final List<Claim> claims;

    private final List<PublicKey> participants;


    public InsuranceState(String policyNumber, long insuredValue, int duration, int premium, MemberX500Name insurer,
                          MemberX500Name insuree, VehicleDetail  vehicleDetail, List<Claim> claims,
                          List<PublicKey> participants) {
        this.policyNumber = policyNumber;
        this.insuredValue = insuredValue;
        this.duration = duration;
        this.premium = premium;
        this.insurer = insurer;
        this.insuree = insuree;
        this.vehicleDetail = vehicleDetail;
        this.claims = claims;
        this.participants = participants;
    }

    public VehicleDetail getVehicleDetail() {
        return vehicleDetail;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public long getInsuredValue() {
        return insuredValue;
    }

    public int getDuration() {
        return duration;
    }

    public int getPremium() {
        return premium;
    }

    public MemberX500Name getInsurer() {
        return insurer;
    }

    public MemberX500Name getInsuree() {
        return insuree;
    }

    public List<Claim> getClaims() {
        return claims;
    }

    @Override
    public List<PublicKey> getParticipants() {
        return participants;
    }
}
