package com.r3.developers.samples.persistence.states

import com.r3.developers.samples.persistence.contracts.InsuranceContract
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey

/**
 * Insurance State
 * The state we would persist in a custom schema
 */
@BelongsToContract(InsuranceContract::class)
data class InsuranceState (
    // Represents the asset which is insured.
    // This will be used to demonstrate one-to-one relationship
    val vehicleDetail: VehicleDetail,
    val policyNumber: String,
    val insuredValue: Long,
    val duration: Int,
    val premium: Int,
    val insurer: MemberX500Name,
    val insuree: MemberX500Name,

    // Insurance claims made against the insurance policy
    // This will be used to demonstrate one-to-many relationship
    val claims: List<Claim> = listOf(),

    private val participants: List<PublicKey>) : ContractState {
    override fun getParticipants(): List<PublicKey> {
        return  participants;
    }
}