package com.r3.developers.csdetemplate.utxoexample.contracts;

import net.corda.v5.ledger.utxo.Command;
import net.corda.v5.ledger.utxo.Contract;
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GoldContract implements Contract {

    private final static Logger log = LoggerFactory.getLogger(GoldContract.class);

    public static class Create implements Command { }

    public static class Transfer implements Command { }


    @Override
    public void verify(UtxoLedgerTransaction transaction) {

    }
}
