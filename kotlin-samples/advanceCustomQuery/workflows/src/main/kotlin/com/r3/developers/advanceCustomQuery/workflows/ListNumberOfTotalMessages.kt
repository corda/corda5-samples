package com.r3.developers.advanceCustomQuery.workflows

import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.time.Instant

// See Chat CorDapp Design section of the getting started docs for a description of this flow.
class ListNumberOfTotalMessages : ClientStartableFlow {

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

        log.info("ListNumberOfTotalMessages.call() called")

        //this is our custom query
        val resultSet = ledgerService.query("GET_MSG_AMOUNT", Integer::class.java)
            .setCreatedTimestampLimit(Instant.now()).setLimit(1000)
            .execute()


        log.info("-------------results will be printed------")
        log.warn(resultSet.toString())

        // Uses the JsonMarshallingService's format() function to serialize the DTO to Json.
        return jsonMarshallingService.format(resultSet.results.toString()) //use .results to get the results of the query
    }
}



/*
RequestBody for triggering the flow via REST:
{
    "clientRequestId": "ListNumberOfTotalMessages-1",
    "flowClassName": "com.r3.developers.advanceCustomQuery.workflows.ListNumberOfTotalMessages",
    "requestBody": {}
}
*/
