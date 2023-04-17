package com.r3.developers.csdetemplate.tokens.workflows;

import com.r3.developers.csdetemplate.utxoexample.states.GoldState;
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

public class ListGoldTokens implements ClientStartableFlow {

    private final static Logger log = LoggerFactory.getLogger(ListGoldTokens.class);

    @CordaInject
    public JsonMarshallingService jsonMarshallingService;

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    @CordaInject
    public UtxoLedgerService utxoLedgerService;

    @Suspendable
    @Override
    public String call(ClientRequestBody requestBody) {
        List<StateAndRef<GoldState>> states = utxoLedgerService.findUnconsumedStatesByType(GoldState.class);

        List<GoldStateList> results = states.stream().map(stateAndRef ->
                new GoldStateList(
                        stateAndRef.getState().getContractState().getIssuer(),
                        stateAndRef.getState().getContractState().getSymbol(),
                        stateAndRef.getState().getContractState().getValue(),
                        stateAndRef.getState().getContractState().getOwner()
                        )
        ).collect(Collectors.toList());

        return jsonMarshallingService.format(results);
    }
}

/*
RequestBody for triggering the flow via http-rpc:
{
    "clientRequestId": "list-1",
    "flowClassName": "com.r3.developers.csdetemplate.tokens.workflows.ListGoldTokens",
    "requestBody": {}
}
*/