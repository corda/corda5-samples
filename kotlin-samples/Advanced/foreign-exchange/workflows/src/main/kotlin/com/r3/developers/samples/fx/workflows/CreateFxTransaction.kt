package com.r3.developers.samples.fx.workflows

import com.r3.developers.samples.fx.SupportedCurrencyCodes
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.FlowEngine
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.membership.MemberInfo
import net.corda.v5.membership.NotaryInfo
import org.slf4j.LoggerFactory
import java.security.PublicKey

@CordaSerializable
data class QuoteFxRateRequest(val currencyPair: String, val fxServiceName: MemberX500Name)

@CordaSerializable
data class QuoteFxRateResponse(val conversionRate: Float)

@CordaSerializable
data class RecipientConfirmQuoteRequest(val currencyPair: String, val conversionRate: Float, val recipientName: MemberX500Name, val fxServiceName: MemberX500Name)

@CordaSerializable
data class RecipientConfirmQuoteResponse(val conversionRate: Float, val status: String)

@InitiatingFlow(protocol = "create-fx-tx")
class CreateFxTransaction(): ClientStartableFlow {

    private data class CreateFxTransaction(
        val convertingFrom: SupportedCurrencyCodes,
        val convertingTo: SupportedCurrencyCodes,
        val amount: Int,
        val recipientMemberName: String
    )

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)

        const val CALLED = "CreateFxTransaction.call() called."
        const val SET_UP = "Initializing flow variables."
        const val NOTARY_NOT_FOUND = "Requested notary not found on the network"
        const val SERVICE_NOT_FOUND = "Requested service not found on the network."
        const val RECIPIENT_NOT_FOUND = "The recipient member vNode in the request body is not found on the network."
        const val GET_FX_QUOTE = "Getting FX Rate Quote from FxService"
        const val GET_RECIPIENT_QUOTE_CONFIRMATION = "Asking recipient to confirm conversion rate with FxService."

        val notaryName: MemberX500Name = MemberX500Name.parse("CN=NotaryService, OU=Test Dept, O=R3, L=London, C=GB")
        val fxServiceName: MemberX500Name = MemberX500Name.parse("CN=ForeignExchangeService, OU=Test Dept, O=R3, L=London, C=GB")
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var notaryLookup: NotaryLookup

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {

        log.info(CALLED)
        val request = requestBody.getRequestBodyAs(jsonMarshallingService,CreateFxTransaction::class.java)
        val convertingFrom = request.convertingFrom
        val convertingTo = request.convertingTo
        val amount = request.amount
        val recipientName = MemberX500Name.parse(request.recipientMemberName)

        log.info(SET_UP)
        val notaryMemberInfo: NotaryInfo = notaryLookup.lookup(notaryName) ?: throw IllegalArgumentException("$NOTARY_NOT_FOUND: '$notaryName'")
        val fxServiceMemberInfo: MemberInfo = memberLookup.lookup(fxServiceName) ?: throw IllegalArgumentException("$SERVICE_NOT_FOUND: '$fxServiceName'")
        val recipientMemberInfo: MemberInfo = memberLookup.lookup(recipientName) ?: throw IllegalArgumentException("$RECIPIENT_NOT_FOUND: '$recipientName'")
        val ourPublicIdentity: PublicKey = memberLookup.myInfo().ledgerKeys.first()
        val fxServiceIdentity: PublicKey = fxServiceMemberInfo.ledgerKeys.first()

        log.info(GET_FX_QUOTE)
        val sessionAliceService = flowMessaging.initiateFlow(fxServiceName)
        val currencyPair = "$convertingFrom$convertingTo"
        val quoteRequest = QuoteFxRateRequest(currencyPair, fxServiceName)
        val quoteResponse: QuoteFxRateResponse = flowEngine.subFlow(QuoteExchangeRateSubFlow(quoteRequest,sessionAliceService))
        val conversionRate = quoteResponse.conversionRate

        log.info(GET_RECIPIENT_QUOTE_CONFIRMATION)
        val sessionAliceToRecipient = flowMessaging.initiateFlow(recipientName)
        val recipientConfirmationRequest = RecipientConfirmQuoteRequest(currencyPair,conversionRate,recipientName, fxServiceName)
        val recipientResponse: RecipientConfirmQuoteResponse = flowEngine.subFlow(ConfirmQuoteSubFlow(recipientConfirmationRequest,sessionAliceToRecipient))
        log.info("recipientResponse: $recipientResponse")

        return "Hey. currencyPair:$currencyPair | amount:$amount | conversionRate:$conversionRate | recipientResponse:$recipientResponse"
    }

}