package com.r3.developers.samples.negotiation.workflows.Modify

import com.r3.developers.samples.negotiation.Proposal
import com.r3.developers.samples.negotiation.ProposalAndTradeContract.Modify
import com.r3.developers.samples.negotiation.util.Member
import com.r3.developers.samples.negotiation.workflows.util.FinalizeRequest
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.StateAndRef
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.time.Duration
import java.time.Instant
import java.util.stream.Collectors

@InitiatingFlow(protocol = "modify")
class ModifyFlowRequest : ClientStartableFlow {

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

        // Obtain the deserialized input arguments to the flow from the requestBody.
        val request = requestBody.getRequestBodyAs(jsonMarshallingService!!, ModifyFlowArgs::class.java)

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

        //creating a new Proposal as an output state
        val counterParty =
            if (memberLookup!!.myInfo().name == proposalInput.proposer.name) proposalInput.proposee else proposalInput.proposer
        val output = Proposal(
            request.newAmount,
            Member(proposalInput.buyer.name, proposalInput.buyer.ledgerKey),
            Member(proposalInput.seller.name, proposalInput.seller.ledgerKey),
            Member(memberLookup!!.myInfo().name, memberLookup!!.myInfo().ledgerKeys[0]),
            Member(counterParty.name, counterParty.ledgerKey),
            Member(memberLookup!!.myInfo().name, memberLookup!!.myInfo().ledgerKeys[0]),
            proposalID
        )

        // Initiating the transactionBuilder with command to "modify"
        val transactionBuilder = utxoLedgerService!!.createTransactionBuilder()
            .setNotary(proposalStateAndRef.state.notaryName)
            .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(Duration.ofMinutes(5).toMillis()))
            .addInputState(proposalStateAndRef.ref)
            .addOutputState(output)
            .addCommand(Modify())
            .addSignatories(output.participants)

        // Call FinalizeIOUSubFlow which will finalise the transaction.
        // If successful the flow will return a String of the created transaction id,
        // if not successful it will return an error message.
        return try {
            val signedTransaction = transactionBuilder.toSignedTransaction()
            flowEngine!!.subFlow(FinalizeRequest(signedTransaction, listOf(counterParty.name)))
        } catch (e: Exception) {
            throw CordaRuntimeException(e.message)
        }
    }
}
