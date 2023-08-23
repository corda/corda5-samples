package com.r3.developers.samples.negotiation;

import com.r3.developers.samples.negotiation.util.Member;
import net.corda.v5.base.annotations.ConstructorForDeserialization;
import net.corda.v5.ledger.utxo.BelongsToContract;
import net.corda.v5.ledger.utxo.ContractState;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.List;
import java.util.UUID;

@BelongsToContract(ProposalAndTradeContract.class)
public class Proposal implements ContractState {

    private final int amount;
    private final Member buyer;
    private final Member seller;
    private final Member proposer;
    private final Member proposee;
    private final UUID proposalID;

    @ConstructorForDeserialization
    public Proposal(int amount, Member buyer, Member seller, Member proposer, Member proposee, UUID proposalID) {
        this.amount = amount;
        this.buyer = buyer;
        this.seller = seller;
        this.proposee = proposee;
        this.proposer = proposer;
        this.proposalID = proposalID;
    }

    public Proposal(int amount, Member buyer, Member seller, Member proposer, Member proposee) {
        this.amount = amount;
        this.buyer = buyer;
        this.seller = seller;
        this.proposee = proposee;
        this.proposer = proposer;
        this.proposalID = UUID.randomUUID();
    }

    public int getAmount() {
        return amount;
    }

    public Member getSeller() {
        return seller;
    }

    public Member getBuyer() {
        return buyer;
    }

    public Member getProposer() {
        return proposer;
    }

    public Member getProposee() {
        return proposee;
    }

    public UUID getProposalID() {
        return proposalID;
    }

    @NotNull
    @Override
    public List<PublicKey> getParticipants() {
        return List.of(proposer.getLedgerKey(), proposee.getLedgerKey());
    }
}

