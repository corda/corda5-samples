package com.r3.developers.samples.negotiation.workflows.util

import com.r3.developers.samples.negotiation.Proposal
import net.corda.v5.application.flows.*
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction
import org.slf4j.LoggerFactory

object FinalizeFlow {
    private val log = LoggerFactory.getLogger(FinalizeFlow::class.java)

    @InitiatingFlow(protocol = "finalize-protocol")
    class FinalizeRequest(
        private val signedTransaction: UtxoSignedTransaction,
        private val sessions: List<FlowSession>
    ) :
        SubFlow<String> {
        // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
        @CordaInject
        var ledgerService: UtxoLedgerService? = null

        @Suspendable
        override fun call(): String {
            // Calls the Corda provided
            // finalise() function which gather signatures from the counterparty,
            // notarises the transaction and persists the transaction to each party's vault.
            val result: String
            result = try {
                val finalizedSignedTransaction = ledgerService!!.finalize(
                    signedTransaction,
                    sessions
                ).transaction
                finalizedSignedTransaction.id.toString()
            } // Soft fails the flow and returns the error message without throwing a flow exception.
            catch (e: Exception) {
                log.warn("Finality failed", e)
                "Finality failed, " + e.message
            }
            // Returns the transaction id converted as a string
            return result
        }
    }

    @InitiatedBy(protocol = "finalize-protocol")
    class FinalizeResponder : ResponderFlow {
        // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
        @CordaInject
        var utxoLedgerService: UtxoLedgerService? = null

        @Suspendable
        override fun call(session: FlowSession) {
            val proposalError = "Only the proposee can modify or accept a proposal."
            val successMessage = "Successfully finished modification responder flow - "
            try {
                val finalizedSignedTransaction = utxoLedgerService!!.receiveFinality(
                    session
                ) { transaction: UtxoLedgerTransaction ->
                    // goes into this if block is command is either modify or accept
                    if (!transaction.getInputStates(Proposal::class.java).isEmpty()) {
                        val proposee =
                            transaction.getInputStates(Proposal::class.java)[0].proposee.name
                        if (proposee.toString() != session.counterparty.toString()) {
                            throw CordaRuntimeException(proposalError)
                        }
                    }
                }.transaction
                log.info(successMessage + finalizedSignedTransaction.id)
            } // Soft fails the flow and log the exception.
            catch (e: Exception) {
                log.warn("Exceptionally finished responder flow", e)
            }
        }
    }
}
