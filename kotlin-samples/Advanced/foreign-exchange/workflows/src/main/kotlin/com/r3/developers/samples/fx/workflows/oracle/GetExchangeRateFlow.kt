package com.r3.developers.samples.fx.workflows.oracle

import com.r3.developers.samples.fx.WithLogger
import com.r3.developers.samples.fx.contract.CurrencyCode
import com.r3.developers.samples.fx.logRequestingExchangeRate
import com.r3.developers.samples.fx.sendAndReceive
import net.corda.v5.application.flows.SubFlow
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import java.math.BigDecimal

class GetExchangeRateFlow(
    private val session: FlowSession,
    private val convertingFrom: CurrencyCode,
    private val convertingTo: CurrencyCode
) : SubFlow<BigDecimal> {

    private companion object : WithLogger

    @Suspendable
    override fun call(): BigDecimal {
        logger.logRequestingExchangeRate(session.counterparty, convertingFrom, convertingTo)
        return session.sendAndReceive<BigDecimal>(convertingFrom to convertingTo)
    }
}