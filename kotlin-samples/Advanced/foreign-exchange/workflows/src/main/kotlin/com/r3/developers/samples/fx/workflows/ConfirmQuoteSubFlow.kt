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
    private val confirmQuoteRequest: RecipientConfirmQuoteRequest
): SubFlow<RecipientConfirmQuoteResponse> {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)

        const val FLOW_CALL = "ConfirmQuoteSubFlow.call() called"
    }

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @Suspendable
    override fun call(): RecipientConfirmQuoteResponse {
        log.info(FLOW_CALL)
        val session = flowMessaging.initiateFlow(confirmQuoteRequest.recipientName)
        return session.sendAndReceive(RecipientConfirmQuoteResponse::class.java,confirmQuoteRequest)
    }
}

@InitiatedBy(protocol = "confirm-quote")
@InitiatingFlow(protocol = "another-one")
class ConfirmQuoteSubFlowResponder(): ResponderFlow {
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
        log.info("receivedMessage:$receivedMessage")
        val currencyPair = receivedMessage.currencyPair
        val fxServiceName = receivedMessage.fxServiceName
        val conversionRateProposed = receivedMessage.conversionRate
        log.info("[ConfirmQuoteSubFlowResponder] currencyPair:$currencyPair | fxServiceName:$fxServiceName | conversationRateProposed:$conversionRateProposed")

        val session2 = flowMessaging.initiateFlow(fxServiceName)
        val requestBody = QuoteFxRateRequest(currencyPair,fxServiceName)

        //todo figure out what to do here
//        val subsubflowResponse = flowEngine.subFlow(QuoteExchangeRateAgainSubFlow(requestBody))

        val response = RecipientConfirmQuoteResponse(1.5f,"accepted")
        session.send(response)






//        log.info(REQUEST_QUOTE)
//        val confirmQuoteRequest = QuoteFxRateRequest(currencyPair,fxServiceName)
//        val conversionRateDoubleCheck: QuoteFxRateResponse = flowEngine.subFlow(QuoteExchangeRateAgainSubFlow(confirmQuoteRequest))
//        log.info("conversionRateDoubleCheck.conversionRate: ${conversionRateDoubleCheck.conversionRate}")
//
//        var status = ""
//        if(conversionRateDoubleCheck.conversionRate == conversationRateProposed) {
//            status = "accepted"
//        } else {
//            status = "not accepted"
//        }
//
//        val response = RecipientConfirmQuoteResponse(conversionRateDoubleCheck.conversionRate,status)
//        session.send(response)



//        val confirmQuoteMessage = QuoteFxRateRequest(currencyPair,fxServiceName)
//        val recipientAndServiceSession = flowMessaging.initiateFlow(fxServiceName)
//        val quoteAgainResponse = recipientAndServiceSession.sendAndReceive(QuoteFxRateResponse::class.java,confirmQuoteMessage)
//        val conversionRateDoubleCheck = quoteAgainResponse.conversionRate
//        val doTheyMatch = conversationRateProposed == conversionRateDoubleCheck
//        log.info("Proposed FX Rate: $conversationRateProposed | FX Rate when double checking: $conversionRateDoubleCheck | DoTheyMatch:$doTheyMatch")
//
//        log.info(SENDING)
//        val response = RecipientConfirmQuoteResponse(doTheyMatch)
//        session.send(response)

    }


}