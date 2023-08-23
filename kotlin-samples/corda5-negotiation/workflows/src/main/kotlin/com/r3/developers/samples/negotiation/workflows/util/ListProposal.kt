package com.r3.developers.samples.negotiation.workflows.util

import com.r3.developers.samples.negotiation.Proposal
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.ledger.utxo.StateAndRef
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.util.stream.Collectors

class ListProposal : ClientStartableFlow {
    // Injects the JsonMarshallingService to read and populate JSON parameters.
    @CordaInject
    var jsonMarshallingService: JsonMarshallingService? = null

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    @CordaInject
    var utxoLedgerService: UtxoLedgerService? = null
    override fun call(requestBody: ClientRequestBody): String {
        // Queries the VNode's vault for unconsumed states and converts the result to a serializable DTO.
        val states = utxoLedgerService!!.findUnconsumedStatesByType(
            Proposal::class.java
        )
        val results = states.stream().map { stateAndRef: StateAndRef<Proposal> ->
            ListProposalArgs(
                stateAndRef.state.contractState.proposalID,
                stateAndRef.state.contractState.amount,
                stateAndRef.state.contractState.buyer.toString(),
                stateAndRef.state.contractState.seller.toString(),
                stateAndRef.state.contractState.proposer.toString(),
                stateAndRef.state.contractState.proposee.toString()
            )
        }.collect(Collectors.toList())

        // Uses the JsonMarshallingService's format() function to serialize the DTO to Json.
        return jsonMarshallingService!!.format(results)
    }
}
