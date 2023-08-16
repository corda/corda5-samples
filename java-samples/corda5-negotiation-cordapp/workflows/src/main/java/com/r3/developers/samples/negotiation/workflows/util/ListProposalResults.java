package com.r3.developers.samples.negotiation.workflows.util;

import net.corda.v5.base.types.MemberX500Name;

import java.util.UUID;

public class ListProposalResults {

   private UUID proposalID;
   private int amount;
   private String buyer;
   private String seller;
   private String proposer;
   private String proposee;

   public ListProposalResults() {
   }

   public ListProposalResults(UUID proposalID, int amount, String buyer, String seller,  String proposer,String proposee) {
      this.proposalID = proposalID;
      this.amount = amount;
      this.buyer = buyer;
      this.seller = seller;
      this.proposer = proposer;
      this.proposee= proposee;
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
   public String getProposer() {
      return proposer;
   }
   public String getProposee() {
      return proposee;
   }


}
