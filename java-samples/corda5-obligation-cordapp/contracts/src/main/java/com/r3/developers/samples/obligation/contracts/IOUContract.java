package com.r3.developers.samples.obligation.contracts;

import com.r3.developers.samples.obligation.states.IOUState;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.ledger.utxo.Command;
import net.corda.v5.ledger.utxo.Contract;
import net.corda.v5.ledger.utxo.ContractState;
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction;

import java.security.PublicKey;
import java.util.Set;


public class IOUContract implements Contract {


    @Override
    public boolean isRelevant(ContractState state, Set<PublicKey> myKeys) {
        return Contract.super.isRelevant(state, myKeys);
    }

    public static class Issue implements Command { }
    public static class Settle implements Command { }
    public static class Transfer implements Command { }

    @Override
    public void verify(UtxoLedgerTransaction transaction) {

        requireThat( transaction.getCommands().size() == 1, "Require a single command.");
        Command command = transaction.getCommands().get(0);
        IOUState output = transaction.getOutputStates(IOUState.class).get(0);
        requireThat(output.getParticipants().size() == 2, "The output state should have two and only two participants.");

        if(command.getClass() == IOUContract.Issue.class) {
            requireThat(transaction.getOutputContractStates().size() == 1, "Only one output states should be created when issuing an IOU.");

        }else if(command.getClass() == IOUContract.Transfer.class) {
            requireThat( transaction.getInputContractStates().size() > 0, "There must be one input IOU.");
        }
        else if(command.getClass() == IOUContract.Settle.class) {
            requireThat( transaction.getInputContractStates().size() > 0, "There must be one input IOU.");
        }
        else {
            throw new CordaRuntimeException("Unsupported command");
        }
    }
    private void requireThat(boolean asserted, String errorMessage) {
        if(!asserted) {
            throw new CordaRuntimeException("Failed requirement: " + errorMessage);
        }
    }

}
