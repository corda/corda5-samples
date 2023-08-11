package com.r3.developers.samples.encumbrance.states

import com.r3.developers.samples.encumbrance.contracts.LoanContract
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey

@BelongsToContract(LoanContract::class)
class Loan(
    val loanId: String,
    val lender: Member,
    val borrower: Member,
    val loanAmount: Int,
    val collateral: String,
): ContractState {
    override fun getParticipants(): List<PublicKey> {
        return listOf(lender.ledgerKey,borrower.ledgerKey)
    }
}