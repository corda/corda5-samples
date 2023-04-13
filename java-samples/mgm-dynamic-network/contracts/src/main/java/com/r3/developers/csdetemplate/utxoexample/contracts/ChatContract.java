package com.r3.developers.csdetemplate.utxoexample.contracts;

import com.r3.developers.csdetemplate.utxoexample.states.ChatState;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.ledger.utxo.Command;
import net.corda.v5.ledger.utxo.Contract;
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ChatContract implements Contract {

    private final static Logger log = LoggerFactory.getLogger(ChatContract.class);

    public static class Create implements Command { }
    public static class Update implements Command { }

    @Override
    public void verify(UtxoLedgerTransaction transaction) {

        requireThat( transaction.getCommands().size() == 1, "Require a single command.");
        Command command = transaction.getCommands().get(0);

        ChatState output = transaction.getOutputStates(ChatState.class).get(0);

        requireThat(output.getParticipants().size() == 2, "The output state should have two and only two participants.");

        if(command.getClass() == Create.class) {
            requireThat(transaction.getInputContractStates().isEmpty(), "When command is Create there should be no input states.");
            requireThat(transaction.getOutputContractStates().size() == 1, "When command is Create there should be one and only one output state.");
        }
        else if(command.getClass() == Update.class) {
            requireThat(transaction.getInputContractStates().size() == 1, "When command is Update there should be one and only one input state.");
            requireThat(transaction.getOutputContractStates().size() == 1, "When command is Update there should be one and only one output state.");

            ChatState input = transaction.getInputStates(ChatState.class).get(0);
            requireThat(input.getId().equals(output.getId()), "When command is Update id must not change.");
            requireThat(input.getChatName().equals(output.getChatName()), "When command is Update chatName must not change.");
            requireThat(
                    input.getParticipants().containsAll(output.getParticipants()) &&
                    output.getParticipants().containsAll(input.getParticipants()),
                    "When command is Update participants must not change.");
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
