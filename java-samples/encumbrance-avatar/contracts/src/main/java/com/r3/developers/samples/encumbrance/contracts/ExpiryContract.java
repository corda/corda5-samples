package com.r3.developers.samples.encumbrance.contracts;

import com.r3.developers.samples.encumbrance.states.Expiry;
import net.corda.v5.ledger.utxo.Command;
import net.corda.v5.ledger.utxo.Contract;
import net.corda.v5.ledger.utxo.TimeWindow;
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class ExpiryContract implements Contract {

    public static class Create implements Command { }

    public static class Pass implements Command { }

    @Override
    public void verify(@NotNull UtxoLedgerTransaction transaction) {
        Expiry expiry;
        if (transaction.getCommands().stream().anyMatch(e -> e instanceof AvatarContract.Transfer))
            expiry = transaction.getInputStates(Expiry.class).get(0);
        else
            expiry = transaction.getOutputStates(Expiry.class).get(0);

        TimeWindow timeWindow = transaction.getTimeWindow();

        //Expiry time should be after the time window, if the avatar expires before the time window, then the avatar
        //cannot be sold
        if (timeWindow.getUntil().isAfter(expiry.getExpiry())) {
            throw new IllegalArgumentException("Avatar transfer time has expired! Expiry date & time was: " +
                    LocalDateTime.ofInstant(expiry.getExpiry(), ZoneId.systemDefault()));
        }
    }
}
