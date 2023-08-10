package com.r3.developers.samples.referencestate.workflows

import com.r3.developers.samples.referencestate.contracts.SanctionListContract
import com.r3.developers.samples.referencestate.states.Member
import com.r3.developers.samples.referencestate.states.SanctionList
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction
import net.corda.v5.membership.MemberInfo
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import kotlin.streams.toList


data class UpdateSanctionListFlowArgs(val partyToSanction: MemberX500Name)

@InitiatingFlow(protocol = "update-sanction-list")
class UpdateSanctionListFlow : ClientStartableFlow {

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
            val (partyToSanctionName) = requestBody.getRequestBodyAs(jsonMarshallingService,UpdateSanctionListFlowArgs::class.java)
            val myInfo = memberLookup.myInfo()
            val partyToSanction =  memberLookup.lookup(partyToSanctionName) ?: throw  CordaRuntimeException("MemberLookup can't find partyToSanctionName specified in flow arguments.")
            val allParties = memberLookup.lookup().filter { it -> !it.name.commonName.equals("NotaryRep1") }.toList()

            val oldSanctionListRef = ledgerService.findUnconsumedStatesByType(SanctionList::class.java).singleOrNull()?: throw  CordaRuntimeException("Sanction List not found.")
            val oldSanctionList =oldSanctionListRef.state.contractState
            val badPeople = ArrayList(oldSanctionList.badPeople)
            badPeople.add(Member(partyToSanctionName,partyToSanction.ledgerKeys[0]))

            val newSanctionList = SanctionList(badPeople,oldSanctionList.issuer,oldSanctionList.uniqueId,oldSanctionList.participants)

            val notaryName = oldSanctionListRef.state.notaryName

            val txBuilder= ledgerService.createTransactionBuilder()
                .setNotary(notaryName)
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofDays(1).toMillis()))
                .addInputState(oldSanctionListRef.ref)
                .addOutputState(newSanctionList)
                .addCommand(SanctionListContract.SanctionListCommand.Update())
                .addSignatories(myInfo.ledgerKeys[0])

            val signedTransaction: UtxoSignedTransaction = txBuilder.toSignedTransaction()

            val finalizedSignedTransaction = ledgerService.finalize(
                signedTransaction,
                (allParties - myInfo).stream().map { it: MemberInfo -> flowMessaging.initiateFlow(it.name) }.toList()
            ).transaction

            return finalizedSignedTransaction.id.toString()
        }catch (e: Exception) {
            log.warn("Failed to process utxo flow for request body '$requestBody' because:'${e.message}'")
            throw e
        }
    }
}

@InitiatedBy(protocol = "update-sanction-list")
class UpdateSanctionListFlowResponder : ResponderFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @Suspendable
    override fun call(session: FlowSession) {
        log.info("UpdateSanctionListFlowResponder.call() called")

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