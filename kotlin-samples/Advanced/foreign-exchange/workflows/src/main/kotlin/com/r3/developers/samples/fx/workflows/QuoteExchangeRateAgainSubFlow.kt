package com.r3.developers.samples.fx.workflows

import com.r3.developers.samples.fx.services.FxService
import net.corda.v5.application.flows.*
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import org.slf4j.LoggerFactory

@InitiatingFlow(protocol = "another-one")
class QuoteExchangeRateAgainSubFlow(private val session: FlowSession, private val quoteFxRateRequest: QuoteFxRateRequest): SubFlow<QuoteFxRateResponse> {
    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)

        const val FLOW_CALL = "QuoteExchangeRateAgainSubFlow.call() called"
    }

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @Suspendable
    override fun call(): QuoteFxRateResponse {
        log.info(FLOW_CALL)
        val session = flowMessaging.initiateFlow(quoteFxRateRequest.fxServiceName)
        return session.sendAndReceive(QuoteFxRateResponse::class.java,quoteFxRateRequest)
    }

}

@InitiatedBy(protocol = "another-one")
class QuoteExchangeRateAgainSubFlowResponder(): ResponderFlow {
    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)

        const val FLOW_CALL = "QuoteExchangeRateAgainSubFlowResponder.call() called"
        const val QUOTING = "FxService to generate quote [again]"
        const val SENDING = "Sending QuoteFxRateResponse to requester [again]"
    }

    @Suspendable
    override fun call(session: FlowSession) {
        log.info(FLOW_CALL)

        val receivedMessage = session.receive(QuoteFxRateRequest::class.java)
        log.info("[QuoteExchangeRateAgainSubFlowResponder] receivedMessage:$receivedMessage")

        log.info(QUOTING)
        val currencyPair = receivedMessage.currencyPair
        val conversionRate = FxService().quoteFxRate(currencyPair)
        val response = QuoteFxRateResponse(conversionRate)

        log.info(SENDING)
        session.send(response)
    }

}