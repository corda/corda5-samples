package com.r3.developers.samples.referencestate.states

import com.r3.developers.samples.referencestate.contracts.SanctionListContract
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey
import java.util.*

@BelongsToContract(SanctionListContract::class)
class SanctionList(val badPeople: List<Member>,
                   val issuer: Member,
                   val uniqueId: UUID = UUID.randomUUID(),
                   private val participants: List<PublicKey>
): ContractState {
    
    override fun getParticipants(): List<PublicKey> {
        return this.participants
    }
}