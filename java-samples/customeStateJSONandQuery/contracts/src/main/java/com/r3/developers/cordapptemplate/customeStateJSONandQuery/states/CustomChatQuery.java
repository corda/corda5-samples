package com.r3.developers.cordapptemplate.customeStateJSONandQuery.states;

import net.corda.v5.ledger.utxo.query.VaultNamedQueryFactory;
import net.corda.v5.ledger.utxo.query.registration.VaultNamedQueryBuilderFactory;
import org.jetbrains.annotations.NotNull;

public class CustomChatQuery implements VaultNamedQueryFactory {


    @Override
    public void create(@NotNull VaultNamedQueryBuilderFactory vaultNamedQueryBuilderFactory) {

        vaultNamedQueryBuilderFactory.create("GET_ALL_MSG")
                .whereJson(
                        "WHERE visible_states.custom_representation ? 'com.r3.developers.cordapptemplate.utxoexample.states.ChatState' "
                )
                .register();

        vaultNamedQueryBuilderFactory.create("GET_MSG_FROM")
                .whereJson(
                        "WHERE visible_states.custom_representation -> 'com.r3.developers.cordapptemplate.utxoexample.states.ChatState' ->> 'messageContentFrom' = :nameOfSender"
                )
                .register();
    }
}
