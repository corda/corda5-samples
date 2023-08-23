package com.r3.developers.samples.fx

import com.r3.developers.samples.fx.contract.CurrencyCode
import net.corda.v5.base.types.MemberX500Name
import org.slf4j.Logger

internal fun Logger.logRequestingExchangeRate(counterparty: MemberX500Name, from: CurrencyCode, to: CurrencyCode) {
    info("Requesting exchange rate from $from to $to from $counterparty.")
}

internal fun Logger.logSendingExchangeRate(counterparty: MemberX500Name, from: CurrencyCode, to: CurrencyCode) {
    info("Sending exchange rate of $from to $to to $counterparty.")
}

internal fun Logger.logCreatingTransaction() {
    info("Creating UTXO transaction.")
}

internal fun Logger.logFinalizingTransaction() {
    info("Finalizing UTXO transaction.")
}