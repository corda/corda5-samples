package com.r3.developers.samples.encumbrance.contracts;

import com.r3.developers.samples.encumbrance.states.Avatar;
import com.r3.developers.samples.encumbrance.states.Expiry;
import net.corda.v5.base.exceptions.CordaRuntimeException;
import net.corda.v5.ledger.utxo.Command;
import net.corda.v5.ledger.utxo.Contract;
import net.corda.v5.ledger.utxo.EncumbranceGroup;
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class AvatarContract implements Contract {

    public static class AvatarCommands implements Command { }

    public static class Create extends AvatarCommands { }

    public static class Transfer extends AvatarCommands { }

    static final String REQUIRES_ONE_COMMAND = "The transaction requires one command";
    static final String REQUIRES_ZERO_INPUTS = "The transaction requires zero inputs";
    static final String REQUIRES_TWO_OUTPUTS = "The transaction requires two outputs";
    static final String REQUIRES_ONE_AVATAR_OUTPUT = "The transaction requires one Avatar output";
    static final String REQUIRES_ONE_EXPIRY_OUTPUT = "The transaction requires one Expiry output";
    static final String REQUIRES_OWNER_SIGNATURE = "Avatar Owner must always sign the newly created Avatar.";
    static final String REQUIRE_AVATAR_ENCUMBRANCE = "Avatar needs to be encumbered.";
    static final String REQUIRE_TWO_INPUTS = "The transaction requires two inputs";
    static final String REQUIRES_ONE_AVATAR_INPUT = "The transaction requires one Avatar input";
    static final String REQUIRES_ONE_EXPIRY_INPUT = "The transaction requires one Expiry input";
    static final String IDENTICAL_INPUT_OUTPUT = "New and old Avatar must just have the owners changed.";
    static final String REQUIRE_NEW_OWNER_SIGNATURE = "New Owner should sign the new Avatar";
    static final String REQIRE_OLD_OWNER_SIGNATURE = "Old owner must sign the old Avatar";

    @Override
    public void verify(@NotNull UtxoLedgerTransaction transaction) {
        requireThat(transaction.getCommands(AvatarCommands.class).size() == 1, REQUIRES_ONE_COMMAND);
        Command command = transaction.getCommands(AvatarCommands.class).get(0);
        //List<PublicKey> signers = commandWithParties.getSigners();

        if (command instanceof Create) {
            requireThat(transaction.getInputContractStates().isEmpty(), REQUIRES_ZERO_INPUTS);
            requireThat(transaction.getOutputContractStates().size() == 2, REQUIRES_TWO_OUTPUTS);
            requireThat(transaction.getOutputStates(Expiry.class).size() == 1, REQUIRES_ONE_EXPIRY_OUTPUT);
            requireThat(transaction.getOutputStates(Avatar.class).size() == 1, REQUIRES_ONE_AVATAR_OUTPUT);

            Avatar avatar = transaction.getOutputStates(Avatar.class).get(0);
            requireThat(transaction.getSignatories().contains(avatar.getOwner().getLedgerKey()),
                    REQUIRES_OWNER_SIGNATURE);
            EncumbranceGroup encumbranceGroup =
                    transaction.getOutputTransactionStates().stream().filter(o->o.getContractState() instanceof Avatar)
                    .findFirst().get().getEncumbranceGroup();
            requireThat(encumbranceGroup != null, REQUIRE_AVATAR_ENCUMBRANCE);

        } else if (command instanceof Transfer) {
            requireThat(transaction.getInputContractStates().size() == 2, REQUIRE_TWO_INPUTS);
            requireThat(transaction.getInputStates(Expiry.class).size() == 1, REQUIRES_ONE_EXPIRY_INPUT);
            requireThat(transaction.getInputStates(Avatar.class).size() == 1, REQUIRES_ONE_AVATAR_INPUT);

            Avatar newAvatar = transaction.getOutputStates(Avatar.class).stream().findFirst().orElseThrow(
                    () -> new CordaRuntimeException("No Avatar created for transferring.")
            );
            Avatar oldAvatar = transaction.getInputStates(Avatar.class).stream().findFirst().orElseThrow(
                    () -> new CordaRuntimeException("Existing Avatar to transfer not found.")
            );

            requireThat(newAvatar.equals(oldAvatar), IDENTICAL_INPUT_OUTPUT);
            requireThat(transaction.getSignatories().contains(newAvatar.getOwner().getLedgerKey()),
                    REQUIRE_NEW_OWNER_SIGNATURE);
            requireThat(transaction.getSignatories().contains(oldAvatar.getOwner().getLedgerKey()),
                    REQIRE_OLD_OWNER_SIGNATURE);
        }
    }

    private void requireThat(boolean asserted, String errorMessage) {
        if(!asserted) {
            throw new CordaRuntimeException("Failed requirement: " + errorMessage);
        }
    }
}
