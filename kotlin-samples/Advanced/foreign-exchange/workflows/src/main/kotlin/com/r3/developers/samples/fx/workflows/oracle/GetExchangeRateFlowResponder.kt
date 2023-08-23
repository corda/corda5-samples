package com.r3.developers.samples.fx.workflows.oracle

import com.r3.developers.samples.fx.WithLogger
import com.r3.developers.samples.fx.contract.CurrencyCode
import com.r3.developers.samples.fx.logSendingExchangeRate
import com.r3.developers.samples.fx.receive
import com.r3.developers.samples.fx.services.ExchangeRateService
import net.corda.v5.application.flows.SubFlow
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable

class GetExchangeRateFlowResponder(private val session: FlowSession) : SubFlow<Unit> {

    private companion object : WithLogger

    @Suspendable
    override fun call() {
        val (convertingFrom, convertingTo) = session.receive<Pair<CurrencyCode, CurrencyCode>>()
        val exchangeRate = ExchangeRateService.getExchangeRate(convertingFrom, convertingTo)

        logger.logSendingExchangeRate(session.counterparty, convertingFrom, convertingTo)
        session.send(exchangeRate)
    }
}