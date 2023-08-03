package com.r3.developers.samples.primenumber.workflows

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

// This class creates a flow session with the primeService virtual node to request for its verification and signature
// If successful, the sub-flow will return the transaction id and log the successful signed transaction
@InitiatingFlow(protocol = "finalize-prime")
class FinalizePrimeSubFlow(private val signedTransaction: UtxoSignedTransaction, private val primeServiceName: MemberX500Name):
    SubFlow<SecureHash> {

        private companion object {
            val log = LoggerFactory.getLogger(this::class.java.enclosingClass)

            const val FLOW_CALL = "FinalizePrimeSubFlow.call() called"
            const val FLOW_SUCCESS = "FinalizePrimeSubFlow.call() succeeded! Final Transaction Id: "
            const val FLOW_FAIL = "FinalizePrimeSubFlow.call() transaction finality failed"
        }

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @Suspendable
    override fun call(): SecureHash {
        log.info(FLOW_CALL)

        val session = flowMessaging.initiateFlow(primeServiceName)

        return try {
            val finalizedSignedTransaction = ledgerService.finalize(signedTransaction, listOf(session))

            finalizedSignedTransaction.transaction.id
                .also {log.info(FLOW_SUCCESS + it)}
        } catch (e: Exception) {
            throw CordaRuntimeException(FLOW_FAIL, e)
        }
    }
}

// This class handles the flow session messages sent by the initiating subflow that requested for transaction finalization
@InitiatedBy(protocol = "finalize-prime")
class FinalizePrimeResponderSubFlow: ResponderFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)

        const val PRIME_SERVICE_SIGNS = "Requesting PrimeService signature."
        const val FLOW_CALL = "FinalizePrimeResponderSubFlow.call() called."
        const val FLOW_FAIL = "FinalizePrimeResponderSubFlow.call() failed: "
    }

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @Suspendable
    override fun call(session: FlowSession){
        log.info(FLOW_CALL)

        try{
            //[receiveFinality] will automatically verify the transaction and its signatures before signing it.
            val finalizedSignedTransaction = ledgerService.receiveFinality(session) { transaction ->
                log.info(PRIME_SERVICE_SIGNS)
                transaction.getOutputStates(Prime::class.java).singleOrNull() ?:throw CordaRuntimeException("Failed verification")
            }
            log.info("Transaction id ${finalizedSignedTransaction.transaction.id} verified: $finalizedSignedTransaction")
        } catch(e: Exception) {
            throw CordaRuntimeException(FLOW_FAIL, e)
        }
    }
}