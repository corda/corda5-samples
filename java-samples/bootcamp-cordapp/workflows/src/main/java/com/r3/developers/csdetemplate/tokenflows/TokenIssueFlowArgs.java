package com.r3.developers.csdetemplate.tokenflows;

// A class to hold the deserialized arguments required to start the flow.
public class TokenIssueFlowArgs {
    private  String amount;
    private  String owner;

    public TokenIssueFlowArgs() {
    }

    public TokenIssueFlowArgs(String amount, String owner) {
        this.amount = amount;
        this.owner = owner;
    }

    public String getAmount() {
        return amount;
    }

    public String getOwner() {
        return owner;
    }
}
