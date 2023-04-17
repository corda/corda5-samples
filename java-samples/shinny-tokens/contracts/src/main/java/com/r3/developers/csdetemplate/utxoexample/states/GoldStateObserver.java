package com.r3.developers.csdetemplate.utxoexample.states;

import net.corda.v5.ledger.utxo.observer.UtxoLedgerTokenStateObserver;
import net.corda.v5.ledger.utxo.observer.UtxoToken;
import net.corda.v5.ledger.utxo.observer.UtxoTokenFilterFields;
import net.corda.v5.ledger.utxo.observer.UtxoTokenPoolKey;

/*
By implementing the UtxoLedgerTokenStateObserver, this observer will generate fungible states/tokens for
each produced gold state when persisting a finalized transaction to the vault.
 */
public class GoldStateObserver implements UtxoLedgerTokenStateObserver<GoldState> {

    @Override
    public Class<GoldState> getStateType() {
        return GoldState.class;
    }

    @Override
    public UtxoToken onCommit(GoldState goldState) {

        //generate a pool with key - type, issuer and symbol to mint the tokens
        UtxoTokenPoolKey poolKey = new UtxoTokenPoolKey(GoldState.class.getName(), goldState.getIssuer(), goldState.getSymbol());
        return new UtxoToken(poolKey, goldState.getValue(), new UtxoTokenFilterFields(null, goldState.getOwner()));
    }
}
