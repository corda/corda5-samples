package com.r3.developers.samples.persistence.contracts;

import net.corda.v5.ledger.utxo.Command;
import net.corda.v5.ledger.utxo.Contract;
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class InsuranceContract implements Contract {
    public static class IssueInsurance implements Command { }

    public static class AddClaim implements Command { }
    @Override
    public void verify(@NotNull UtxoLedgerTransaction transaction) {
        // Add contract validation logic here.
    }
}
