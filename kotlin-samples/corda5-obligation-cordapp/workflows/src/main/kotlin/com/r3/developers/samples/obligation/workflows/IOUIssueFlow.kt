package com.r3.developers.samples.obligation.workflows

import com.r3.developers.samples.obligation.contracts.IOUContract
import com.r3.developers.samples.obligation.states.IOUState
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.FlowEngine
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.common.Party
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*


// A class to hold the deserialized arguments required to start the flow.
data class IOUIssueFlowArgs(val amount: String, val lender: String)

class IOUIssueFlow: ClientStartableFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @CordaInject
    lateinit var notaryLookup: NotaryLookup

    // FlowEngine service is required to run SubFlows.
    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        log.info("IOUIssueFlow.call() called")

        try {
            // Obtain the deserialized input arguments to the flow from the requestBody.
            val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, IOUIssueFlowArgs::class.java)

            // Get MemberInfos for the Vnode running the flow and the otherMember.
            // Good practice in Kotlin CorDapps is to only throw RuntimeException.
            // Note, in Java CorDapps only unchecked RuntimeExceptions can be thrown not
            // declared checked exceptions as this changes the method signature and breaks override.
            val myInfo = memberLookup.myInfo()
            val lenderInfo = memberLookup.lookup(MemberX500Name.parse(flowArgs.lender)) ?: throw CordaRuntimeException("MemberLookup can't find otherMember specified in flow arguments.")

            // Create the IOUState from the input arguments and member information.
            val iou = IOUState(
                amount = flowArgs.amount.toInt(),
                lender = lenderInfo.name,
                borrower = myInfo.name,
                paid = 0,
                linearId = UUID.randomUUID(),
                listOf(myInfo.ledgerKeys[0],lenderInfo.ledgerKeys[0])
            )
            // Obtain the notary.
            val notary = notaryLookup.notaryServices.single()

            // Use UTXOTransactionBuilder to build up the draft transaction.
            val txBuilder= ledgerService.getTransactionBuilder()
                .setNotary(Party(notary.name, notary.publicKey))
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                .addOutputState(iou)
                .addCommand(IOUContract.Issue())
                .addSignatories(iou.participants)

            // Convert the transaction builder to a UTXOSignedTransaction. Verifies the content of the
            // UtxoTransactionBuilder and signs the transaction with any required signatories that belong to
            // the current node.
            val signedTransaction = txBuilder.toSignedTransaction()

            // Call FinalizeIOUSubFlow which will finalise the transaction.
            // If successful the flow will return a String of the created transaction id,
            // if not successful it will return an error message.
            return flowEngine.subFlow(FinalizeIOUSubFlow(signedTransaction, listOf(lenderInfo.name)))


        }
        // Catch any exceptions, log them and rethrow the exception.
        catch (e: Exception) {
            log.warn("Failed to process utxo flow for request body '$requestBody' because:'${e.message}'")
            throw e
        }    }

}

/*
RequestBody for triggering the flow via http-rpc:
{
    "clientRequestId": "createiou-1",
    "flowClassName": "com.r3.developers.samples.obligation.workflows.IOUIssueFlow",
    "requestBody": {
        "amount":"20",
        "lender":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB"
        }
}
 */
