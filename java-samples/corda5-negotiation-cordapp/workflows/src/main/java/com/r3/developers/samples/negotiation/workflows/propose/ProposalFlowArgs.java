package com.r3.developers.samples.negotiation.workflows.propose;

import net.corda.v5.base.annotations.CordaSerializable;
import net.corda.v5.base.types.MemberX500Name;

@CordaSerializable
public class ProposalFlowArgs {
    private int amount;

    private MemberX500Name counterParty;

    private boolean isBuyer;

    public ProposalFlowArgs() {
    }

    public ProposalFlowArgs(int amount, MemberX500Name counterParty, boolean isBuyer) {
        this.amount = amount;
        this.counterParty = counterParty;
        this.isBuyer = isBuyer;
    }

    public int getAmount() {
        return amount;
    }

    public MemberX500Name getCounterParty() {
        return counterParty;
    }

    public boolean getIsBuyer() {
        return isBuyer;
    }
}
