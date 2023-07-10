package com.r3.developers.samples.referencestate.workflows;

import net.corda.v5.base.annotations.CordaSerializable;
import net.corda.v5.base.types.MemberX500Name;

@CordaSerializable
public class UpdateSanctionListFlowArgs {
    private MemberX500Name partyToSanction;

    public UpdateSanctionListFlowArgs() {
    }

    public MemberX500Name getPartyToSanction() {
        return partyToSanction;
    }
}
