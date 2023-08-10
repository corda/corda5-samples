package com.r3.developers.samples.referencestate.contracts;

import com.r3.developers.samples.referencestate.states.SanctionList;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.ledger.utxo.Command;
import net.corda.v5.ledger.utxo.Contract;
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class SanctionListContract implements Contract {

    public static class Create implements Command { }

    public static class Update implements Command { }

    static final String REQUIRES_ONE_COMMAND = "The transaction requires exactly one command";
    static final String CREATE_REQUIRES_ZERO_INPUTS = "When creating a sanctions list there should be no inputs";
    static final String CREATE_REQUIRES_ONE_OUTPUT = "When creating a sanctions list there should be exactly one output";
    static final String CREATE_REQUIRES_ONE_SANCTION_ENTITY_OUTPUT =
            "When creating a sanctions list there should be one output of type SanctionedEntities";
    static final String CREATE_REQUIRES_ISSUER_SIGNATURE = "The issuer of the sanctions list must sign";

    static final String UPDATE_REQUIRES_ONE_INPUT = "There must be exactly one input when updating Sanctions List";
    static final String UPDATE_REQUIRES_ONE_OUTPUT = "There must be exactly one output when updating Sanctions List";
    static final String UPDATE_REQUIRES_ONE_SANCTION_ENTITY_INPUT =
            "There must be exactly one input Sanctions List when updating";
    static final String UPDATE_REQUIRES_ONE_SANCTION_ENTITY_OUTPUT =
            "There must be exactly one input Sanctions List when updating";
    static final String UPDATE_ISSUER_SHOULD_NOT_CHANGE =
            "The issuer must remain the same across an update";


    @Override
    public void verify(@NotNull UtxoLedgerTransaction transaction) {

        requireThat(!transaction.getCommands().isEmpty(), REQUIRES_ONE_COMMAND);
        Command command = transaction.getCommands().get(0);

        if (command instanceof Create) {
            requireThat(transaction.getInputContractStates().isEmpty(), CREATE_REQUIRES_ZERO_INPUTS);
            requireThat(transaction.getOutputContractStates().size() ==1, CREATE_REQUIRES_ONE_OUTPUT);
            requireThat(transaction.getOutputStates(SanctionList.class).size() == 1,
                    CREATE_REQUIRES_ONE_SANCTION_ENTITY_OUTPUT);

            SanctionList out = transaction.getOutputStates(SanctionList.class).get(0);
            requireThat(transaction.getSignatories().contains(out.getIssuer().getLedgerKey()), CREATE_REQUIRES_ISSUER_SIGNATURE);

        } else if (command instanceof Update) {
            requireThat(transaction.getInputContractStates().size() == 1, UPDATE_REQUIRES_ONE_INPUT);
            requireThat(transaction.getOutputContractStates().size() == 1, UPDATE_REQUIRES_ONE_OUTPUT);

            requireThat(transaction.getInputStates(SanctionList.class).size() == 1,
                    UPDATE_REQUIRES_ONE_SANCTION_ENTITY_INPUT);
            requireThat(transaction.getOutputStates(SanctionList.class).size() == 1,
                    UPDATE_REQUIRES_ONE_SANCTION_ENTITY_OUTPUT);

            SanctionList input = transaction.getInputStates(SanctionList.class).get(0);
            SanctionList output = transaction.getOutputStates(SanctionList.class).get(0);
            requireThat(input.getIssuer().getName().equals(output.getIssuer().getName()),
                    UPDATE_ISSUER_SHOULD_NOT_CHANGE);
        }
    }

    private void requireThat(boolean asserted, String errorMessage) {
        if(!asserted) {
            throw new CordaRuntimeException("Failed requirement: " + errorMessage);
        }
    }
}
