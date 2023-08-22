package com.r3.developers.samples.fx.workflows.primetodelete

import com.r3.developers.samples.fx.primetodelete.Query
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.ResponderFlow
import net.corda.v5.application.flows.SubFlow
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory

// This class creates a flow session with the other virtual node to request for its verification and signature
// If successful, the sub-flow will return the transaction id and log the successful signed transaction
@InitiatingFlow(protocol = "finalize-prime")
class FinalizeSignedPrimeTransactionSubFlow(private val primeFinalizationRequest: PrimeFinalizationRequest):
    SubFlow<PrimeFinalizationResponse> {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)

        const val FLOW_CALL = "FinalizeSignedPrimeTransactionSubFlow.call() called"
        const val FLOW_SUCCESS = "FinalizeSignedPrimeTransactionSubFlow.call() succeeded!"
        const val FLOW_FAIL = "FinalizeSignedPrimeTransactionSubFlow.call() failed"
    }

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @Suspendable
    override fun call(): PrimeFinalizationResponse {
        log.info(FLOW_CALL)
        val signedPrimeTransaction = primeFinalizationRequest.primeSignedTransaction
        val otherMemberName = primeFinalizationRequest.otherMemberName
        val session = flowMessaging.initiateFlow(otherMemberName)

        return try {
            val finalizedSignedTransaction = ledgerService.finalize(signedPrimeTransaction,listOf(session))
            val response = PrimeFinalizationResponse(finalizedSignedTransaction.transaction.id)
            response
                .also{ log.info(FLOW_SUCCESS, it)}
        } catch (e: Exception) {
            throw CordaRuntimeException(FLOW_FAIL, e)
        }
    }
}

// This class handles the flow session messages sent by the initiating subflow that requested for transaction finalization
@InitiatedBy(protocol = "finalize-prime")
class FinalizeSignedPrimeTransactionResponderSubFlow(): ResponderFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        const val FLOW_CALL = "FinalizeSignedPrimeTransactionResponderSubFlow.call() called."
        const val FLOW_FAIL = "FinalizeSignedPrimeTransactionResponderSubFlow.call() failed: "

        const val CHECK_SUCCESS = "Counterparty confirms the Create transaction has an input Query state signed by the service"
        const val REQUIRE_SINGLE_QUERY_INPUT_STATE = "Transaction requires a single input state"

    }

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @Suspendable
    override fun call(session: FlowSession) {
        log.info(FLOW_CALL)

        try{
            //[receiveFinality] will automatically verify the transaction and its signatures before signing it.
            val finalizedSignedTransaction = ledgerService.receiveFinality(session) { transaction ->
                // if the counter-party is able to do this successfully, it is their way of validating that the
                // third party has successfully signed the initial query for the prime number
                transaction.getInputStates(Query::class.java).singleOrNull() ?: throw CordaRuntimeException(
                    REQUIRE_SINGLE_QUERY_INPUT_STATE
                )
                    .also { log.info(CHECK_SUCCESS) }
            }
            log.info("Transaction id ${finalizedSignedTransaction.transaction.id} verified: $finalizedSignedTransaction")
        } catch(e: Exception) {
            throw CordaRuntimeException(FLOW_FAIL, e)
        }
    }
}