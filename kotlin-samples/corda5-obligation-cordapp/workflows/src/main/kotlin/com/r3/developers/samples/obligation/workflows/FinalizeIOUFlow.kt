package com.r3.developers.samples.obligation.workflows

import com.r3.developers.samples.obligation.states.IOUState
import net.corda.v5.application.flows.*
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction
import org.slf4j.LoggerFactory

// @InitiatingFlow declares the protocol which will be used to link the initiator to the responder.
@InitiatingFlow(protocol = "finalize-iou-protocol")
class FinalizeIOUSubFlow(private val signedTransaction: UtxoSignedTransaction, private val otherMember: List<MemberX500Name>): SubFlow<String> {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @Suspendable
    override fun call(): String {

        log.info("FinalizeIOUSubFlow.call() called")

        // Initiates a session with the other Member.
        val sessions = otherMember.map { flowMessaging.initiateFlow(it) }

        return try {
            // Calls the Corda provided finalise() function which gather signatures from the counterparty,
            // notarises the transaction and persists the transaction to each party's vault.
            // On success returns the id of the transaction created.
            val finalizedSignedTransaction = ledgerService.finalize(
                signedTransaction,
                sessions
            )
            // Returns the transaction id converted to a string.
            finalizedSignedTransaction.transaction.id.toString().also {
                log.info("Success! Response: $it")
            }
        }
        // Soft fails the flow and returns the error message without throwing a flow exception.
        catch (e: Exception) {
            log.warn("Finality failed", e)
            "Finality failed, ${e.message}"
        }
    }
}

//@InitiatingBy declares the protocol which will be used to link the initiator to the responder.
@InitiatedBy(protocol = "finalize-iou-protocol")
class FinalizeIOUResponderFlow: ResponderFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @Suspendable
    override fun call(session: FlowSession) {

        log.info("FinalizeIOUResponderFlow.call() called")

        try {
            // Calls receiveFinality() function which provides the responder to the finalise() function
            // in the Initiating Flow. Accepts a lambda validator containing the business logic to decide whether
            // responder should sign the Transaction.
            val finalizedSignedTransaction = ledgerService.receiveFinality(session) { ledgerTransaction ->

                // Note, this exception will only be shown in the logs if Corda Logging is set to debug.
                val state = ledgerTransaction.getOutputStates(IOUState::class.java).singleOrNull() ?:
                throw CordaRuntimeException("Failed verification - transaction did not have exactly one output IOUState.")

                log.info("Verified the transaction- ${ledgerTransaction.id}")
            }
            log.info("Finished responder flow - ${finalizedSignedTransaction.transaction.id}")
        }
        // Soft fails the flow and log the exception.
        catch (e: Exception) {
            log.warn("Exceptionally finished responder flow", e)
        }
    }
}