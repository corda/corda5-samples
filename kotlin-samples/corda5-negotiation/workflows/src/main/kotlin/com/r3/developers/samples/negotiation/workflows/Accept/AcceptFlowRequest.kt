package com.r3.developers.samples.negotiation.workflows.Accept

import com.r3.developers.samples.negotiation.Proposal
import com.r3.developers.samples.negotiation.ProposalAndTradeContract.Accept
import com.r3.developers.samples.negotiation.Trade
import com.r3.developers.samples.negotiation.util.Member
import com.r3.developers.samples.negotiation.workflows.util.FinalizeFlow
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.StateAndRef
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.time.Duration
import java.time.Instant
import java.util.List
import java.util.stream.Collectors

@InitiatingFlow(protocol = "accept")
class AcceptFlowRequest : ClientStartableFlow {
    @CordaInject
    var flowMessaging: FlowMessaging? = null

    @CordaInject
    var jsonMarshallingService: JsonMarshallingService? = null

    @CordaInject
    var memberLookup: MemberLookup? = null

    @CordaInject
    var utxoLedgerService: UtxoLedgerService? = null

    @CordaInject
    var flowEngine: FlowEngine? = null

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        val statesNotFound = "Multiple or zero Proposal states not found wth id: "

        val request = requestBody.getRequestBodyAs(
            jsonMarshallingService!!,
            AcceptFlowArgs::class.java
        )
        // Get UUID from input JSON
        val proposalID = request.proposalID

        // Getting the old Proposal State as an input state
        val proposalStatAndRef = utxoLedgerService!!.findUnconsumedStatesByType(
            Proposal::class.java
        )
        val proposalStatAndRefWithId = proposalStatAndRef.stream().filter { it: StateAndRef<Proposal> ->
            it.state.contractState.proposalID == proposalID
        }.collect(Collectors.toList())
        if (proposalStatAndRefWithId.size != 1) throw CordaRuntimeException(statesNotFound + proposalID)
        val proposalStateAndRef = proposalStatAndRefWithId[0]
        val proposalInput = proposalStateAndRef.state.contractState

        // Creating a Trade as an output state
        val output = Trade(
            proposalInput.amount,
            Member(proposalInput.buyer.name, proposalInput.buyer.ledgerKey),
            Member(proposalInput.seller.name, proposalInput.seller.ledgerKey), proposalInput.participants
        )
        val counterParty =
            if (memberLookup!!.myInfo().name == proposalInput.proposer.name) proposalInput.proposee else proposalInput.proposer

        //Initiate the transactionBuilder with command to "Accept"
        val transactionBuilder = utxoLedgerService!!.createTransactionBuilder()
            .setNotary(proposalStateAndRef.state.notaryName)
            .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofMinutes(5).toMillis()))
            .addInputState(proposalStateAndRef.ref)
            .addOutputState(output)
            .addCommand(Accept())
            .addSignatories(output.participants)


        // Call FinalizeIOUSubFlow which will finalise the transaction.
        // If successful the flow will return a String of the created transaction id,
        // if not successful it will return an error message.
        return try {
            val signedTransaction = transactionBuilder.toSignedTransaction()
            val counterPartySession = flowMessaging!!.initiateFlow(counterParty.name)
            flowEngine!!.subFlow(FinalizeFlow.FinalizeRequest(signedTransaction, List.of(counterPartySession)))
        } catch (e: Exception) {
            throw CordaRuntimeException(e.message)
        }
    }
}
