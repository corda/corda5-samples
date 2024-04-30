package com.r3.developers.cordapptemplate.customJSON.workflows

import com.r3.developers.cordapptemplate.customJSON.states.ChatState
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.utxo.StateAndRef
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.time.Instant

// See Chat CorDapp Design section of the getting started docs for a description of this flow.
class ListChatsByCustomQueryFlow : ClientStartableFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {

        log.info("ListChatsByCustomQueryFlow.call() called")

        //this is our custom query
        val resultSet = ledgerService.query("GET_MSG_FROM", StateAndRef::class.java)
            .setParameter("nameOfSender", "CN=Alice, OU=Test Dept, O=R3, L=London, C=GB")
            .setCreatedTimestampLimit(Instant.now()).setLimit(1000)
            .execute()

        //from custom query to states, we can operate
        val resultState = resultSet.results.map {it.state.contractState as ChatState}

        //from the states -> human-readable.
        val results = resultState.map {
            ChatStateResults(
                it.id,
                it.chatName,
                it.messageFrom.toString(),
                it.message) }
        log.info("-------------results will be printed------")
        log.warn(results.toString())

        // Uses the JsonMarshallingService's format() function to serialize the DTO to Json.
        return jsonMarshallingService.format(results.toString())
    }
}



/*
RequestBody for triggering the flow via REST:
{
    "clientRequestId": "customlist-1",
    "flowClassName": "com.r3.developers.cordapptemplate.utxoexample.workflows.ListChatsByCustomQueryFlow",
    "requestBody": {}
}
*/
