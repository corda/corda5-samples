package com.r3.developers.advanceCustomQuery.states

import net.corda.v5.ledger.utxo.StateAndRef
import net.corda.v5.ledger.utxo.query.VaultNamedQueryCollector
import net.corda.v5.ledger.utxo.query.VaultNamedQueryFactory
import net.corda.v5.ledger.utxo.query.VaultNamedQueryStateAndRefFilter
import net.corda.v5.ledger.utxo.query.VaultNamedQueryStateAndRefTransformer
import net.corda.v5.ledger.utxo.query.registration.VaultNamedQueryBuilderFactory

class ChatCustomQueryFactory : VaultNamedQueryFactory {

    override fun create(vaultNamedQueryBuilderFactory: VaultNamedQueryBuilderFactory) {
        vaultNamedQueryBuilderFactory.create("GET_ALL_MSG")
            .whereJson(
                "WHERE visible_states.custom_representation ? 'com.r3.developers.advanceCustomQuery.states.ChatState' "
            )
            .register()

        vaultNamedQueryBuilderFactory.create("GET_MSG_FROM")
            .whereJson(
                "WHERE visible_states.custom_representation -> 'com.r3.developers.advanceCustomQuery.states.ChatState' ->> 'messageContentFrom' = :nameOfSender"
            )
            .register()
        vaultNamedQueryBuilderFactory.create("GET_MSGS_CONTAINING_HELLO")
            .whereJson(
                "WHERE visible_states.custom_representation ? 'com.r3.developers.advanceCustomQuery.states.ChatState' "
            )
            .filter(CustomQueryFilter())
            .register()

        vaultNamedQueryBuilderFactory.create("GET_ALL_MSGS_CONTENT")
            .whereJson(
                "WHERE visible_states.custom_representation ? 'com.r3.developers.advanceCustomQuery.states.ChatState' "
            )
            .map(CustomQueryTransformer())
            .register()

        vaultNamedQueryBuilderFactory.create("GET_MSG_AMOUNT")
            .whereJson(
                "WHERE visible_states.custom_representation ? 'com.r3.developers.advanceCustomQuery.states.ChatState' "
            )
            .collect(CustomQueryCollector())
            .register()

        vaultNamedQueryBuilderFactory.create("GET_MSG_AMOUNT_HAS_HELLO")
            .whereJson(
                "WHERE visible_states.custom_representation ? 'com.r3.developers.advanceCustomQuery.states.ChatState' "
            )
            .filter(CustomQueryFilter())
            .collect(CustomQueryCollector())
            .register()

        vaultNamedQueryBuilderFactory.create("GET_ALL_MSGS_CONTENT_HAS_HELLO")
            .whereJson(
                "WHERE visible_states.custom_representation ? 'com.r3.developers.advanceCustomQuery.states.ChatState' "
            )
            .filter(CustomQueryFilter())
            .map(CustomQueryTransformer())
            .register()
    }
}

class CustomQueryFilter : VaultNamedQueryStateAndRefFilter<ChatState> {
    override fun filter(data: StateAndRef<ChatState>, parameters: MutableMap<String, Any>): Boolean {
        return data.state.contractState.message.lowercase().contains("hello")
    }
}

class CustomQueryTransformer : VaultNamedQueryStateAndRefTransformer<ChatState, String> {
    override fun transform(data: StateAndRef<ChatState>, parameters: MutableMap<String, Any>): String {
        return data.state.contractState.message
    }
}

class CustomQueryCollector : VaultNamedQueryCollector<String, Int> {
    override fun collect(
        resultSet: MutableList<String>,
        parameters: MutableMap<String, Any>
    ): VaultNamedQueryCollector.Result<Int> {
        return VaultNamedQueryCollector.Result(
            listOf(resultSet.size),
            true
        )
    }
}