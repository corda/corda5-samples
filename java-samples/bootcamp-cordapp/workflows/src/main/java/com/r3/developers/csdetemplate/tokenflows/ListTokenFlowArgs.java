package com.r3.developers.csdetemplate.tokenflows;

public class ListTokenFlowArgs {


    private String issuer;
    private String owner;
    private int amount;

    public ListTokenFlowArgs() {
    }

    public ListTokenFlowArgs(String issuer, String owner, int amount) {
        this.issuer = issuer;
        this.owner = owner;
        this.amount = amount;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getOwner() {
        return owner;
    }

    public int getAmount() {
        return amount;
    }
}
