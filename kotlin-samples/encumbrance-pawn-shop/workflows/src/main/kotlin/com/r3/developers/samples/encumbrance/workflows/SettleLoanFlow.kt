package com.r3.developers.samples.encumbrance.workflows

import com.r3.developers.samples.encumbrance.contracts.AssetContract
import com.r3.developers.samples.encumbrance.contracts.LoanContract
import com.r3.developers.samples.encumbrance.states.Asset
import com.r3.developers.samples.encumbrance.states.Loan
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant


data class SettleLoanFlowArgs(val loanId: String)

@InitiatingFlow(protocol = "settle-loan")
class SettleLoanFlow: ClientStartableFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        try {
            val (loanId) = requestBody.getRequestBodyAs(jsonMarshallingService, SettleLoanFlowArgs::class.java)

            val loanStateAndRef = ledgerService.findUnconsumedStatesByType(Loan::class.java).singleOrNull {
                it.state.contractState.loanId == loanId
            } ?: throw CordaRuntimeException("Multiple or zero Loan states with id ${loanId} found.")

            val loan = loanStateAndRef.state.contractState

            val assetStateAndRef = ledgerService.findUnconsumedStatesByType(Asset::class.java).singleOrNull {
                it.state.contractState.assetId == loan.collateral
            } ?: throw CordaRuntimeException("Multiple or zero Asset states with id ${loan.collateral} found.")

            val input = assetStateAndRef.state.contractState
            val asset = Asset(
                input.owner,
                input.assetName,
                input.assetId,
                listOf(input.owner.ledgerKey)
            )

            val txBuilder = ledgerService.createTransactionBuilder()
                .setNotary(assetStateAndRef.state.notaryName)
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                .addInputStates(loanStateAndRef.ref, assetStateAndRef.ref)
                .addOutputState(asset)
                .addCommand(LoanContract.LoanCommands.Settle())
                .addCommand(AssetContract.AssetCommands.Unlock())
                .addSignatories(loan.borrower.ledgerKey, loan.lender.ledgerKey)

            val signedTransaction: UtxoSignedTransaction = txBuilder.toSignedTransaction()
            val finalizedTransaction = ledgerService.finalize(
                signedTransaction, listOf(
                    flowMessaging.initiateFlow(
                        loanStateAndRef.state.contractState.lender.name
                    )
                )
            ).transaction

            return finalizedTransaction.id.toString()
        }catch (e: Exception) {
            log.warn("Failed to process utxo flow for request body '$requestBody' because:'${e.message}'")
            throw e
        }
    }
}
@InitiatedBy(protocol = "settle-loan")
class SettleLoanFlowResponder : ResponderFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @Suspendable
    override fun call(session: FlowSession) {
        log.info("TransferAssetResponder.call() called")

        try {
            val finalizedSignedTransaction = ledgerService.receiveFinality(session) { ledgerTransaction -> }
            log.info("Finished responder flow - ${finalizedSignedTransaction.transaction.id}")
        }
        // Soft fails the flow and log the exception.
        catch (e: Exception) {
            log.warn("Exceptionally finished responder flow", e)
        }

    }
}
/*
  {
     "clientRequestId": "settle-loan",
     "flowClassName": "com.r3.developers.samples.encumbrance.workflows.SettleLoanFlow",
     "requestBody": {
        "loanId": "<loan-id>"
     }
  }
   */