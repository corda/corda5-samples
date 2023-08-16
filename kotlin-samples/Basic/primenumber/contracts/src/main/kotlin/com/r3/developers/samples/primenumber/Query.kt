package com.r3.developers.samples.primenumber

import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey

// Links the state "Query" with the PrimeContract Contract class
@BelongsToContract(PrimeContract::class)
class Query(
    val nthPrime: Int,
    private val participants: List<PublicKey>
): ContractState {

    override fun getParticipants(): List<PublicKey> = participants

    override fun toString() = "Service agrees nthPrime is $nthPrime"
}