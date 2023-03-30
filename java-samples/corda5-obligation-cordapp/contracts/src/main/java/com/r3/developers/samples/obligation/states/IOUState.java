package com.r3.developers.samples.obligation.states;

import com.r3.developers.samples.obligation.contracts.IOUContract;
import net.corda.v5.base.annotations.ConstructorForDeserialization;
import net.corda.v5.base.types.MemberX500Name;
import net.corda.v5.ledger.utxo.BelongsToContract;
import net.corda.v5.ledger.utxo.ContractState;

import java.security.PublicKey;
import java.util.List;
import java.util.UUID;

//Link with the Contract class
@BelongsToContract(IOUContract.class)
public class IOUState implements ContractState {

    //private variables
    public final int amount;
    public final MemberX500Name lender;
    public final MemberX500Name borrower;
    public final int paid;
    private final UUID linearId;
    public List<PublicKey> participants;


    @ConstructorForDeserialization
    public IOUState(int amount, MemberX500Name lender, MemberX500Name borrower, int paid, UUID linearId, List<PublicKey> participants) {
        this.amount = amount;
        this.lender = lender;
        this.borrower = borrower;
        this.paid = paid;
        this.linearId = linearId;
        this.participants = participants;
    }

    public IOUState(int amount, MemberX500Name lender, MemberX500Name borrower, List<PublicKey> participants) {
        this.amount = amount;
        this.lender = lender;
        this.borrower = borrower;
        this.paid = 0;
        this.linearId = UUID.randomUUID();
        this.participants = participants;
    }

    public int getAmount() {
        return amount;
    }

    public MemberX500Name getLender() {
        return lender;
    }

    public MemberX500Name getBorrower() {
        return borrower;
    }

    public int getPaid() {
        return paid;
    }

    public UUID getLinearId() {
        return linearId;
    }

    @Override
    public List<PublicKey> getParticipants() {
        return participants;
    }

    //Helper method for settle flow
    public IOUState pay(int amountToPay) {
        int newAmountPaid = this.paid + (amountToPay);
        return new IOUState(amount, lender, borrower, newAmountPaid,this.linearId,this.participants);
    }

    //Helper method for transfer flow
    public IOUState withNewLender(MemberX500Name newLender, List<PublicKey> newParticipants) {
        return new IOUState(amount, newLender, borrower, paid,linearId,newParticipants);
    }

}
