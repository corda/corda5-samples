package com.r3.developers.csdetemplate.tokenflows;

import com.r3.developers.csdetemplate.utxoexample.states.TokenState;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.ClientStartableFlow;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.ledger.utxo.StateAndRef;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class ListTokenFlow implements ClientStartableFlow {

    private final static Logger log = LoggerFactory.getLogger(ListTokenFlow.class);

    @CordaInject
    public JsonMarshallingService jsonMarshallingService;

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    @CordaInject
    public UtxoLedgerService utxoLedgerService;

    @Suspendable
    @Override
    public String call(ClientRequestBody requestBody) {

        log.info("ListTokenFlow.call() called");

        // Queries the VNode's vault for unconsumed states and converts the result to a serializable DTO.
        List<StateAndRef<TokenState>> states = utxoLedgerService.findUnconsumedStatesByType(TokenState.class);
        List<ListTokenFlowArgs> results = states.stream().map(stateAndRef ->
                new ListTokenFlowArgs(
                        stateAndRef.getState().getContractState().getIssuer().toString(),
                        stateAndRef.getState().getContractState().getOwner().toString(),
                        stateAndRef.getState().getContractState().getAmount()
                )
        ).collect(Collectors.toList());

        // Uses the JsonMarshallingService's format() function to serialize the DTO to Json.
        return jsonMarshallingService.format(results);
    }
}
/*
RequestBody for triggering the flow via http-rpc:
{
    "clientRequestId": "list-1",
    "flowClassName": "com.r3.developers.csdetemplate.tokenflows.ListTokenFlow",
    "requestBody": {}
}
*/