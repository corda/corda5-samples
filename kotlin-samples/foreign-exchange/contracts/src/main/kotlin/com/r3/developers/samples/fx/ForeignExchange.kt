package com.r3.developers.samples.fx

import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.math.BigDecimal
import java.security.PublicKey

//Links the contract state "ForeignExchange" with the ForeignExchangeContract contract class
@BelongsToContract(ForeignExchangeContract::class)
class ForeignExchange(
    val initiatorIdentity: PublicKey,
    val recipientIdentity: PublicKey,
    val amount: BigDecimal,
    val convertingFrom: SupportedCurrencyCodes,
    val convertingTo: SupportedCurrencyCodes,
    val exchangeRate: BigDecimal,
    val convertedAmount: BigDecimal,
    val status: TransactionStatuses,
    private val participants: List<PublicKey>
): ContractState {
    override fun getParticipants(): List<PublicKey> = participants

    override fun toString() = "The FX transaction of $amount $convertingFrom to $convertingTo (exchange rate = ${exchangeRate.toDouble()}) amounting to ${convertedAmount.toDouble()} is $status."
}
