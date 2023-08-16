package com.r3.developers.samples.negotiation.contracts;

import com.r3.developers.samples.negotiation.states.Proposal;
import com.r3.developers.samples.negotiation.states.Trade;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.ledger.utxo.Command;
import net.corda.v5.ledger.utxo.Contract;

import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction;

import java.util.Objects;


public class ProposalAndTradeContract implements Contract {
    @Override
    public void verify(UtxoLedgerTransaction transaction){
        // Extract the command from the transaction
        // Verify the transaction according to the intention of the transaction
        final NegotiationCommands command = (NegotiationCommands) transaction.getCommands().get(0);
        if(command!=null){
         command.verify(transaction);
        } else {
            throw new IllegalArgumentException("Expected a single command of type NegotiationCommand.");
        }
    }

     public interface NegotiationCommands extends Command {
         public void verify(UtxoLedgerTransaction transaction);
     }


        public static class Propose implements NegotiationCommands {
         @Override
         public void verify(UtxoLedgerTransaction transaction){
           Proposal proposalStateOutput = transaction.getOutputStates(Proposal.class).get(0);



           require(transaction.getInputStateAndRefs().isEmpty(),"There are no inputs");
           require(transaction.getOutputTransactionStates().size()==1,"Only one output state should be created.");
           require(transaction.getOutputStates(Proposal.class).size()==1,"The single output is of type ProposalState");
           require(transaction.getCommands().size()==1,"There is exactly one command");
           require(Objects.equals(proposalStateOutput.getProposer().toString(), proposalStateOutput.getBuyer().toString()),"The buyer are the proposer");
           require(Objects.equals(proposalStateOutput.getProposee().toString(), proposalStateOutput.getSeller().toString()),"The seller are the proposee");
           require(transaction.getSignatories().contains(proposalStateOutput.getProposer().getLedgerKey()),"The proposer is a required signer");
           require(transaction.getSignatories().contains(proposalStateOutput.getProposee().getLedgerKey()),"The proposee is a required signer");
         }
        }

        public static class Accept implements NegotiationCommands {
            @Override
            public void verify(UtxoLedgerTransaction transaction){
            Trade tradeStateOutput = transaction.getOutputStates(Trade.class).get(0);
            Proposal proposalStateInputs =transaction.getInputStates(Proposal.class).get(0);

            require(transaction.getInputStateAndRefs().size()==1,"There is exactly one input");
            require(transaction.getInputStates(Proposal.class).size() == 1, "The single input is of type ProposalState");
            require(transaction.getOutputTransactionStates().size()==1,"There is exactly one output");
            require(transaction.getOutputStates(Trade.class).size()==1,"The single output is of type TradeState");
            require(transaction.getCommands().size()==1,"There is exactly one command");

            require( tradeStateOutput.getAmount()==proposalStateInputs.getAmount(),"The amount is unmodified in the output");
            require(proposalStateInputs.getBuyer().toString().equals(tradeStateOutput.getBuyer().toString()),"The buyer is unmodified in the output");
            require(proposalStateInputs.getSeller().toString().equals(tradeStateOutput.getSeller().toString()),"The seller is unmodified in the output");

            require(transaction.getSignatories().contains(proposalStateInputs.getProposer().getLedgerKey()),"The proposer is a required signer");
            require(transaction.getSignatories().contains(proposalStateInputs.getProposee().getLedgerKey()),"The proposee is a required signer");

            }
        }

        public static class Modify implements NegotiationCommands {
            @Override
            public void verify(UtxoLedgerTransaction transaction){
            Proposal proposalStateOutput = transaction.getOutputStates(Proposal.class).get(0);
            Proposal proposalStateInputs =transaction.getInputStates(Proposal.class).get(0);

            require(transaction.getInputStateAndRefs().size()==1,"There is exactly one input");
            require(transaction.getInputStates(Proposal.class).size() == 1,"The single input is of type ProposalState");
            require(transaction.getOutputTransactionStates().size()==1,"There is exactly one output");
            require(transaction.getOutputStates(Proposal.class).size()==1,"The single output is of type ProposalState");
            require(transaction.getCommands().size()==1,"There is exactly one command");

            require(proposalStateOutput.getAmount() != proposalStateInputs.getAmount(),"The amount is modified in the output");
            require(Objects.equals(proposalStateInputs.getBuyer().toString(), proposalStateOutput.getBuyer().toString()),"The buyer is unmodified in the output");
            require(Objects.equals(proposalStateInputs.getSeller().toString(), proposalStateOutput.getSeller().toString()),"The seller is unmodified in the output");

            //warn message over members
            require(transaction.getSignatories().contains(proposalStateInputs.getProposer().getLedgerKey()),"The proposer is a required signer");
            require(transaction.getSignatories().contains(proposalStateInputs.getProposee().getLedgerKey()),"The proposee is a required signer");
            }
        }

    private static void require(boolean asserted, String errorMessage) {
        if (!asserted) {
            throw new CordaRuntimeException(errorMessage);
        }
    }

}
