package com.r3.developers.samples.negotiation.workflows.modify;

import net.corda.v5.base.annotations.CordaSerializable;

import java.util.UUID;

@CordaSerializable
public class ModifyFlowArgs {
    private UUID proposalID;
    private int newAmount;

    public ModifyFlowArgs() {
    }

    public ModifyFlowArgs(UUID proposalId, int newAmount) {
        this.proposalID = proposalId;
        this.newAmount = newAmount;
    }

    public int getNewAmount() {
        return newAmount;
    }

    public UUID getProposalID() {
        return proposalID;
    }


}
