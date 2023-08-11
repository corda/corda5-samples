package com.r3.developers.samples.referencestate.workflows;

import net.corda.v5.base.annotations.CordaSerializable;
import net.corda.v5.base.types.MemberX500Name;

@CordaSerializable
public class IOUIssueFlowArgs{
    private int iouValue;
    private MemberX500Name lenderName;
    private MemberX500Name sanctionAuthority;

    public IOUIssueFlowArgs() {
    }

    public int getIouValue() {
        return iouValue;
    }

    public MemberX500Name getLenderName() {
        return lenderName;
    }

    public MemberX500Name getSanctionAuthority() {
        return sanctionAuthority;
    }
}
