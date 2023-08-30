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

@InitiatingFlow(protocol = "quote-again")
class QuoteAgainSubFlow (
    private val quoteFxRateRequest: QuoteFxRateRequest,
): SubFlow<QuoteFxRateResponse> {

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @Suspendable
    override fun call(): QuoteFxRateResponse {
        val sessionRecipientAndService = flowMessaging.initiateFlow(quoteFxRateRequest.fxServiceName)
        return sessionRecipientAndService.sendAndReceive(QuoteFxRateResponse::class.java,quoteFxRateRequest)
    }
}

@InitiatedBy(protocol = "quote-again")
class QuoteAgainSubFlowResponder(): ResponderFlow {

    @Suspendable
    override fun call(session: FlowSession) {
        val receivedMessage = session.receive(QuoteFxRateRequest::class.java)
        val currencyPair = receivedMessage.currencyPair
        val conversionRate = FxService().quoteFxRate(currencyPair)
        val response = QuoteFxRateResponse(conversionRate)
        session.send(response)
    }
}