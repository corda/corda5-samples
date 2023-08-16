package com.r3.developers.samples.encumbrance.workflows;

public class RequestLoanFlowArgs {

    private String lender;
    private int loanAmount;
    private String collateral;

    public RequestLoanFlowArgs() {
    }

    public RequestLoanFlowArgs(String lender, int loanAmount, String collateral) {
        this.lender = lender;
        this.loanAmount = loanAmount;
        this.collateral = collateral;
    }

    public String getLender() {
        return lender;
    }

    public void setLender(String lender) {
        this.lender = lender;
    }

    public int getLoanAmount() {
        return loanAmount;
    }

    public void setLoanAmount(int loanAmount) {
        this.loanAmount = loanAmount;
    }

    public String getCollateral() {
        return collateral;
    }

    public void setCollateral(String collateral) {
        this.collateral = collateral;
    }
}
