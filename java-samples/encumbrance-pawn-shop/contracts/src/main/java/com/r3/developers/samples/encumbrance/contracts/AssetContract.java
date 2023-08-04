package com.r3.developers.samples.encumbrance.contracts;


import com.r3.developers.samples.encumbrance.states.Asset;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.ledger.utxo.Command;
import net.corda.v5.ledger.utxo.Contract;
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class AssetContract implements Contract {

    public static class AssetCommands implements Command { }

    public static class Create extends AssetCommands { }

    public static class Transfer extends AssetCommands { }

    public static class Lock extends AssetCommands { }

    public static class Unlock extends AssetCommands { }

    static final String REQUIRES_ONE_COMMAND = "The transaction requires one command";
    static final String REQUIRES_ZERO_INPUTS = "The transaction requires zero inputs";
    static final String REQUIRES_ONE_INPUT = "The transaction requires one input";
    static final String REQUIRES_ONE_OUTPUT = "The transaction requires one output";
    static final String REQUIRES_ONE_ASSET_OUTPUT = "The transaction requires one Asset output";
    static final String REQUIRES_ONE_ASSET_INPUT = "The transaction requires one Asset input";
    static final String REQUIRES_OWNER_SIGN = "Owner must sign the transaction";
    static final String REQUIRES_DIFFERENT_OWNER = "Owner must change in this transaction";

    @Override
    public void verify(@NotNull UtxoLedgerTransaction transaction) {
        requireThat(transaction.getCommands(AssetCommands.class).size() == 1, REQUIRES_ONE_COMMAND);
        Command command = transaction.getCommands(AssetCommands.class).get(0);

        if (command instanceof Create) {
            requireThat(transaction.getInputContractStates().isEmpty(), REQUIRES_ZERO_INPUTS);
            requireThat(transaction.getOutputContractStates().size() == 1, REQUIRES_ONE_OUTPUT);
            requireThat(transaction.getOutputStates(Asset.class).size() == 1, REQUIRES_ONE_ASSET_OUTPUT);

            Asset asset = transaction.getOutputStates(Asset.class).get(0);
            requireThat(transaction.getSignatories().contains(asset.getOwner().getLedgerKey()), REQUIRES_OWNER_SIGN);

        } else if (command instanceof Transfer) {
            requireThat(transaction.getInputContractStates().size() == 1, REQUIRES_ONE_INPUT);
            requireThat(transaction.getOutputContractStates().size() == 1, REQUIRES_ONE_OUTPUT);
            requireThat(transaction.getOutputStates(Asset.class).size() == 1, REQUIRES_ONE_ASSET_OUTPUT);
            requireThat(transaction.getInputStates(Asset.class).size() == 1, REQUIRES_ONE_ASSET_INPUT);

            Asset input = transaction.getInputStates(Asset.class).get(0);
            requireThat(transaction.getSignatories().contains(input.getOwner().getLedgerKey()), REQUIRES_OWNER_SIGN);

            Asset output = transaction.getOutputStates(Asset.class).get(0);
            requireThat(!input.getOwner().getName().equals(output.getOwner().getName()), REQUIRES_DIFFERENT_OWNER);

        } else if (command instanceof Lock) {
            // Verification logic required while locking the asset goes here
        } else if (command instanceof Unlock) {
            // Verification logic required while unlocking the asset goes here
        } else {
            throw new CordaRuntimeException("Invalid Command");
        }
    }

    private void requireThat(boolean asserted, String errorMessage) {
        if(!asserted) {
            throw new CordaRuntimeException("Failed requirement: " + errorMessage);
        }
    }
}
