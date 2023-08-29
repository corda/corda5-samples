package com.r3.developers.samples.tokens.states;

import com.r3.developers.samples.tokens.contracts.GoldContract;
import net.corda.v5.crypto.SecureHash;
import net.corda.v5.ledger.utxo.BelongsToContract;
import net.corda.v5.ledger.utxo.ContractState;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.security.PublicKey;
import java.util.List;

@BelongsToContract(GoldContract.class)
public class GoldState implements ContractState {

    private SecureHash issuer;
    private SecureHash owner;
    private String symbol;
    private BigDecimal amount;
    public List<PublicKey> participants;

    public GoldState(SecureHash issuer, SecureHash owner, String symbol, BigDecimal amount, List<PublicKey> participants) {
        this.issuer = issuer;
        this.owner = owner;
        this.symbol = symbol;
        this.amount = amount;
        this.participants = participants;
    }

    public SecureHash getIssuer() {
        return issuer;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public SecureHash getOwner() {
        return owner;
    }

    @NotNull
    @Override
    public List<PublicKey> getParticipants() {
        return participants;
    }

}
