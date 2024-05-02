package com.r3.developers.samples.obligation.workflows;

import com.r3.developers.samples.obligation.states.IOUState;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.ClientStartableFlow;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.ledger.utxo.StateAndRef;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class ListIOUFlow implements ClientStartableFlow {

    private final static Logger log = LoggerFactory.getLogger(ListIOUFlow.class);

    // Injects the JsonMarshallingService to read and populate JSON parameters.
    @CordaInject
    public JsonMarshallingService jsonMarshallingService;

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    @CordaInject
    public UtxoLedgerService utxoLedgerService;

    @Suspendable
    @Override
    public String call(ClientRequestBody requestBody) {

        log.info("ListIOUFlow.call() called");

        // Queries the VNode's vault for unconsumed states and converts the result to a serializable DTO.
        List<StateAndRef<IOUState>> states = utxoLedgerService.findUnconsumedStatesByExactType(IOUState.class,100, Instant.now()).getResults();
        List<ListIOUFlowResults> results = states.stream().map(stateAndRef ->
                new ListIOUFlowResults(
                        stateAndRef.getState().getContractState().getLinearId(),
                        stateAndRef.getState().getContractState().getAmount(),
                        stateAndRef.getState().getContractState().getBorrower().toString(),
                        stateAndRef.getState().getContractState().getLender().toString(),
                        stateAndRef.getState().getContractState().getPaid()
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
    "flowClassName": "com.r3.developers.samples.obligation.workflows.ListIOUFlow",
    "requestBody": {}
}
*/