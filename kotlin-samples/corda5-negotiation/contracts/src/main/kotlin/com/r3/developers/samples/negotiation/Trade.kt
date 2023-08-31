package com.r3.developers.samples.negotiation

import com.r3.developers.samples.negotiation.util.Member
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey
import java.util.*

@BelongsToContract(ProposalAndTradeContract::class)
class Trade(
    var amount: Int,

    var buyer: Member,

    var seller: Member,

    var acceptor: MemberX500Name,

    var tradelId: UUID,

    private val participants: List<PublicKey>
) : ContractState {

    override fun getParticipants(): List<PublicKey> {
        return participants
    }
}