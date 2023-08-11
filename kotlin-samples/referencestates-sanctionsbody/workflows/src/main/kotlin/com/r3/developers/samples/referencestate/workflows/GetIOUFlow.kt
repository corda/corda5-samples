package com.r3.developers.samples.referencestate.workflows

import com.r3.developers.samples.referencestate.states.SanctionableIOUState
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.util.*

data class IOU(val value: Int,
               val lender: MemberX500Name,
               val borrower:MemberX500Name,
               val uniqueIdentifier: UUID)


class GetIOUFlow : ClientStartableFlow {

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
        log.info("GetIOUFlow.call() called")

        // Queries the VNode's vault for unconsumed states and converts the result to a serializable DTO.
        val states = ledgerService.findUnconsumedStatesByType(SanctionableIOUState::class.java)
        val results = states.map {it ->
            IOU(
                it.state.contractState.value,
                it.state.contractState.lender.name,
                it.state.contractState.borrower.name,
                it.state.contractState.uniqueIdentifier
            )
        }

        // Uses the JsonMarshallingService's format() function to serialize the DTO to Json.
        return jsonMarshallingService.format(results)
    }
}