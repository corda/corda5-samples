package com.r3.developers.advanceCustomQuery.listUtil;

import com.r3.developers.advanceCustomQuery.states.ChatState;
import net.corda.v5.application.flows.ClientRequestBody;
import net.corda.v5.application.flows.ClientStartableFlow;
import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.application.persistence.PagedQuery;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.ledger.utxo.StateAndRef;
import net.corda.v5.ledger.utxo.UtxoLedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

// See Chat CorDapp Design section of the getting started docs for a description of this flow.
public class ListAllMsgContentsIncludingHello implements ClientStartableFlow {

    private final static Logger log = LoggerFactory.getLogger(ListChatsFlow.class);

    @CordaInject
    public JsonMarshallingService jsonMarshallingService;

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    @CordaInject
    public UtxoLedgerService utxoLedgerService;

    @Suspendable
    @Override
    public String call(ClientRequestBody requestBody) {

        log.info("ListChatsByCustomQueryFlow.call() called");

        // Queries the VNode's vault for unconsumed states and converts the result to a serializable DTO.
        PagedQuery.ResultSet<String> resultSet = utxoLedgerService.query("GET_ALL_MSGS_CONTENT_HAS_HELLO", String.class)
                .setCreatedTimestampLimit(Instant.now()).setLimit(1000)
                .execute();

        // Uses the JsonMarshallingService's format() function to serialize the DTO to Json.
        return jsonMarshallingService.format(resultSet.getResults().toString());
    }
}

/*
RequestBody for triggering the flow via REST:
{
    "clientRequestId": "ListAllMsgContentsIncludingHello-1",
    "flowClassName": "com.r3.developers.advanceCustomQuery.listUtil.ListAllMsgContentsIncludingHello",
    "requestBody": {}
}
*/