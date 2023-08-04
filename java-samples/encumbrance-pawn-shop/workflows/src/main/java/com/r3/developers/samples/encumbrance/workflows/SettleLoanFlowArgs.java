package com.r3.developers.samples.encumbrance.workflows;

public class SettleLoanFlowArgs {
    private String loanId;

    public SettleLoanFlowArgs() {
    }

    public SettleLoanFlowArgs(String loanId) {
        this.loanId = loanId;
    }

    public String getLoanId() {
        return loanId;
    }

    public void setLoanId(String loanId) {
        this.loanId = loanId;
    }
}
