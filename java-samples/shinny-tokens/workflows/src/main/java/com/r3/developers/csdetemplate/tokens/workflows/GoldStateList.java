package com.r3.developers.csdetemplate.tokens.workflows;

import net.corda.v5.crypto.SecureHash;

import java.math.BigDecimal;

public class GoldStateList {

    private SecureHash issuer;
    private String symbol;
    private BigDecimal value;
    private SecureHash owner;


    public GoldStateList(SecureHash issuer, String symbol, BigDecimal value, SecureHash owner) {
        this.issuer = issuer;
        this.symbol = symbol;
        this.value = value;
        this.owner = owner;
    }

    public GoldStateList() {
    }

    public SecureHash getIssuer() {
        return issuer;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getValue() {
        return value;
    }

    public SecureHash getOwner() {
        return owner;
    }
}
