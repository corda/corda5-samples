package com.r3.developers.samples.negotiation.workflows.util;

import java.util.UUID;

public class ListTradeArgs {

    private UUID proposalID;
    private int amount;
    private String buyer;
    private String seller;
    public ListTradeArgs() {
    }

    public ListTradeArgs(UUID proposalID, int amount, String buyer, String seller) {
        this.proposalID = proposalID;
        this.amount = amount;
        this.buyer = buyer;
        this.seller = seller;
    }

    public UUID getProposalID() {
        return proposalID;
    }

    public int getAmount() {
        return amount;
    }

    public String getBuyer() {
        return buyer;
    }

    public String getSeller() {
        return seller;
    }

}
