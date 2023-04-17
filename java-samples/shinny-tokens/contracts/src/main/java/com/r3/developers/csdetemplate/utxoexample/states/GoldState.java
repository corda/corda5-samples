package com.r3.developers.csdetemplate.utxoexample.states;

import com.r3.developers.csdetemplate.utxoexample.contracts.GoldContract;
import net.corda.v5.crypto.SecureHash;
import net.corda.v5.ledger.utxo.BelongsToContract;
import net.corda.v5.ledger.utxo.ContractState;

import java.math.BigDecimal;
import java.security.PublicKey;
import java.util.List;

@BelongsToContract(GoldContract.class)
public class GoldState implements ContractState {

    private SecureHash issuer;
    private String symbol;
    private BigDecimal value;
    private SecureHash owner;
    public List<PublicKey> participants;

    public GoldState(SecureHash issuer, String symbol, BigDecimal value, List<PublicKey> participants, SecureHash owner) {
        this.issuer = issuer;
        this.symbol = symbol;
        this.value = value;
        this.participants = participants;
        this.owner = owner;
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

    @Override
    public List<PublicKey> getParticipants() {
        return participants;
    }





}
