package com.r3.developers.samples.fx.workflows

import com.r3.developers.samples.fx.services.FxService
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.ResponderFlow
import net.corda.v5.application.flows.SubFlow
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import org.slf4j.LoggerFactory

@InitiatingFlow(protocol = "quote-fx")
class QuoteExchangeRateSubFlow(
    private val quoteFxRateRequest: QuoteFxRateRequest,
    private val session: FlowSession
): SubFlow<QuoteFxRateResponse> {
    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        const val FLOW_CALL = "QuoteExchangeRateSubFlow.call() called"
    }

    @Suspendable
    override fun call(): QuoteFxRateResponse {
        log.info(FLOW_CALL)
        return session.sendAndReceive(QuoteFxRateResponse::class.java,quoteFxRateRequest)
    }

}

@InitiatedBy(protocol = "quote-fx")
class QuoteExchangeRateSubFlowResponder(): ResponderFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)

        const val FLOW_CALL = "QuoteExchangeRateSubFlowResponder.call() called"
        const val QUOTING = "FxService to generate quote"
        const val SENDING = "Sending QuoteFxRateResponse to requester"
    }

    @Suspendable
    override fun call(session: FlowSession) {
        log.info(FLOW_CALL)

        val receivedMessage = session.receive(QuoteFxRateRequest::class.java)
        log.info("receivedMessage:$receivedMessage")

        log.info(QUOTING)
        val currencyPair = receivedMessage.currencyPair
        val conversionRate = FxService().quoteFxRate(currencyPair)
        val response = QuoteFxRateResponse(conversionRate)

        log.info(SENDING)
        session.send(response)
    }

}