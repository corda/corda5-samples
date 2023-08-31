package com.r3.developers.samples.negotiation;

import com.r3.developers.samples.negotiation.util.Member;
import net.corda.v5.base.annotations.ConstructorForDeserialization;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.utxo.BelongsToContract;
import net.corda.v5.ledger.utxo.ContractState;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.List;
import java.util.UUID;


@BelongsToContract(ProposalAndTradeContract.class)
public class Trade implements ContractState {

    private final int amount;
    private final Member buyer;
    private final Member seller;
    private final UUID proposalID;
    private final MemberX500Name acceptor;
    public List<PublicKey> participants;


    @ConstructorForDeserialization
    public Trade(int amount, Member buyer, Member seller, UUID proposalID, MemberX500Name acceptor, List<PublicKey> participants) {
        this.amount = amount;
        this.buyer = buyer;
        this.seller = seller;
        this.proposalID = proposalID;
        this.acceptor = acceptor;
        this.participants = participants;
    }

    public Trade(int amount, Member buyer, Member seller, MemberX500Name acceptor, List<PublicKey> participants) {
        this.amount = amount;
        this.buyer = buyer;
        this.seller = seller;
        this.acceptor = acceptor;
        this.proposalID = UUID.randomUUID();
        this.participants = participants;
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

    public UUID getProposalID() {
        return proposalID;
    }
    public MemberX500Name getAcceptor() {
        return acceptor;
    }

    @NotNull
    @Override
    public List<PublicKey> getParticipants() {
        return participants;
    }

}