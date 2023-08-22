package com.r3.developers.samples.fx

import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey

//Links the contract state "ForeignExchange" with the ForeignExchangeContract contract class
@BelongsToContract(ForeignExchangeContract::class)
class ForeignExchange(
    val amount: Int,
    val convertingFrom: SupportedCurrencyCodes,
    val convertingTo: SupportedCurrencyCodes,
    val exchangeRate: Float,
    val convertedAmount: Int,
    val status: String, //todo experiment with
    private val participants: List<PublicKey>
): ContractState {
    override fun getParticipants(): List<PublicKey> = participants

    override fun toString() = "The FX transaction of $amount $convertingFrom to $convertingTo (exchange rate = $exchangeRate) is $convertedAmount is $status."
}
