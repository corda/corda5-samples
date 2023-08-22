package com.r3.developers.samples.fx

import net.corda.v5.ledger.utxo.Command

interface ForeignExchangeCommands: Command {
    class Create(
        val amount: Int,
        val convertingFrom: SupportedCurrencyCodes,
        val convertingTo: SupportedCurrencyCodes,
        val exchangeRate: Float,
        val convertedAmount: Int,
        val status: String
    ): ForeignExchangeCommands
}

//ISO-4127 Currency Codes
enum class SupportedCurrencyCodes { GBP, EUR, USD, CAD }