package com.r3.developers.samples.primenumber.flows

import com.r3.developers.samples.primenumber.Prime
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.ResponderFlow
import net.corda.v5.application.flows.SubFlow
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.crypto.SecureHash
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction
import org.slf4j.LoggerFactory

@InitiatingFlow(protocol = "finalize-prime")
class FinalizePrimeSubFlow(private val signedTransaction: UtxoSignedTransaction, private val oracleName: MemberX500Name):
    SubFlow<SecureHash> {

        private companion object {
            val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        }

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @Suspendable
    override fun call(): SecureHash {
        log.info("FinalizePrimeSubFlow.call() called")

        val session = flowMessaging.initiateFlow(oracleName)

        return try {
            val finalizedSignedTransaction = ledgerService.finalize(signedTransaction, listOf(session))

            finalizedSignedTransaction.transaction.id
                .also {log.info("Success! Response: $it")}
        } catch (e: Exception) {
            throw CordaRuntimeException("Finality failed", e)
        }
    }
}

@InitiatedBy(protocol = "finalize-prime")
class FinalizePrimeResponderSubFlow: ResponderFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)

        const val ORACLE_SIGNS = "Requesting oracle signature."
    }

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @Suspendable
    override fun call(session: FlowSession){
        log.info("FinalizePrimeResponderSubFlow.call() called")

        try{
            /*
            * [receiveFinality] will automatically verify the transaction and its signatures before signing it.
            * However, just because a transaction is contractually valid doesn't mean we necessarily want to sign.
            * What if we don't want to deal with the counterparty in question, or the value is too high,
            * or we're not happy with the transaction's structure? [UtxoTransactionValidator] (the lambda created
            * here) allows us to define the additional checks. If any of these conditions are not met,
            * we will not sign the transaction - even if the transaction and its signatures are contractually valid.
            */
            val finalizedSignedTransaction = ledgerService.receiveFinality(session) { transaction ->
                log.info(ORACLE_SIGNS)

                transaction.getOutputStates(Prime::class.java).singleOrNull()
                    ?:throw CordaRuntimeException("Failed verification")
            }
        log.info("Transaction id ${finalizedSignedTransaction.transaction.id} verified: $finalizedSignedTransaction")
        } catch(e: Exception) {
            log.warn("FinalizePrimeResponderSubFlow failed", e)
            throw CordaRuntimeException("FinalizePrimeResponderSubFlow failed: ${e.message}")
        }
    }
}