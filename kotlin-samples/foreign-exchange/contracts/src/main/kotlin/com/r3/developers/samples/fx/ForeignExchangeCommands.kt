package com.r3.developers.samples.fx

import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.ledger.utxo.Command
import java.math.BigDecimal
import java.security.PublicKey

//This is used by the CreateFxTransaction workflow class to add a command to a ForeignExchange state
interface ForeignExchangeCommands: Command {
    class Create(
        val initiatorIdentity: PublicKey,
        val recipientIdentity: PublicKey,
        val amount: BigDecimal,
        val convertingFrom: SupportedCurrencyCodes,
        val convertingTo: SupportedCurrencyCodes,
        val exchangeRate: BigDecimal,
        val convertedAmount: BigDecimal,
        val status: TransactionStatuses
    ): ForeignExchangeCommands
}

@CordaSerializable
enum class SupportedCurrencyCodes { GBP, EUR, USD, CAD }

@CordaSerializable
enum class TransactionStatuses { SUCCESSFUL, FAILED }