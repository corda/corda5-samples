package com.r3.developers.csdetemplate.utxoexample.states;

import com.r3.developers.csdetemplate.utxoexample.contracts.TokenContract;
import net.corda.v5.base.annotations.ConstructorForDeserialization;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.utxo.BelongsToContract;
import net.corda.v5.ledger.utxo.ContractState;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.List;

@BelongsToContract(TokenContract.class)
public class TokenState implements ContractState {

    public final MemberX500Name issuer;
    public final MemberX500Name owner;
    public final int amount;
    public List<PublicKey> participants;

    @ConstructorForDeserialization
    public TokenState(int amount, MemberX500Name issuer, MemberX500Name owner, List<PublicKey> participants) {
        this.amount = amount;
        this.issuer = issuer;
        this.owner = owner;
        this.participants = participants;
    }

    public int getAmount() {
        return amount;
    }

    public MemberX500Name getIssuer() {
        return issuer;
    }

    public MemberX500Name getOwner() {
        return owner;
    }

    @NotNull
    @Override
    public List<PublicKey> getParticipants() {
        return participants;
    }

}
