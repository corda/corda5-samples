package com.r3.developers.samples.obligation.states;


import com.r3.developers.samples.obligation.contracts.ProposalAndTradeContract;
import net.corda.v5.base.annotations.ConstructorForDeserialization;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.utxo.BelongsToContract;
import net.corda.v5.ledger.utxo.ContractState;
import org.jetbrains.annotations.NotNull;


import java.security.PublicKey;
import java.util.*;

@BelongsToContract(ProposalAndTradeContract.class)
public class Proposal implements ContractState {

    private  int amount;
    private  Member buyer;
    private  Member seller;
    private  Member proposer;
    private  Member proposee;
    private final UUID linearId;


    @ConstructorForDeserialization
    public Proposal(int amount, Member buyer, Member seller, Member proposer, Member proposee, UUID linearId) {
        this.amount=amount;
        this.buyer= buyer;
        this.seller=seller;
        this.proposee= proposee;
        this.proposer=proposer;
        this.linearId=linearId;
    }

    public Proposal(int amount, Member buyer, Member seller, Member proposer, Member proposee) {
        this.amount=amount;
        this.buyer= buyer;
        this.seller=seller;
        this.proposee= proposee;
        this.proposer=proposer;
        this.linearId=UUID.randomUUID();
    }

    public int getAmount(){return amount;}

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

    public UUID getLinearId() {
        return linearId;
    }

    @NotNull
    @Override
    public List<PublicKey> getParticipants() {
        return  List.of(proposer.getLedgerKey(), proposee.getLedgerKey());
    }
}

