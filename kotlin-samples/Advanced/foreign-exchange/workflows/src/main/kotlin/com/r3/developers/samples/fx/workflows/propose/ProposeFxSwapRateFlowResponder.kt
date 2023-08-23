package com.r3.developers.samples.fx.workflows.propose

import com.r3.developers.samples.fx.WithLogger
import com.r3.developers.samples.fx.logFinalizingTransaction
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.SubFlow
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction

internal class ProposeFxSwapRateFlowResponder(private val session: FlowSession) : SubFlow<UtxoSignedTransaction> {

    private companion object : WithLogger

    @CordaInject
    private lateinit var utxoLedgerService: UtxoLedgerService

    @Suspendable
    override fun call(): UtxoSignedTransaction {

        logger.logFinalizingTransaction()
        return utxoLedgerService.receiveFinality(session) {
            // Implement any pre-signing checks here...
        }.transaction
    }
}