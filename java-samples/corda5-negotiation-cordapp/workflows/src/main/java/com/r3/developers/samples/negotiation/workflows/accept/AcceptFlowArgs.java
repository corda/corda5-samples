package com.r3.developers.samples.negotiation.workflows.accept;

import net.corda.v5.base.annotations.CordaSerializable;

import java.util.UUID;

@CordaSerializable
public class AcceptFlowArgs {

    private UUID proposalID;

    public AcceptFlowArgs() {
    }

    public AcceptFlowArgs(UUID proposalId) {
        this.proposalID = proposalId;
    }

    public UUID getProposalID() {
        return proposalID;
    }


}
