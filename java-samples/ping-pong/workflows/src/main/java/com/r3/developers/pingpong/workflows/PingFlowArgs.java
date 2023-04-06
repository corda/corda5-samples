package com.r3.developers.pingpong.workflows;

import net.corda.v5.base.types.MemberX500Name;

// A class to hold the arguments required to start the flow
public class PingFlowArgs {
    private MemberX500Name otherMember;

    // The JSON Marshalling Service, which handles serialisation, needs this constructor.
    public PingFlowArgs() {}

    public PingFlowArgs(MemberX500Name otherMember) {
        this.otherMember = otherMember;
    }

    public MemberX500Name getOtherMember() {
        return otherMember;
    }
}