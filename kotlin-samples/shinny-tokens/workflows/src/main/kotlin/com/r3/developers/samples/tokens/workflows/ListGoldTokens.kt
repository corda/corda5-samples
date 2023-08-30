package com.r3.developers.samples.tokens.workflows

import com.r3.developers.samples.tokens.states.GoldState
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.crypto.SecureHash
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.util.stream.Collectors

// This flow is used to list all the gold tokens available in the vault.
class ListGoldTokens: ClientStartableFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    // Injects the UtxoLedgerService to enable the flow to make use of the Ledger API.
    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        val states = utxoLedgerService.findUnconsumedStatesByType(
            GoldState::class.java
        )

        // Queries the VNode's vault for unconsumed states and converts the result to a serializable DTO.
        val results = states.stream().map{
            GoldStateList(
                it.state.contractState.issuer,
                it.state.contractState.symbol,
                it.state.contractState.amount,
                it.state.contractState.owner
            )
        }.collect(Collectors.toList())
        // Uses the JsonMarshallingService's format() function to serialize the DTO to Json.
        return jsonMarshallingService.format(results)
    }
}

/*
RequestBody for triggering the flow via http-rpc:
{
    "clientRequestId": "list-1",
    "flowClassName": "com.r3.developers.samples.tokens.workflows.ListGoldTokens",
    "requestBody": {}
}
*/


data class GoldStateList(
    val issuer: SecureHash,
    val symbol: String,
    val amount: BigDecimal,
    val owner: SecureHash
)