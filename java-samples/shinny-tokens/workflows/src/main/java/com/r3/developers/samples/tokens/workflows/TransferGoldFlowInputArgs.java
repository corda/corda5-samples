package com.r3.developers.samples.tokens.workflows;

// A class to hold the deserialized arguments required to start the flow.
public class TransferGoldFlowInputArgs {

    // Serialisation service requires a default constructor
    public TransferGoldFlowInputArgs() {}

    private String symbol;
    private String issuer;
    private String amount;
    private String receiver;

    public TransferGoldFlowInputArgs(String symbol, String issuer, String amount, String receiver) {
        this.symbol = symbol;
        this.issuer = issuer;
        this.amount = amount;
        this.receiver = receiver;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getAmount() {
        return amount;
    }

    public String getReceiver() {
        return receiver;
    }
}