package com.r3.developers.samples.negotiation.workflows.accept;

import net.corda.v5.base.annotations.CordaSerializable;
import net.corda.v5.base.types.MemberX500Name;

import java.util.UUID;

@CordaSerializable
public class AcceptFlowArgs {

    private UUID proposalID;
    private MemberX500Name acceptor;

    public AcceptFlowArgs() {
    }

    public AcceptFlowArgs(UUID proposalId, MemberX500Name acceptor) {
        this.proposalID = proposalId;
        this.acceptor = acceptor;
    }

    public UUID getProposalID() {
        return proposalID;
    }

    public MemberX500Name getAcceptor() {
        return acceptor;
    }
}
