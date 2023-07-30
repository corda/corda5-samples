package com.r3.developers.samples.encumbrance.contracts;

import com.r3.developers.samples.encumbrance.states.Asset;
import com.r3.developers.samples.encumbrance.states.Loan;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.ledger.utxo.Command;
import net.corda.v5.ledger.utxo.Contract;
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class LoanContract implements Contract {

    public static class LoanCommands implements Command { }
    public static class Issue extends LoanCommands { }

    public static class Settle extends LoanCommands { }

    static final String REQUIRES_ONE_COMMAND = "The transaction requires one Loan command";
    static final String REQUIRES_ONE_INPUT = "The transaction requires one input";
    static final String REQUIRES_TWO_INPUTS = "The transaction requires two inputs";
    static final String REQUIRES_ONE_ASSET_INPUT = "The transaction requires one Asset input";
    static final String REQUIRES_ONE_LOAN_INPUT = "The transaction requires one Loan input";
    static final String REQUIRES_TWO_OUTPUTS = "The transaction requires two outputs";
    static final String REQUIRES_ONE_OUTPUT = "The transaction requires one output";
    static final String REQUIRES_ONE_ASSET_OUTPUT = "The transaction requires one Asset output";
    static final String REQUIRES_ONE_LOAN_OUTPUT = "The transaction requires one Loan output";
    static final String REQUIRES_LENDER_SIGN = "The transaction must be signed by the lender";

    @Override
    public void verify(@NotNull UtxoLedgerTransaction transaction) {
        requireThat(transaction.getCommands(LoanCommands.class).size() == 1, REQUIRES_ONE_COMMAND);
        Command command = transaction.getCommands(LoanCommands.class).get(0);

        if (command instanceof Issue) {
            requireThat(transaction.getInputContractStates().size() == 1, REQUIRES_ONE_INPUT);
            requireThat(transaction.getInputStates(Asset.class).size() == 1, REQUIRES_ONE_ASSET_INPUT);

            requireThat(transaction.getOutputContractStates().size() == 2, REQUIRES_TWO_OUTPUTS);
            requireThat(transaction.getOutputStates(Asset.class).size() == 1, REQUIRES_ONE_ASSET_OUTPUT);
            requireThat(transaction.getOutputStates(Loan.class).size() == 1, REQUIRES_ONE_LOAN_OUTPUT);

            requireThat(
                    transaction.getSignatories().contains(
                    transaction.getOutputStates(Loan.class).get(0).getLender().getLedgerKey()), REQUIRES_LENDER_SIGN);

        }else if (command instanceof Settle){
            requireThat(transaction.getInputContractStates().size() == 2, REQUIRES_TWO_INPUTS);
            requireThat(transaction.getInputStates(Asset.class).size() == 1, REQUIRES_ONE_ASSET_INPUT);
            requireThat(transaction.getInputStates(Loan.class).size() == 1, REQUIRES_ONE_LOAN_INPUT);

            requireThat(transaction.getOutputContractStates().size() == 1, REQUIRES_ONE_OUTPUT);
            requireThat(transaction.getOutputStates(Asset.class).size() == 1, REQUIRES_ONE_ASSET_OUTPUT);

        }else {
            throw new CordaRuntimeException("Invalid Command");
        }
    }

    private void requireThat(boolean asserted, String errorMessage) {
        if(!asserted) {
            throw new CordaRuntimeException("Failed requirement: " + errorMessage);
        }
    }
}
