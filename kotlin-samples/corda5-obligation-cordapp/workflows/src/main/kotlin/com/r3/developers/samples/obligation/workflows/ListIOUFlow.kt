package com.r3.developers.samples.obligation.workflows

import com.r3.developers.samples.obligation.states.IOUState
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.util.*


data class ListIOUFlowResults(val id: UUID,val amount: Int,val borrower: String,val lender: String,val paid: Int)


class ListIOUFlow: ClientStartableFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }
    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService
    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        log.info("IOUSettleFlow.call() called")

        val states = ledgerService.findUnconsumedStatesByType(IOUState::class.java)
        val results = states.map { stateAndRef ->
            ListIOUFlowResults(
                stateAndRef.state.contractState.linearId,
                stateAndRef.state.contractState.amount,
                stateAndRef.state.contractState.borrower.toString(),
                stateAndRef.state.contractState.lender.toString(),
                stateAndRef.state.contractState.paid,
            )
        }
        return jsonMarshallingService.format(results)
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