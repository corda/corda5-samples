package com.r3.developers.samples.obligation.workflows;

public class RecieveTransactionFlowArgs {

    private String transactionId;

    public RecieveTransactionFlowArgs(){}
    public RecieveTransactionFlowArgs(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionId() {
        return transactionId;
    }
}



