package com.r3.developers.samples.obligation.workflows;

import java.util.UUID;

// A class to hold the deserialized arguments required to start the flow.
public class ListIOUFlowResults {

    private UUID id;
    private int amount;
    private String borrower;
    private String lender;
    private int paid;

    public ListIOUFlowResults() {
    }

    public ListIOUFlowResults(UUID id, int amount, String borrower, String lender, int paid) {
        this.id = id;
        this.amount = amount;
        this.borrower = borrower;
        this.lender = lender;
        this.paid = paid;
    }

    public UUID getId() {
        return id;
    }

    public int getAmount() {
        return amount;
    }

    public String getBorrower() {
        return borrower;
    }

    public String getLender() {
        return lender;
    }

    public int getPaid() {
        return paid;
    }
}
