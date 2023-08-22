package com.r3.developers.samples.fx.primetodelete

import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey

// Links the state "Prime" with the PrimeContract Contract class
@BelongsToContract(PrimeContract::class)
class Prime(
    // The nthPrime is the indexed prime number defined by n
    val n: Int,
    val nthPrime: Int,
    private val participants: List<PublicKey>
): ContractState {
    override fun getParticipants(): List<PublicKey> = participants

    override fun toString() = "The ${n}th prime number is $nthPrime"
}