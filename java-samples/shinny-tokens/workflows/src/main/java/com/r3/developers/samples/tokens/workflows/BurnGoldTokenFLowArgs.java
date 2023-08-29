package com.r3.developers.samples.tokens.workflows;

public class BurnGoldTokenFLowArgs {

    public BurnGoldTokenFLowArgs() {
    }

    private String symbol;
    private String issuer;
    private String amount;

    public BurnGoldTokenFLowArgs(String symbol, String issuer, String amount) {
        this.symbol = symbol;
        this.issuer = issuer;
        this.amount = amount;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }
}
