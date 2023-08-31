package com.r3.developers.samples.negotiation;

import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.ledger.utxo.Command;
import net.corda.v5.ledger.utxo.Contract;
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction;

import java.util.Objects;


public class ProposalAndTradeContract implements Contract {

    @Override
    public void verify(UtxoLedgerTransaction transaction) {
        String commandErrorMsg = "Expected a single command of type NegotiationCommand.";

        // Extract the command from the transaction
        // Verify the transaction according to the intention of the transaction
        final NegotiationCommands command = (NegotiationCommands) transaction.getCommands().get(0);
        if (command != null) {
            command.verify(transaction);
        } else {
            throw new IllegalArgumentException(commandErrorMsg);
        }
    }

    public interface NegotiationCommands extends Command {
        void verify(UtxoLedgerTransaction transaction);
    }

    //class to verify the inputs for propose command
    public static class Propose implements NegotiationCommands {

        String noInputsMsg = "There are no inputs";
        String oneOutputMsg = "Only one output state should be created.";
        String outputTypeMsg = "The single output is of type ProposalState";
        String commandMsg = "There is exactly one command";
        String buyerMsg = "The buyer are the proposer";
        String sellerMsg = "The seller are the proposee";
        String proposerMsg = "The proposer is a required signer";
        String proposeeMsg = "The proposee is a required signer";

        @Override
        public void verify(UtxoLedgerTransaction transaction) {
            Proposal proposalStateOutput = transaction.getOutputStates(Proposal.class).get(0);

            require(transaction.getInputStateAndRefs().isEmpty(), noInputsMsg);
            require(transaction.getOutputTransactionStates().size() == 1, oneOutputMsg);
            require(transaction.getOutputStates(Proposal.class).size() == 1, outputTypeMsg);
            require(transaction.getCommands().size() == 1, commandMsg);
            require(Objects.equals(proposalStateOutput.getProposer().toString(), proposalStateOutput.getBuyer().toString()), buyerMsg);
            require(Objects.equals(proposalStateOutput.getProposee().toString(), proposalStateOutput.getSeller().toString()), sellerMsg);
            require(transaction.getSignatories().contains(proposalStateOutput.getProposer().getLedgerKey()), proposerMsg);
            require(transaction.getSignatories().contains(proposalStateOutput.getProposee().getLedgerKey()), proposeeMsg);
        }
    }

    //class to verify the inputs for Modify command
    public static class Modify implements NegotiationCommands {
        String oneInputMsg = "There is exactly one input";
        String inputTypeMsg = "The single input is of type ProposalState";
        String oneOutputMsg = "There is exactly one output";
        String outputTypeMsg = "The single output is of type ProposalState";
        String oneCommandMsg = "There is exactly one command";
        String amountModifiedMsg = "The amount is modified in the output";
        String buyerMsg = "The buyer is unmodified in the output";
        String sellerMsg = "The seller is unmodified in the output";
        String proposerMsg = "The proposer is a required signer";
        String proposeeMsg = "The proposee is a required signer";


        @Override
        public void verify(UtxoLedgerTransaction transaction) {
            Proposal proposalStateOutput = transaction.getOutputStates(Proposal.class).get(0);
            Proposal proposalStateInputs = transaction.getInputStates(Proposal.class).get(0);

            require(transaction.getInputStateAndRefs().size() == 1, oneInputMsg);
            require(transaction.getInputStates(Proposal.class).size() == 1, inputTypeMsg);
            require(transaction.getOutputTransactionStates().size() == 1, oneOutputMsg);
            require(transaction.getOutputStates(Proposal.class).size() == 1, outputTypeMsg);
            require(transaction.getCommands().size() == 1, oneCommandMsg);

            require(proposalStateOutput.getAmount() != proposalStateInputs.getAmount(), amountModifiedMsg);
            require(Objects.equals(proposalStateInputs.getBuyer().toString(), proposalStateOutput.getBuyer().toString()), buyerMsg);
            require(Objects.equals(proposalStateInputs.getSeller().toString(), proposalStateOutput.getSeller().toString()), sellerMsg);

            require(transaction.getSignatories().contains(proposalStateInputs.getProposer().getLedgerKey()), proposerMsg);
            require(transaction.getSignatories().contains(proposalStateInputs.getProposee().getLedgerKey()), proposeeMsg);
        }
    }

    //class to verify the inputs for Accept command
    public static class Accept implements NegotiationCommands {
        String oneInputMsg = "There is exactly one input";
        String inputTypeMsg = "The single input is of type ProposalState";
        String oneOutputMsg = "There is exactly one output";
        String outputTypeMsg = "The single output is of type TradeState";
        String oneCommandMsg = "There is exactly one command";
        String amountMsg = "The amount is unmodified in the output";
        String buyerMsg = "The buyer is unmodified in the output";
        String sellerMsg = "The seller is unmodified in the output";
        String proposerMsg = "The proposer is a required signer";
        String proposeMsg = "The propose is a required signer";
        String proposerCannotAcceptProposalMsg = "The proposer cannot accept their own proposal";

        @Override
        public void verify(UtxoLedgerTransaction transaction) {
            Trade tradeStateOutput = transaction.getOutputStates(Trade.class).get(0);
            Proposal proposalStateInputs = transaction.getInputStates(Proposal.class).get(0);

            require(transaction.getInputStateAndRefs().size() == 1, oneInputMsg);
            require(transaction.getInputStates(Proposal.class).size() == 1, inputTypeMsg);
            require(transaction.getOutputTransactionStates().size() == 1, oneOutputMsg);
            require(transaction.getOutputStates(Trade.class).size() == 1, outputTypeMsg);
            require(transaction.getCommands().size() == 1, oneCommandMsg);
            require(proposalStateInputs.getModifier() == null ||
                            !proposalStateInputs.getModifier().getName().toString().equals(tradeStateOutput.getAcceptor().toString()),
                            proposerCannotAcceptProposalMsg);

            require(tradeStateOutput.getAmount() == proposalStateInputs.getAmount(), amountMsg);
            require(proposalStateInputs.getBuyer().toString().equals(tradeStateOutput.getBuyer().toString()), buyerMsg);
            require(proposalStateInputs.getSeller().toString().equals(tradeStateOutput.getSeller().toString()), sellerMsg);

            require(transaction.getSignatories().contains(proposalStateInputs.getProposer().getLedgerKey()), proposerMsg);
            require(transaction.getSignatories().contains(proposalStateInputs.getProposee().getLedgerKey()), proposeMsg);
        }
    }

    // helper method that throws the error message if relevant input is not accepted.
    private static void require(boolean asserted, String errorMessage) {
        if (!asserted) {
            throw new CordaRuntimeException(errorMessage);
        }
    }

}
