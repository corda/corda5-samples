package com.r3.developers.samples.referencestate.workflows

import com.r3.developers.samples.referencestate.states.SanctionList
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.util.*

data class SanctionListDetail(val badPeople:List<MemberX500Name>,
                             val  issuer:MemberX500Name,
                             val  uniqueId: UUID)

class GetSanctionListFlow : ClientStartableFlow {

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
        log.info("GetSanctionListFlow.call() called")

        // Queries the VNode's vault for unconsumed states and converts the result to a serializable DTO.
        val states = ledgerService.findUnconsumedStatesByType(SanctionList::class.java)
        val results = states.map {it ->
            SanctionListDetail(
                it.state.contractState.badPeople.map { it.name }.toList(),
                it.state.contractState.issuer.name,
                it.state.contractState.uniqueId
            )
        }

        // Uses the JsonMarshallingService's format() function to serialize the DTO to Json.
        return jsonMarshallingService.format(results)
    }

}