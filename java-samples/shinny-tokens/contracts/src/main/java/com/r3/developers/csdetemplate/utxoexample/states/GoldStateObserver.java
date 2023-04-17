package com.r3.developers.csdetemplate.utxoexample.states;

import net.corda.v5.ledger.utxo.observer.UtxoLedgerTokenStateObserver;
import net.corda.v5.ledger.utxo.observer.UtxoToken;
import net.corda.v5.ledger.utxo.observer.UtxoTokenFilterFields;
import net.corda.v5.ledger.utxo.observer.UtxoTokenPoolKey;

public class GoldStateObserver implements UtxoLedgerTokenStateObserver<GoldState> {

    @Override
    public Class<GoldState> getStateType() {
        return GoldState.class;
    }

    @Override
    public UtxoToken onCommit(GoldState goldState) {
        UtxoTokenPoolKey poolKey = new UtxoTokenPoolKey(GoldState.class.getName(), goldState.getIssuer(), goldState.getSymbol());
        return new UtxoToken(poolKey, goldState.getValue(), new UtxoTokenFilterFields(null, goldState.getOwner()));
    }
}
