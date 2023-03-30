package com.r3.developers.samples.obligation.workflows;

import java.util.UUID;

// A class to hold the deserialized arguments required to start the flow.
public class IOUSettleFlowArgs {
    private String amountSettle;
    private UUID iouID;

    public IOUSettleFlowArgs() {
    }

    public IOUSettleFlowArgs(String amountSettle, UUID iouID) {
        this.amountSettle = amountSettle;
        this.iouID = iouID;
    }

    public String getAmountSettle() {
        return amountSettle;
    }

    public UUID getIouID() {
        return iouID;
    }
}