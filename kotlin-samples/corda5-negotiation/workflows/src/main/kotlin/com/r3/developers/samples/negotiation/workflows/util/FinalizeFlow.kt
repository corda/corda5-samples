package com.r3.developers.samples.negotiation.workflows.util


import net.corda.v5.application.flows.*
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction
import org.slf4j.LoggerFactory


    @InitiatingFlow(protocol = "finalize-protocol")
    class FinalizeRequest(private val signedTransaction: UtxoSignedTransaction, private val otherMember: List<MemberX500Name>) : SubFlow<String> {
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
            // Calls the Corda provided
            // finalise() function which gather signatures from the counterparty,
            // notarises the transaction and persists the transaction to each party's vault.

            // Initiates a session with the other Member.
            val sessions = otherMember.map { flowMessaging.initiateFlow(it) }

            return try {
                val finalizedSignedTransaction = ledgerService.finalize(
                    signedTransaction,
                    sessions
                )
                // Returns the transaction id converted to a string.
                finalizedSignedTransaction.transaction.id.toString().also {
                    log.info("Success! Response: $it")
                }
            } // Soft fails the flow and returns the error message without throwing a flow exception.
            catch (e: Exception) {
                log.warn("Finality failed", e)
                "Finality failed, " + e.message
            }

        }
    }

    @InitiatedBy(protocol = "finalize-protocol")
    class FinalizeResponder : ResponderFlow {
        private companion object {
            val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        }

        // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
        @CordaInject
        lateinit var utxoLedgerService: UtxoLedgerService

        @Suspendable
        override fun call(session: FlowSession) {
            val successMessage = "Successfully finished the finalise responder flow - "
            try {
                val finalizedSignedTransaction = utxoLedgerService.receiveFinality(session) {}
                log.info(successMessage+finalizedSignedTransaction.transaction.id)
            } // Soft fails the flow and log the exception.
            catch (e: Exception) {
                log.warn("Exceptionally finished responder flow", e)
            }
        }
    }

