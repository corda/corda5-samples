package com.r3.developers.samples.obligation.workflows;

// A class to hold the deserialized arguments required to start the flow.
public class IOUIssueFlowArgs {
    private  String amount;
    private  String lender;

    public IOUIssueFlowArgs() {
    }

    public IOUIssueFlowArgs(String amount, String lender) {
        this.amount = amount;
        this.lender = lender;
    }

    public String getAmount() {
        return amount;
    }

    public String getLender() {
        return lender;
    }
}
