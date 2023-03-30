package com.r3.developers.samples.obligation.states

import com.r3.developers.samples.obligation.contracts.IOUContract
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey
import java.util.*

//Link with the Contract class
@BelongsToContract(IOUContract::class)
data class IOUState (
    //private variables
    val amount: Int,
    val lender: MemberX500Name,
    val borrower: MemberX500Name,
    val paid: Int,
    val linearId: UUID,
    private val participants: List<PublicKey>
) : ContractState {

    //Helper method for settle flow
    fun pay(amountToPay: Int) : IOUState {
        return IOUState(amount,lender,borrower,paid+amountToPay,linearId,participants)
    }

    //Helper method for transfer flow
    fun withNewLender(newLender:MemberX500Name, newParticipants:List<PublicKey> ): IOUState {
        return IOUState(amount,newLender,borrower,paid,linearId,newParticipants)
    }

    override fun getParticipants(): List<PublicKey> {
        return participants
    }
}