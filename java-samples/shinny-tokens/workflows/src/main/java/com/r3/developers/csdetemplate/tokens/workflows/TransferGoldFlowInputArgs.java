package com.r3.developers.csdetemplate.tokens.workflows;

// A class to hold the deserialized arguments required to start the flow.
public class TransferGoldFlowInputArgs {

    // Serialisation service requires a default constructor
    public TransferGoldFlowInputArgs() {}

    private String symbol;
    private String issuer;
    private String value;
    private String newOwner;

    public TransferGoldFlowInputArgs(String symbol, String issuer, String value, String newOwner) {
        this.symbol = symbol;
        this.issuer = issuer;
        this.value = value;
        this.newOwner = newOwner;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getValue() {
        return value;
    }

    public String getNewOwner() {
        return newOwner;
    }
}