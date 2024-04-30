package com.r3.developers.cordapptemplate.customJSON.states

import net.corda.v5.ledger.utxo.query.VaultNamedQueryFactory
import net.corda.v5.ledger.utxo.query.registration.VaultNamedQueryBuilderFactory

class ChatCustomQueryFactory : VaultNamedQueryFactory {
    override fun create(vaultNamedQueryBuilderFactory: VaultNamedQueryBuilderFactory) {
        vaultNamedQueryBuilderFactory.create("GET_ALL_MSG")
            .whereJson(
                "WHERE visible_states.custom_representation ? 'com.r3.developers.cordapptemplate.utxoexample.states.ChatState' "
            )
            .register()

        vaultNamedQueryBuilderFactory.create("GET_MSG_FROM")
            .whereJson(
                "WHERE visible_states.custom_representation -> 'com.r3.developers.cordapptemplate.utxoexample.states.ChatState' ->> 'messageContentFrom' = :nameOfSender"
            )
            .register()
    }
}