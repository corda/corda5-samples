package com.r3.developers.samples.tokens.states;

import net.corda.v5.application.crypto.DigestService;
import net.corda.v5.ledger.utxo.observer.UtxoLedgerTokenStateObserver;
import net.corda.v5.ledger.utxo.observer.UtxoToken;
import net.corda.v5.ledger.utxo.observer.UtxoTokenFilterFields;
import net.corda.v5.ledger.utxo.observer.UtxoTokenPoolKey;
import org.jetbrains.annotations.NotNull;

/*
By implementing the UtxoLedgerTokenStateObserver, this observer will generate fungible states/tokens for
each produced gold state when persisting a finalized transaction to the vault.
 */
public class GoldStateObserver implements UtxoLedgerTokenStateObserver<GoldState> {

    @Override
    public Class<GoldState> getStateType() {
        return GoldState.class;
    }

    @NotNull
    @Override
    public UtxoToken onCommit(@NotNull GoldState state, @NotNull DigestService digestService) {
        //generate a pool with key - type, issuer and symbol to mint the tokens
        UtxoTokenPoolKey poolKey = new UtxoTokenPoolKey(GoldState.class.getName(), state.getIssuer(), state.getSymbol());
        return new UtxoToken(poolKey, state.getAmount(), new UtxoTokenFilterFields(null, state.getOwner()));
    }
}
