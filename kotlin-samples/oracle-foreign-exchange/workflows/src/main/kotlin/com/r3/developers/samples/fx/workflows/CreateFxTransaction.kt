package com.r3.developers.samples.fx.workflows

import com.r3.developers.samples.fx.ForeignExchange
import com.r3.developers.samples.fx.ForeignExchangeCommands
import com.r3.developers.samples.fx.SupportedCurrencyCodes
import com.r3.developers.samples.fx.TransactionStatuses
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.FlowEngine
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.membership.MemberInfo
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.security.PublicKey
import java.time.Instant
import java.time.temporal.ChronoUnit

// This is the client-side worfklow that gets called first. It will:
// - set up and identify the recipient member node, the FxService node and the notary node
// - initiate a flowSession between Alice and the FxService and trigger the QuoteExchangeRateSubFlow sub-flow to get a quote given the requested inputs
// - initiate another flowSession between Alice and the recipient (specified by MemberX500 name in the inputs) to confirm the quote
// --   Note, how the ConfirmQuoteSubFlow sub-flow that gets triggered will trigger the QuoteAgainSubFlow sub-flow so that
// --   the flowSession connects the recipient with the FxService
// - if both parties agree with the conversion rate, build the ForeignExchange transaction including the output state, command, etc.
// - finalize the transaction and return the id as well as the output state to communicate the entire flow's completion.
@InitiatingFlow(protocol = "create-fx-tx")
class CreateFxTransaction(): ClientStartableFlow {

    private data class CreateFxTransaction(
        val convertingFrom: SupportedCurrencyCodes,
        val convertingTo: SupportedCurrencyCodes,
        val amount: BigDecimal,
        val recipientMemberName: String
    )

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)

        const val CALLED = "CreateFxTransaction.call() called."
        const val SET_UP = "Initializing flow variables."
        const val RECIPIENT_NOT_FOUND = "The recipient member vNode in the request body is not found on the network."
        const val GET_FX_QUOTE = "Getting FX Rate Quote from FxService"
        const val GET_RECIPIENT_QUOTE_CONFIRMATION = "Asking recipient to confirm conversion rate with FxService."
        const val QUOTE_UNCONFIRMED = "Recipient declined the fx quote proposed by initiator. "
        const val PREPARE_TRANSACTION = "Prparing the transaction variables."
        const val BUILD_TRANSACTION = "Building the transaction."
        const val FINALIZE_TRANSACTION = "Finalizing the transaction."

        val notaryName: MemberX500Name = MemberX500Name.parse("CN=NotaryService, OU=Test Dept, O=R3, L=London, C=GB")
        val fxServiceName: MemberX500Name = MemberX500Name.parse("CN=ForeignExchangeService, OU=Test Dept, O=R3, L=London, C=GB")
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {

        // Deserializing the JSON string input into meaningful data for the workflow
        log.info(CALLED)
        val request = requestBody.getRequestBodyAs(jsonMarshallingService,CreateFxTransaction::class.java)
        val convertingFrom = request.convertingFrom
        val convertingTo = request.convertingTo
        val amount = request.amount
        val recipientName = MemberX500Name.parse(request.recipientMemberName)

        // Obtaining the public identity of the member nodes involved in the transaction
        log.info(SET_UP)
        val recipientMemberInfo: MemberInfo = memberLookup.lookup(recipientName)
            ?: throw IllegalArgumentException("$RECIPIENT_NOT_FOUND: '$recipientName'")
        val ourPublicIdentity: PublicKey = memberLookup.myInfo().ledgerKeys.first()
        val recipientMemberIdentity: PublicKey = recipientMemberInfo.ledgerKeys.first()

        // Executing the sub-flow to obtain a quote from the oracle FxService
        log.info(GET_FX_QUOTE)
        val sessionAliceService = flowMessaging.initiateFlow(fxServiceName)
        val currencyPair = "$convertingFrom$convertingTo"
        val quoteRequest = QuoteFxRateRequest(currencyPair, fxServiceName)
        val quoteResponse: QuoteFxRateResponse = flowEngine.subFlow(QuoteExchangeRateSubFlow(quoteRequest,sessionAliceService))
        val initiatorConversionRate = quoteResponse.conversionRate
        val convertedAmount = amount*initiatorConversionRate

        // Communicating the FX rate with the recipient so that it may also execute sub-flows to obtain
        // a quote from the oracle FxService themselves
        log.info(GET_RECIPIENT_QUOTE_CONFIRMATION)
        val sessionAliceRecipientQuote = flowMessaging.initiateFlow(recipientName)
        val recipientConfirmationRequest = RecipientConfirmQuoteRequest(currencyPair,initiatorConversionRate,recipientName, fxServiceName)
        val recipientResponse: RecipientConfirmQuoteResponse = flowEngine.subFlow(ConfirmQuoteSubFlow(recipientConfirmationRequest,sessionAliceRecipientQuote))

        // If the quotes obtained by the initiator and the recipient, then the workflow ends and returns the discrepancy
        if(!recipientResponse.confirmed){
           return buildString {
               append(QUOTE_UNCONFIRMED)
               append("currencyPair:$currencyPair. ")
               append("initiatorConversionRate:${initiatorConversionRate.toDouble()}. ")
               append("recipientConversionRate:${recipientResponse.recipientConversionRate.toDouble()}.")
           }
        }

        // Creating the output state and command involved when building the transaction
        log.info(PREPARE_TRANSACTION)
        val outputState = ForeignExchange(
            ourPublicIdentity,
            recipientMemberIdentity,
            amount,
            convertingFrom,
            convertingTo,
            initiatorConversionRate,
            convertedAmount,
            TransactionStatuses.SUCCESSFUL,
            listOf(ourPublicIdentity,recipientMemberIdentity)
        )
        val command = ForeignExchangeCommands.Create(
            ourPublicIdentity,
            recipientMemberIdentity,
            amount,
            convertingFrom,
            convertingTo,
            initiatorConversionRate,
            convertedAmount,
            TransactionStatuses.SUCCESSFUL
        )

        // The initiator creates and signs the foreign exchange transaction
        log.info(BUILD_TRANSACTION)
        val fxTransaction = ledgerService.createTransactionBuilder()
            .setNotary(notaryName)
            .addOutputState(outputState) // contract state verification happens under the hood here
            .addCommand(command)
            .addSignatories(listOf(ourPublicIdentity,recipientMemberIdentity))
            .setTimeWindowUntil(Instant.now().plus(1,ChronoUnit.DAYS))
            .toSignedTransaction()

        // The initiator requests for the finalization of the transaction from all the involved parties
        log.info(FINALIZE_TRANSACTION)
        val sessionAliceRecipientFinalize = flowMessaging.initiateFlow(recipientName)
        val finalizedTransaction = flowEngine.subFlow(FinalizeFxTransactionSubFlow(fxTransaction,listOf(sessionAliceRecipientFinalize)))

        // remember how we override the state ForeignExchange.toString() method to be a meaningful output message
        return "$outputState | transaction:${finalizedTransaction.id}"
    }

}