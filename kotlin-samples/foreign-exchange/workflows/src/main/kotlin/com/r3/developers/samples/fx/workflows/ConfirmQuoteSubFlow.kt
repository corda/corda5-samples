package com.r3.developers.samples.fx.workflows

import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.FlowEngine
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.ResponderFlow
import net.corda.v5.application.flows.SubFlow
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import org.slf4j.LoggerFactory

@InitiatingFlow(protocol = "confirm-quote")
class ConfirmQuoteSubFlow(
    private val confirmQuoteRequest: RecipientConfirmQuoteRequest,
    private val sessionAliceToRecipient: FlowSession
): SubFlow<RecipientConfirmQuoteResponse> {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        const val FLOW_CALL = "ConfirmQuoteSubFlow.call() called"
    }

    @Suspendable
    override fun call(): RecipientConfirmQuoteResponse {
        log.info(FLOW_CALL)
        return sessionAliceToRecipient.sendAndReceive(RecipientConfirmQuoteResponse::class.java,confirmQuoteRequest)
    }
}


@InitiatedBy(protocol = "confirm-quote")
class ConfirmQuoteSubFlowResponder(): ResponderFlow {
    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        const val FLOW_CALL = "ConfirmQuoteSubFlowResponder.call() called"
        const val REQUEST_QUOTE = "Recipient to confirming quote from FxService"
        const val SENDING = "Sending confirmation to requester"
    }

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call(session: FlowSession) {
        log.info(FLOW_CALL)
        val receivedMessage = session.receive(RecipientConfirmQuoteRequest::class.java)
        val currencyPair = receivedMessage.currencyPair
        val fxServiceName = receivedMessage.fxServiceName
        val initiatorConversionRate = receivedMessage.conversionRate

        //query to oracle
        log.info(REQUEST_QUOTE)
        val requestBody = QuoteFxRateRequest(currencyPair, fxServiceName)
        val quoteAgainResponse: QuoteFxRateResponse = flowEngine.subFlow(QuoteAgainSubFlow(requestBody))
        val recipientConversionRate = quoteAgainResponse.conversionRate
        val doTheyMatch = (initiatorConversionRate==recipientConversionRate)
        log.info("initiatorConversionRate:$initiatorConversionRate | recipientConversionRate:$recipientConversionRate | doTheyMatch: $doTheyMatch")

        log.info(SENDING)
        val response = RecipientConfirmQuoteResponse(doTheyMatch,recipientConversionRate)
        session.send(response)
    }


}