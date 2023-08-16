package com.r3.developers.samples.encumbrance.states;

import com.r3.developers.samples.encumbrance.contracts.LoanContract;
import net.corda.v5.ledger.utxo.BelongsToContract;
import net.corda.v5.ledger.utxo.ContractState;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

/**
 * This state represents the loan which will be issued to the borrower. It contains a collateral which would be
 * locked using Corda encumbrance feature till the loan has been repaid.
 */
@BelongsToContract(LoanContract.class)
public class Loan implements ContractState {

    private final String loanId;
    private final Member lender;
    private final Member borrower;
    private final int loanAmount;
    private final String collateral;

    public Loan(String loanId, Member lender, Member borrower, int loanAmount, String collateral) {
        this.loanId = loanId;
        this.lender = lender;
        this.borrower = borrower;
        this.loanAmount = loanAmount;
        this.collateral = collateral;
    }

    public String getLoanId() {
        return loanId;
    }

    public Member getLender() {
        return lender;
    }

    public Member getBorrower() {
        return borrower;
    }

    public int getLoanAmount() {
        return loanAmount;
    }

    public String getCollateral() {
        return collateral;
    }

    @NotNull
    @Override
    public List<PublicKey> getParticipants() {
        return Arrays.asList(lender.getLedgerKey(), borrower.getLedgerKey());
    }
}
