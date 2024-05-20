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
import net.corda.v5.ledger.utxo.Command
import net.corda.v5.ledger.utxo.StateRef
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.security.PublicKey
import java.time.Duration
import java.time.Instant
import java.util.*

data class TestContractFlowArgs(val otherMember: String)

class TestContractFlow: ClientStartableFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API
    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @CordaInject
    lateinit var notaryLookup: NotaryLookup

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {


        val results = mutableMapOf<String, String>()

        log.info("TestContractFlow.call() called")

        class FakeCommand : Command

        try {
            val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, TestContractFlowArgs::class.java)

            val myInfo = memberLookup.myInfo()

            val otherMember = memberLookup.lookup(MemberX500Name.parse(flowArgs.otherMember)) ?:
            throw CordaRuntimeException("MemberLookup can't find otherMember specified in flow arguments.")

            // Obtain the Notary name and public key.
            val notary = notaryLookup.notaryServices.first()

            // Create a well formed transaction with an output State which can be referenced
            // as an input StateRef in the tests
            lateinit var inputStateRef: StateRef
            lateinit var iouStateId: UUID

            try {
                val iouState = IOUState(
                    amount = 10,
                    paid = 3,
                    lender = myInfo.name,
                    borrower = otherMember.name,
                    linearId = UUID.randomUUID(),
                    participants = listOf(myInfo.ledgerKeys.first(), otherMember.ledgerKeys.first())
                )

                iouStateId = iouState.linearId

                val txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.name)
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(iouState)
                    .addCommand(IOUContract.Issue())
                    .addSignatories(iouState.participants)

                @Suppress("DEPRECATION", "UNUSED_VARIABLE")
                val signedTransaction = txBuilder.toSignedTransaction()

                inputStateRef = StateRef(signedTransaction.id, 0)
                flowEngine.subFlow(FinalizeIOUSubFlow(signedTransaction, listOf(otherMember.name)))

            } catch (e:Exception) {
                throw CordaRuntimeException("Set up transaction could not be created because of exception: ${e.message}")
            }




            // *************   START TESTS ****************

            // Multiple Commands not permitted
            results["Multiple Commands not permitted"] = try {
                val iouState = IOUState(
                    amount = 10,
                    paid = 3,
                    lender = myInfo.name,
                    borrower = otherMember.name,
                    linearId = UUID.randomUUID(),
                    participants = listOf(myInfo.ledgerKeys.first(), otherMember.ledgerKeys.first())
                )

                val txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.name)
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(iouState)
                    .addInputState(inputStateRef)
                    .addCommand(IOUContract.Issue())
                    .addCommand(IOUContract.Transfer())
                    .addSignatories(iouState.participants)

                @Suppress("DEPRECATION", "UNUSED_VARIABLE")
                val signedTransaction = txBuilder.toSignedTransaction()

                "Fail"

            } catch (e:Exception) {
                val exceptionMessage =  e.message ?: "No exception message"
                if (exceptionMessage.contains("Requires a single command.")) {
                    "Pass" }
                else {
                    "Contract failed but with a different Exception: ${e.message}"
                }
            }
            // Only Two Participants
            results["Only Two Participants"] = try {
                val iouState = IOUState(
                    amount = 10,
                    paid = 3,
                    lender = myInfo.name,
                    borrower = otherMember.name,
                    linearId = UUID.randomUUID(),
                    participants = listOf(myInfo.ledgerKeys.first())
                )

                val txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.name)
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(iouState)
                    .addCommand(IOUContract.Issue())
                    .addSignatories(iouState.participants)

                @Suppress("DEPRECATION", "UNUSED_VARIABLE")
                val signedTransaction = txBuilder.toSignedTransaction()

                "Fail"

            } catch (e:Exception) {
                val exceptionMessage =  e.message ?: "No exception message"
                if (exceptionMessage.contains("only two participants")) {
                    "Pass" }
                else {
                    "Contract failed but with a different Exception: ${e.message}"
                }
            }


            // Settle needs only one output
            results["Settle needs only one output"] = try {
                val iouState = IOUState(
                    amount = 10,
                    paid = 3,
                    lender = myInfo.name,
                    borrower = otherMember.name,
                    linearId = UUID.randomUUID(),
                    participants = listOf(myInfo.ledgerKeys.first(), otherMember.ledgerKeys.first())
                )

                val txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.name)
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(iouState)
                    .addOutputState(iouState)
                    .addInputState(inputStateRef)
                    .addCommand(IOUContract.Settle())
                    .addSignatories(iouState.participants)

                @Suppress("DEPRECATION", "UNUSED_VARIABLE")
                val signedTransaction = txBuilder.toSignedTransaction()

                "Fail"

            } catch (e:Exception) {
                val exceptionMessage =  e.message ?: "No exception message"
                if (exceptionMessage.contains("one output state")) {
                    "Pass" }
                else {
                    "Contract failed but with a different Exception: ${e.message}"
                }
            }


            // Transfer needs only one output
            results["Transfer needs only one output"] = try {
                val iouState = IOUState(
                    amount = 10,
                    paid = 3,
                    lender = myInfo.name,
                    borrower = otherMember.name,
                    linearId = UUID.randomUUID(),
                    participants = listOf(myInfo.ledgerKeys.first(), otherMember.ledgerKeys.first())
                )

                val txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.name)
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(iouState)
                    .addOutputState(iouState)
                    .addInputState(inputStateRef)
                    .addCommand(IOUContract.Transfer())
                    .addSignatories(iouState.participants)

                @Suppress("DEPRECATION", "UNUSED_VARIABLE")
                val signedTransaction = txBuilder.toSignedTransaction()

                "Fail"

            } catch (e:Exception) {
                val exceptionMessage =  e.message ?: "No exception message"
                if (exceptionMessage.contains("one output state")) {
                    "Pass" }
                else {
                    "Contract failed but with a different Exception: ${e.message}"
                }
            }


            // Issue needs only one output
            results["Issue needs only one output"] = try {
                val iouState = IOUState(
                    amount = 10,
                    paid = 3,
                    lender = myInfo.name,
                    borrower = otherMember.name,
                    linearId = UUID.randomUUID(),
                    participants = listOf(myInfo.ledgerKeys.first(), otherMember.ledgerKeys.first())
                )

                val txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.name)
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(iouState)
                    .addOutputState(iouState)
                    .addCommand(IOUContract.Issue())
                    .addSignatories(iouState.participants)

                @Suppress("DEPRECATION", "UNUSED_VARIABLE")
                val signedTransaction = txBuilder.toSignedTransaction()

                "Fail"

            } catch (e:Exception) {
                val exceptionMessage =  e.message ?: "No exception message"
                if (exceptionMessage.contains("one output state")) {
                    "Pass" }
                else {
                    "Contract failed but with a different Exception: ${e.message}"
                }
            }

            // Transfer requires one input
            results["Transfer requires one input"] = try {
                val iouState = IOUState(
                    amount = 10,
                    paid = 3,
                    lender = myInfo.name,
                    borrower = otherMember.name,
                    linearId = UUID.randomUUID(),
                    participants = listOf(myInfo.ledgerKeys.first(), otherMember.ledgerKeys.first())
                )

                val txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.name)
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(iouState)
                    .addCommand(IOUContract.Transfer())
                    .addSignatories(iouState.participants)

                @Suppress("DEPRECATION", "UNUSED_VARIABLE")
                val signedTransaction = txBuilder.toSignedTransaction()

                "Fail"

            } catch (e:Exception) {
                val exceptionMessage =  e.message ?: "No exception message"
                if (exceptionMessage.contains("one input state")) {
                    "Pass" }
                else {
                    "Contract failed but with a different Exception: ${e.message}"
                }
            }

            // Settle requires one input
            results["Settle requires one input"] = try {
                val iouState = IOUState(
                    amount = 10,
                    paid = 3,
                    lender = myInfo.name,
                    borrower = otherMember.name,
                    linearId = UUID.randomUUID(),
                    participants = listOf(myInfo.ledgerKeys.first(), otherMember.ledgerKeys.first())
                )

                val txBuilder = ledgerService.createTransactionBuilder()
                    .setNotary(notary.name)
                    .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                    .addOutputState(iouState)
                    .addCommand(IOUContract.Settle())
                    .addSignatories(iouState.participants)

                @Suppress("DEPRECATION", "UNUSED_VARIABLE")
                val signedTransaction = txBuilder.toSignedTransaction()

                "Fail"

            } catch (e:Exception) {
                val exceptionMessage =  e.message ?: "No exception message"
                if (exceptionMessage.contains("one input state")) {
                    "Pass" }
                else {
                    "Contract failed but with a different Exception: ${e.message}"
                }
            }



            return results.toString()

            // Catch any exceptions, log them and rethrow the exception.
        } catch (e: Exception) {
            log.warn("Failed to process utxo flow for request body '$requestBody' because:'${e.message}'")
            throw e
        }
    }

}
/*
{
    "clientRequestId": "dummy-1",
    "flowClassName": "com.r3.developers.samples.obligation.workflows.TestContractFlow",
    "requestBody": {
        "otherMember":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB"
    }
}

 */