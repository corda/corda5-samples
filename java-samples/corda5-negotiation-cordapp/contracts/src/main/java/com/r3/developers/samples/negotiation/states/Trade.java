package com.r3.developers.samples.negotiation.states;


import com.r3.developers.samples.negotiation.contracts.ProposalAndTradeContract;
import net.corda.v5.base.annotations.ConstructorForDeserialization;
import net.corda.v5.ledger.utxo.BelongsToContract;
import net.corda.v5.ledger.utxo.ContractState;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.List;
import java.util.UUID;


@BelongsToContract(ProposalAndTradeContract.class)
public class Trade implements ContractState {

    private int amount;
    private Member buyer;
    private Member seller;
    private UUID proposalID;
    public List<PublicKey> participants;


    @ConstructorForDeserialization
    public Trade(int amount, Member buyer, Member seller, UUID proposalID, List<PublicKey> participants) {
        this.amount = amount;
        this.buyer = buyer;
        this.seller = seller;
        this.proposalID = proposalID;
        this.participants = participants;
    }

    public Trade(int amount, Member buyer, Member seller, List<PublicKey> participants) {
        this.amount = amount;
        this.buyer = buyer;
        this.seller = seller;
        this.proposalID = UUID.randomUUID();
        this.participants = participants;
    }


    public int getAmount(){return amount;}

    public Member getSeller() {
        return seller;
    }

    public Member getBuyer() {
        return buyer;
    }

    public UUID getProposalID() {
        return proposalID;
    }

    @NotNull
    @Override
    public List<PublicKey> getParticipants() {
        return participants;
    }

}