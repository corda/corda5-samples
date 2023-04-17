package com.r3.developers.csdetemplate.tokens.workflows;

// A class to hold the deserialized arguments required to start the flow.
public class MintGoldFlowInputArgs {

    // Serialisation service requires a default constructor
    public MintGoldFlowInputArgs() {}

    private String symbol;
    private String issuer;
    private String value;

    public MintGoldFlowInputArgs(String symbol, String issuer, String value) {
        this.symbol = symbol;
        this.issuer = issuer;
        this.value = value;
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
}