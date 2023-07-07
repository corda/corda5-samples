package com.r3.developers.samples.referencestate.workflows;

import net.corda.v5.base.annotations.CordaSerializable;
import net.corda.v5.base.types.MemberX500Name;

@CordaSerializable
public class IOUIssueFlowArgs{
    private int iouValue;
    private MemberX500Name otherParty;
    private MemberX500Name sanctionsBody;

    public IOUIssueFlowArgs() {
    }

    public int getIouValue() {
        return iouValue;
    }

    public MemberX500Name getOtherParty() {
        return otherParty;
    }

    public MemberX500Name getSanctionsBody() {
        return sanctionsBody;
    }
}
