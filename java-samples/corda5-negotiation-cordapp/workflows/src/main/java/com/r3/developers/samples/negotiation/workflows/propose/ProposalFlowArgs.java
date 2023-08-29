package com.r3.developers.samples.negotiation.workflows.propose;

import net.corda.v5.base.annotations.CordaSerializable;

@CordaSerializable
public class ProposalFlowArgs {
    private int amount;

    private String counterParty;

    private boolean isBuyer;

    public ProposalFlowArgs() {
    }

    public ProposalFlowArgs(int amount, String counterParty, boolean isBuyer) {
        this.amount = amount;
        this.counterParty = counterParty;
        this.isBuyer = isBuyer;
    }

    public int getAmount() {
        return amount;
    }

    public String getCounterParty() {
        return counterParty;
    }

    public boolean isBuyer() {
        return isBuyer;
    }


}
