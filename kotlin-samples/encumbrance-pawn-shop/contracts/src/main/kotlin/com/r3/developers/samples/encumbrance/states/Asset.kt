package com.r3.developers.samples.encumbrance.states

import com.r3.developers.samples.encumbrance.contracts.AssetContract
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey

@BelongsToContract(AssetContract::class)
data class Asset (
    val owner : Member,
    val assetName: String,
    val assetId: String,
    private val participants: List<PublicKey>) : ContractState {

    override fun getParticipants(): List<PublicKey> {
        return participants
    }
}