package com.r3.developers.samples.negotiation.workflows.util;

import com.r3.developers.samples.negotiation.Trade;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.ClientStartableFlow;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.ledger.utxo.StateAndRef;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class ListTrade implements ClientStartableFlow {

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
        List<StateAndRef<Trade>> states = utxoLedgerService.findUnconsumedStatesByExactType(Trade.class, 100, Instant.now()).getResults();
        List<ListTradeArgs> results = states.stream().map(stateAndRef ->
                new ListTradeArgs(
                        stateAndRef.getState().getContractState().getProposalID(),
                        stateAndRef.getState().getContractState().getAmount(),
                        stateAndRef.getState().getContractState().getBuyer().toString(),
                        stateAndRef.getState().getContractState().getSeller().toString()

                )
        ).collect(Collectors.toList());

        // Uses the JsonMarshallingService's format() function to serialize the DTO to Json.
        return jsonMarshallingService.format(results);

    }
}
