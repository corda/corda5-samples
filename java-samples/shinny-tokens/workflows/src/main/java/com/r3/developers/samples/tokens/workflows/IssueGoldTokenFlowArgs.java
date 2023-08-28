package com.r3.developers.samples.tokens.workflows;

// A class to hold the deserialized arguments required to start the flow.
public class IssueGoldTokenFlowArgs {

    // Serialisation service requires a default constructor
    public IssueGoldTokenFlowArgs() {}

    private String symbol;
    private String amount;
    private String owner;

    public IssueGoldTokenFlowArgs(String symbol, String amount, String owner) {
        this.symbol = symbol;
        this.amount = amount;
        this.owner = owner;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getAmount() {
        return amount;
    }

    public String getOwner() {
        return owner;
    }
}