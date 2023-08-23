package com.r3.developers.samples.fx.workflows

import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.FlowEngine
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.ResponderFlow
import net.corda.v5.application.flows.SubFlow
import net.corda.v5.application.messaging.FlowMessaging
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

@Suspendable
@InitiatingFlow(protocol = "aaa")
@InitiatedBy(protocol = "confirm-quote")
class ConfirmQuoteSubFlowResponder(
//    private val session: FlowSession
): ResponderFlow {
    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        const val FLOW_CALL = "ConfirmQuoteSubFlowResponder.call() called"
        const val REQUEST_QUOTE = "Recipient to confirming quote from FxService"
        const val SENDING = "Sending confirmation to requester"
    }

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @Suspendable
    override fun call(session: FlowSession) {
        log.info(FLOW_CALL)
        val receivedMessage = session.receive(RecipientConfirmQuoteRequest::class.java)
        val currencyPair = receivedMessage.currencyPair
        val fxServiceName = receivedMessage.fxServiceName
        val conversionRateProposed = receivedMessage.conversionRate
        log.info("[ConfirmQuoteSubFlowResponder] currencyPair:$currencyPair | fxServiceName:$fxServiceName | conversationRateProposed:$conversionRateProposed")

        //todo fix
        val sessionRecipientAndService = flowMessaging.initiateFlow(fxServiceName)
        val requestBody = QuoteFxRateRequest(currencyPair, fxServiceName)
        val ireallyhopethisworkssubsbuflowresponse = flowEngine.subFlow(QuoteExchangeRateSubFlow(requestBody,sessionRecipientAndService))
        log.info("did it work: $ireallyhopethisworkssubsbuflowresponse")

        val response = RecipientConfirmQuoteResponse(1.5f,"accepted")
        session.send(response)







    }


}