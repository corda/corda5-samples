package com.r3.developers.samples.encumbrance.workflows

import com.r3.developers.samples.encumbrance.contracts.AssetContract
import com.r3.developers.samples.encumbrance.contracts.LoanContract
import com.r3.developers.samples.encumbrance.states.Asset
import com.r3.developers.samples.encumbrance.states.Loan
import com.r3.developers.samples.encumbrance.states.Member
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*


data class RequestLoanFlowArgs(val lender: String, val loanAmount : Int, val collateral: String )

@InitiatingFlow(protocol = "issue-loan")
class RequestLoanFlow : ClientStartableFlow {
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
        try{
            val (lenderArgs, loanAmount, collateral) = requestBody.getRequestBodyAs(
                jsonMarshallingService,
                RequestLoanFlowArgs::class.java
            )

            val myInfo = memberLookup.myInfo()
            val otherMember = memberLookup.lookup(MemberX500Name.parse(lenderArgs)) ?: throw CordaRuntimeException("MemberLookup can't find otherMember specified in flow arguments.")
            val lender = Member(otherMember.name,otherMember.ledgerKeys[0])

            val stateAndRef = ledgerService.findUnconsumedStatesByType(Asset::class.java).singleOrNull {
                it.state.contractState.assetId == collateral
            } ?: throw CordaRuntimeException("Multiple or zero Asset states with id ${collateral} found.")

            val input = stateAndRef.state.contractState

            val loan = Loan(
                UUID.randomUUID().toString(),
                Member(lender.name,lender.ledgerKey),
                Member(myInfo.name,myInfo.ledgerKeys[0]),
                loanAmount,
                collateral
            )

            val collateralAsset = Asset(
                input.owner,
                input.assetName,
                input.assetId,
                listOf(input.owner.ledgerKey,myInfo.ledgerKeys[0])
            )

            val notaryName = stateAndRef.state.notaryName
            val txBuilder= ledgerService.createTransactionBuilder()
                .setNotary(notaryName)
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                .addInputState(stateAndRef.ref)
                .addEncumberedOutputStates("loan-" + loan.loanId, loan, collateralAsset)
                .addCommand(LoanContract.LoanCommands.Issue())
                .addCommand(AssetContract.AssetCommands.Lock())
                .addSignatories(loan.lender.ledgerKey, loan.borrower.ledgerKey)

            val signedTransaction: UtxoSignedTransaction = txBuilder.toSignedTransaction()
            val finalizedTransaction =
                ledgerService.finalize(signedTransaction, listOf(flowMessaging.initiateFlow(lender.name))).transaction

            return finalizedTransaction.id.toString()
        }catch (e: Exception) {
            log.warn("Failed to process utxo flow for request body '$requestBody' because:'${e.message}'")
            throw e
        }
    }
}

@InitiatedBy(protocol = "issue-loan")
class RequestLoanFlowResponder : ResponderFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @Suspendable
    override fun call(session: FlowSession) {
        log.info("RequestLoanFlowResponder.call() called")

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
  "clientRequestId": "request-loan",
  "flowClassName": "com.r3.developers.samples.encumbrance.workflows.RequestLoanFlow",
  "requestBody": {
     "lender": "CN=Bob, OU=Test Dept, O=R3, L=London, C=GB",
     "loanAmount": 1000,
     "collateral": "<asset-id>" // Check Viewing Data in the Vault Section on get this
  }
}
 */