package com.r3.developers.samples.tokens.workflows

import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.SubFlow
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction
import org.slf4j.LoggerFactory

@InitiatingFlow(protocol = "finalize-gold-protocol")
class FinalizeGoldTokenSubFlow(
    private val signedTransaction: UtxoSignedTransaction,
    private val otherMember: MemberX500Name
) : SubFlow<String> {
    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @Suspendable
    override fun call(): String {
        log.info("FinalizeMintSubFlow.call() called")

        // Initiates a session with the other Member.
        val session = flowMessaging.initiateFlow(otherMember)

        // Calls the Corda provided finalise() function which gather signatures from the counterparty,
        // notarises the transaction and persists the transaction to each party's vault.
        // On success returns the id of the transaction created.
        var result: String
        try{
            val sessionsList = listOf(session)
            val finalizedSignedTransaction = ledgerService.finalize(
                signedTransaction,
                sessionsList
            ).transaction

            result = finalizedSignedTransaction.id.toString()
            log.info("Success! Response: $result")
        }
        // Soft fails the flow and returns the error message without throwing a flow exception.
        catch (e: Exception){
            log.warn("Finality failed", e)
            result = "Finality failed, " + e.message
        }
        // Returns the transaction id converted as a string
        return result
    }
}