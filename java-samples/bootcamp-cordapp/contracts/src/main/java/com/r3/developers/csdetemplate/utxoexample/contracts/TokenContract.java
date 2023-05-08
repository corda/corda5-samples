package com.r3.developers.csdetemplate.utxoexample.contracts;

import com.r3.developers.csdetemplate.utxoexample.states.TokenState;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.ledger.utxo.Command;
import net.corda.v5.ledger.utxo.Contract;
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class TokenContract implements Contract {

    public static class Issue implements Command { }

    @Override
    public void verify(@NotNull UtxoLedgerTransaction transaction) {

        requireThat( transaction.getCommands().size() == 1, "Require a single command.");
        Command command = transaction.getCommands().get(0);
        TokenState output = transaction.getOutputStates(TokenState.class).get(0);
        requireThat(output.getParticipants().size() == 2, "The output state should have two and only two participants.");

        if(command.getClass() == Issue.class) {
            requireThat(transaction.getOutputContractStates().size() == 1, "Only one output states should be created when issuing a token.");

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
