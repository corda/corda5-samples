package com.r3.developers.samples.encumbrance.workflows

import com.r3.developers.samples.encumbrance.contracts.AssetContract
import com.r3.developers.samples.encumbrance.states.Asset
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
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant


data class TransferAssetFlowArgs(val assetId: String, val buyer: String)


@InitiatingFlow(protocol = "transfer-asset")
class TransferAssetFlow: ClientStartableFlow {

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
            val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, TransferAssetFlowArgs::class.java)
            val otherMember = memberLookup.lookup(MemberX500Name.parse(flowArgs.buyer)) ?: throw CordaRuntimeException("MemberLookup can't find otherMember specified in flow arguments.")
            val buyer = Member(otherMember.name,otherMember.ledgerKeys[0])

            val stateAndRef = ledgerService.findUnconsumedStatesByType(Asset::class.java).singleOrNull {
                it.state.contractState.assetId == flowArgs.assetId
            } ?: throw CordaRuntimeException("Multiple or zero Asset states with id ${flowArgs.assetId} found.")

            val input = stateAndRef.state.contractState
            val output = Asset(
                buyer,
                input.assetName,
                input.assetId,
                listOf(buyer.ledgerKey)
            )

            val notaryName = stateAndRef.state.notaryName

            val txBuilder= ledgerService.createTransactionBuilder()
                .setNotary(notaryName)
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                .addInputState(stateAndRef.ref)
                .addOutputState(output)
                .addCommand(AssetContract.AssetCommands.Transfer())
                .addSignatories(buyer.ledgerKey,input.owner.ledgerKey)

            val signedTransaction = txBuilder.toSignedTransaction()

            val finalizedSignedTransaction = ledgerService.finalize(
                signedTransaction,listOf(flowMessaging.initiateFlow(buyer.name))
            ).transaction

            return finalizedSignedTransaction.id.toString()

        }
        // Catch any exceptions, log them and rethrow the exception.
        catch (e: Exception) {
            log.warn("Failed to process utxo flow for request body '$requestBody' because:'${e.message}'")
            throw e
        }
    }
}


@InitiatedBy(protocol = "transfer-asset")
class TransferAssetResponder : ResponderFlow {

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
     "clientRequestId": "transfer-asset",
     "flowClassName": "com.r3.developers.samples.encumbrance.workflows.TransferAssetFlow",
     "requestBody": {
        "assetId": "<asset-id>", // Check Viewing Data in the Vault Section on get this
        "buyer": "CN=Charlie, OU=Test Dept, O=R3, L=London, C=GB"
     }
  }
   */
/*
  {
  "clientRequestId": "transfer-asset-second-try",
  "flowClassName": "com.r3.developers.samples.encumbrance.workflows.TransferAssetFlow",
     "requestBody": {
        "assetId": "<asset-id>",
        "buyer": "CN=Charlie, OU=Test Dept, O=R3, L=London, C=GB"
     }
  }
   */