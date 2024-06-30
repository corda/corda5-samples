package com.r3.developers.samples.obligation.workflows;

import java.util.List;

public class sendAndRecieveTransactionArgs {


    private String stateRef;
    private List<String> members;
    private boolean forceBackchain;

    public sendAndRecieveTransactionArgs(){

    }
    public sendAndRecieveTransactionArgs(String stateRef, List<String> members, boolean forceBackchain) {
        this.stateRef = stateRef;
        this.members = members;
        this.forceBackchain = forceBackchain;
    }

    public String getStateRef() {
        return stateRef;
    }

    public List<String> getMembers() {
        return members;
    }

    public boolean isForceBackchain() {
        return forceBackchain;
    }

}
