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
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*

data class IOUTransferFlowArgs(val newLender: String, val iouID: UUID)

class IOUTransferFlow: ClientStartableFlow {

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

    // FlowEngine service is required to run SubFlows.
    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        log.info("IOUSettleFlow.call() called")

        try {
            // Obtain the deserialized input arguments to the flow from the requestBody.
            val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, IOUTransferFlowArgs::class.java)
            // Get MemberInfos for the Vnode running the flow and the otherMember.
            // Good practice in Kotlin CorDapps is to only throw RuntimeException.
            // Note, in Java CorDapps only unchecked RuntimeExceptions can be thrown not
            // declared checked exceptions as this changes the method signature and breaks override.
            val myInfo = memberLookup.myInfo()

            val iouID = flowArgs.iouID

            val iouStateAndRefs = ledgerService.findUnconsumedStatesByType(IOUState::class.java)
            val iouStateAndRefsWithId = iouStateAndRefs.filter { it.state.contractState.linearId.equals(iouID)}
            if (iouStateAndRefsWithId.size != 1) throw CordaRuntimeException("Multiple or zero IOU states with id \" + iouID + \" found")
            val iouStateAndRef = iouStateAndRefsWithId[0]

            val notary = iouStateAndRef.state.notary

            val iouInput = iouStateAndRef.state.contractState

            if (!(myInfo.name.equals(iouInput.lender))) throw CordaRuntimeException("Only IOU borrower can settle the IOU.")

            val borrower = memberLookup.lookup(iouInput.borrower) ?: throw CordaRuntimeException("MemberLookup can't find otherMember specified in flow arguments.")
            val newLenderInfo = memberLookup.lookup(MemberX500Name.parse(flowArgs.newLender)) ?: throw CordaRuntimeException("MemberLookup can't find otherMember specified in flow arguments.")


            val iouOutput = iouInput.withNewLender(newLenderInfo.name, listOf(borrower.ledgerKeys[0],newLenderInfo.ledgerKeys[0]))


            // Use UTXOTransactionBuilder to build up the draft transaction.
            val txBuilder= ledgerService.getTransactionBuilder()
                .setNotary(notary)
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                .addInputState(iouStateAndRef.ref)
                .addOutputState(iouOutput)
                .addCommand(IOUContract.Settle())
                .addSignatories(iouOutput.participants + listOf(myInfo.ledgerKeys[0]))

            // Convert the transaction builder to a UTXOSignedTransaction. Verifies the content of the
            // UtxoTransactionBuilder and signs the transaction with any required signatories that belong to
            // the current node.
            val signedTransaction = txBuilder.toSignedTransaction()

            // Call FinalizeIOUSubFlow which will finalise the transaction.
            // If successful the flow will return a String of the created transaction id,
            // if not successful it will return an error message.
            return flowEngine.subFlow(FinalizeIOUSubFlow(signedTransaction, listOf(borrower.name,newLenderInfo.name)))


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
    "clientRequestId": "transferiou-1",
    "flowClassName": "com.r3.developers.samples.obligation.workflows.IOUTransferFlow",
    "requestBody": {
        "newLender":"CN=Charlie, OU=Test Dept, O=R3, L=London, C=GB",
        "iouID":"4ea35048-879e-43f0-9593-343388715627"
        }
}
4ea35048-879e-43f0-9593-343388715627
*/
