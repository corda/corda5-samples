package com.r3.developers.samples.fx.contract

import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.math.BigDecimal
import java.security.PublicKey
import java.time.Instant

@BelongsToContract(FxSwapRateContract::class)
data class FxSwapRate(
    val initiator: PublicKey,
    val responder: PublicKey,
    val convertingFrom: CurrencyCode,
    val convertingTo: CurrencyCode,
    val amount: BigDecimal,
    val exchangeRate: BigDecimal,
    val expires: Instant,
    val status: FxSwapRateStatus
) : ContractState {

    val convertedAmount: BigDecimal get() = amount * exchangeRate

    override fun getParticipants(): List<PublicKey> {
        return listOf(initiator, responder).distinct()
    }

    internal fun immutableEquals(other: FxSwapRate): Boolean {
        return this === other
                || other.initiator == initiator
                && other.responder == responder
                && other.convertingFrom == convertingFrom
                && other.convertingTo == convertingTo
                && other.amount == amount
                && other.exchangeRate == exchangeRate
                && other.expires == expires
    }

    override fun toString(): String = buildString {
        append("Exchange: ")
        append("$amount $convertingFrom from $initiator")
        append(" with ")
        append("$convertedAmount $convertingTo from $responder")
        append(": $status (expires at $expires.")
    }
}