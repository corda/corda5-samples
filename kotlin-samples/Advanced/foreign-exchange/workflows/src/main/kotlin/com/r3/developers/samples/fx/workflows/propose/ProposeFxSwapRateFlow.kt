package com.r3.developers.samples.fx.workflows.propose

import com.r3.developers.samples.fx.WithLogger
import com.r3.developers.samples.fx.contract.FxSwapRate
import com.r3.developers.samples.fx.contract.FxSwapRateContract
import com.r3.developers.samples.fx.logFinalizingTransaction
import com.r3.developers.samples.fx.logCreatingTransaction
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.SubFlow
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction
import net.corda.v5.membership.NotaryInfo

internal class ProposeFxSwapRateFlow(
    private val fxSwapRate: FxSwapRate,
    private val notaryInfo: NotaryInfo,
    private val sessions: List<FlowSession>
) : SubFlow<UtxoSignedTransaction> {

    private companion object : WithLogger

    @CordaInject
    private lateinit var utxoLedgerService: UtxoLedgerService

    @Suspendable
    override fun call(): UtxoSignedTransaction {
        logger.logCreatingTransaction()
        val transaction = utxoLedgerService
            .createTransactionBuilder()
            .addOutputState(fxSwapRate)
            .addCommand(FxSwapRateContract.Propose())
            .addSignatories(fxSwapRate.initiator)
            .setNotary(notaryInfo.name)
            .toSignedTransaction()

        logger.logFinalizingTransaction()
        return utxoLedgerService.finalize(transaction, sessions).transaction
    }
}