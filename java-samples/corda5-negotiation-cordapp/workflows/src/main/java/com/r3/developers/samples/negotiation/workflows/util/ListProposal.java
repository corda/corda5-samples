package com.r3.developers.samples.negotiation.workflows.util;

import com.r3.developers.samples.negotiation.contracts.Proposal;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.ClientStartableFlow;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.ledger.utxo.StateAndRef;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class ListProposal implements ClientStartableFlow {

    // Injects the JsonMarshallingService to read and populate JSON parameters.
    @CordaInject
    public JsonMarshallingService jsonMarshallingService;

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    @CordaInject
    public UtxoLedgerService utxoLedgerService;

    @NotNull
    @Override
    public String call(@NotNull ClientRequestBody requestBody) {
        // Queries the VNode's vault for unconsumed states and converts the result to a serializable DTO.
        List<StateAndRef<Proposal>> states = utxoLedgerService.findUnconsumedStatesByType(Proposal.class);
        List<ListProposalArgs> results = states.stream().map(stateAndRef ->
                new ListProposalArgs(
                        stateAndRef.getState().getContractState().getProposalID(),
                        stateAndRef.getState().getContractState().getAmount(),
                        stateAndRef.getState().getContractState().getBuyer().toString(),
                        stateAndRef.getState().getContractState().getSeller().toString(),
                        stateAndRef.getState().getContractState().getProposer().toString(),
                        stateAndRef.getState().getContractState().getProposee().toString()

                )
        ).collect(Collectors.toList());

        // Uses the JsonMarshallingService's format() function to serialize the DTO to Json.
        return jsonMarshallingService.format(results);

    }
}
