package com.r3.developers.samples.primenumber

import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey


@BelongsToContract(PrimeContract::class)
class Prime(
    val n: Int,
    val nthPrime: Int,
    private val participants: List<PublicKey>
): ContractState {
    override fun getParticipants(): List<PublicKey> = participants

    override fun toString() = "The ${n}th prime number is $nthPrime"
}