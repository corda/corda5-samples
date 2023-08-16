package com.r3.developers.samples.encumbrance.workflows

import com.r3.developers.samples.encumbrance.states.Asset
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory

data class AssetDetail(
    val owner: MemberX500Name,
    val assetId: String,
    val assetName: String,
    val encumbranceGroupTag: String?,
    val encumbranceGroupSize: Int?
)

class GetAssetFlow : ClientStartableFlow {

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

        log.info("GetAssetFlow.call() called")

        // Queries the VNode's vault for unconsumed states and converts the result to a serializable DTO.
        val states = ledgerService.findUnconsumedStatesByType(Asset::class.java)
        val results = states.map {it ->
            AssetDetail(
                it.state.contractState.owner.name,
                it.state.contractState.assetId,
                it.state.contractState.assetName,
                it.state.encumbranceGroup?.tag,
                it.state.encumbranceGroup?.size
            )
        }

        // Uses the JsonMarshallingService's format() function to serialize the DTO to Json.
        return jsonMarshallingService.format(results)
    }
}
/*
  {
     "clientRequestId": "get-asset",
     "flowClassName": "com.r3.developers.samples.encumbrance.workflows.GetAssetFlow",
     "requestBody": {
     }
  }
 */