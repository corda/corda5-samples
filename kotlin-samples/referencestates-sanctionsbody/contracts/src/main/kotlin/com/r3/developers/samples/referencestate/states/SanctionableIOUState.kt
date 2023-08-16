package com.r3.developers.samples.referencestate.states

import com.r3.developers.samples.referencestate.contracts.SanctionableIOUContract
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey
import java.util.*

@BelongsToContract(SanctionableIOUContract::class)
class SanctionableIOUState(val value: Int,
                           val lender: Member,
                           val borrower: Member,
                           val uniqueIdentifier: UUID =  UUID.randomUUID(),
) : ContractState {

    override fun getParticipants(): List<PublicKey> {
        return listOf(lender.ledgerKey,borrower.ledgerKey)
    }
}