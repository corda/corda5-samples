package com.r3.developers.samples.referencestate.workflows

import com.r3.developers.samples.referencestate.contracts.SanctionableIOUContract
import com.r3.developers.samples.referencestate.states.Member
import com.r3.developers.samples.referencestate.states.SanctionList
import com.r3.developers.samples.referencestate.states.SanctionableIOUState
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.StateAndRef
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import kotlin.streams.toList


data class IOUIssueFlowArgs(val iouValue: Int, val lenderName: MemberX500Name, val sanctionAuthority: MemberX500Name)

@InitiatingFlow(protocol = "iou-issue")
class IOUIssueFlow : ClientStartableFlow {

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
    lateinit var notaryLookup: NotaryLookup

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        try{
            val (iouValue, lenderName, sanctionAuthority) = requestBody.getRequestBodyAs(jsonMarshallingService,IOUIssueFlowArgs::class.java)
            val myInfo = memberLookup.myInfo()
            val lender =  memberLookup.lookup(lenderName) ?: throw CordaRuntimeException("MemberLookup can't find otherMember specified in flow arguments.")

            val sanctionsListToUse = getSanctionsList(sanctionAuthority) ?: throw CordaRuntimeException("The sanctionAuthority did not issue any sanction list")

            val iouState = SanctionableIOUState(iouValue,
                Member(lender.name,lender.ledgerKeys[0]),
                Member(myInfo.name,myInfo.ledgerKeys[0])
            )

            val notaryName = sanctionsListToUse.state.notaryName
            val txBuilder = ledgerService.createTransactionBuilder()
                .setNotary(notaryName)
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                .addReferenceState(sanctionsListToUse.ref)
                .addOutputState(iouState)
                .addCommand(SanctionableIOUContract.SanctionableIOUCommands.Create(sanctionAuthority))
                .addSignatories(iouState.participants)

            val signedTransaction = txBuilder.toSignedTransaction()

            val finalizedTransaction = ledgerService.finalize(signedTransaction,listOf(flowMessaging.initiateFlow(lenderName))).transaction

            return finalizedTransaction.id.toString()
        }catch (e: Exception) {
            log.warn("Failed to process utxo flow for request body '$requestBody' because:'${e.message}'")
            throw e
        }
    }

    @Suspendable
    fun getSanctionsList(sanctionsBody: MemberX500Name): StateAndRef<SanctionList>? {
        val sanctionLists = ledgerService.findUnconsumedStatesByType(SanctionList::class.java).stream().filter { it ->
            it.state.contractState.issuer.name.equals(sanctionsBody)
        }.toList()[0]
        return sanctionLists
    }
}

@InitiatedBy(protocol = "iou-issue")
class IOUIssueFlowResponder : ResponderFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @Suspendable
    override fun call(session: FlowSession) {
        log.info("IOUIssueFlowResponder.call() called")

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