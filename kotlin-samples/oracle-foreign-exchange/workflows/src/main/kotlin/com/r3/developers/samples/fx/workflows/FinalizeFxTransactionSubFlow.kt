package com.r3.developers.samples.fx.workflows

import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.ResponderFlow
import net.corda.v5.application.flows.SubFlow
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction
import org.slf4j.LoggerFactory
import java.time.Instant

@InitiatingFlow(protocol = "finalize")
internal class FinalizeFxTransactionSubFlow(
private val fxTransaction: UtxoSignedTransaction,
private val sessions: List<FlowSession>
): SubFlow<UtxoSignedTransaction> {

    private companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        const val FLOW_CALL = "FinalizeFxTransactionSubFlow.call() called."
    }

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    override fun call(): UtxoSignedTransaction {
        log.info(FLOW_CALL)
        return ledgerService.finalize(fxTransaction, sessions).transaction
    }

}

@InitiatedBy(protocol = "finalize")
internal class FinalizeFxTransactionSubFlowResponder(): ResponderFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        const val FLOW_CALL = "FinalizeFxTransactionSubFlowResponder.call() called."
    }

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    override fun call(session: FlowSession) {
        log.info(FLOW_CALL)
        utxoLedgerService.receiveFinality(session){
            // Implement any pre-signing checks here...
            transaction -> transaction.timeWindow.until.isAfter(Instant.now())
        }
    }
}