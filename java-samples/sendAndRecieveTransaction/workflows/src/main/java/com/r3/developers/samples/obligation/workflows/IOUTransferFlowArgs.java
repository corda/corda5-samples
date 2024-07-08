package com.r3.developers.samples.obligation.workflows;

import java.util.UUID;

// A class to hold the deserialized arguments required to start the flow.
public class IOUTransferFlowArgs {

    private String newLender;
    private UUID iouID;

    public IOUTransferFlowArgs() {
    }

    public IOUTransferFlowArgs(String newLender, UUID iouID) {
        this.newLender = newLender;
        this.iouID = iouID;
    }

    public String getNewLender() {
        return newLender;
    }

    public UUID getIouID() {
        return iouID;
    }
}
