package com.r3.developers.samples.negotiation

import com.r3.developers.samples.negotiation.util.Member
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey
import java.util.*

@BelongsToContract(ProposalAndTradeContract::class)
class Proposal(
    var amount: Int,

    var buyer: Member,

    var seller: Member,

    var proposer: Member,

    var proposee: Member,

    val modifier: Member?,

    val proposalID: UUID,


    ) : ContractState {
    override fun getParticipants(): List<PublicKey> {
        return listOf(proposer.ledgerKey, proposee.ledgerKey)
    }
}

