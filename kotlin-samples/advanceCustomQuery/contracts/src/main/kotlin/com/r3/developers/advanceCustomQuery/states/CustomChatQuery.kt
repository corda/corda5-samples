package com.r3.developers.advanceCustomQuery.states

import net.corda.v5.ledger.utxo.StateAndRef
import net.corda.v5.ledger.utxo.query.VaultNamedQueryCollector
import net.corda.v5.ledger.utxo.query.VaultNamedQueryFactory
import net.corda.v5.ledger.utxo.query.VaultNamedQueryStateAndRefFilter
import net.corda.v5.ledger.utxo.query.VaultNamedQueryStateAndRefTransformer
import net.corda.v5.ledger.utxo.query.registration.VaultNamedQueryBuilderFactory

class ChatCustomQueryFactory : VaultNamedQueryFactory {

    override fun create(vaultNamedQueryBuilderFactory: VaultNamedQueryBuilderFactory) {
        vaultNamedQueryBuilderFactory.create("GET_ALL_MSG") //used only in the other "simple" sample
            .whereJson(
                "WHERE visible_states.custom_representation ? 'com.r3.developers.advanceCustomQuery.states.ChatState' "
            )
            .register()

        vaultNamedQueryBuilderFactory.create("GET_MSG_FROM") //used only in the other "simple" sample
            .whereJson(
                "WHERE visible_states.custom_representation -> 'com.r3.developers.advanceCustomQuery.states.ChatState' ->> 'messageContentFrom' = :nameOfSender"
            )
            .register()
        vaultNamedQueryBuilderFactory.create("GET_MSGS_CONTAINING_HELLO")
            .whereJson(
                "WHERE visible_states.custom_representation ? 'com.r3.developers.advanceCustomQuery.states.ChatState' "
            )
            .filter(CustomQueryFilter()) //applies the filter to this query
            .register()

        vaultNamedQueryBuilderFactory.create("GET_ALL_MSGS_CONTENT")
            .whereJson(
                "WHERE visible_states.custom_representation ? 'com.r3.developers.advanceCustomQuery.states.ChatState' "
            )
            .map(CustomQueryTransformer()) //applies the transformer to this query
            .register()

        vaultNamedQueryBuilderFactory.create("GET_MSG_AMOUNT")
            .whereJson(
                "WHERE visible_states.custom_representation ? 'com.r3.developers.advanceCustomQuery.states.ChatState' "
            )
            .collect(CustomQueryCollector()) //applies the collector to this query
            .register()

        vaultNamedQueryBuilderFactory.create("GET_MSG_AMOUNT_HAS_HELLO")
            .whereJson(
                "WHERE visible_states.custom_representation ? 'com.r3.developers.advanceCustomQuery.states.ChatState' "
            )
            .filter(CustomQueryFilter()) //applies the filter to this query
            .collect(CustomQueryCollector()) //applies the collector after the filter has been applied
            .register()

        vaultNamedQueryBuilderFactory.create("GET_ALL_MSGS_CONTENT_HAS_HELLO")
            .whereJson(
                "WHERE visible_states.custom_representation ? 'com.r3.developers.advanceCustomQuery.states.ChatState' "
            )
            .filter(CustomQueryFilter()) //applies the filter to this query
            .map(CustomQueryTransformer()) //applies the transformer after the filter has been applied
            .register()
    }
}


// for filtering states of type ChatState.
class CustomQueryFilter : VaultNamedQueryStateAndRefFilter<ChatState> {
    override fun filter(data: StateAndRef<ChatState>, parameters: MutableMap<String, Any>): Boolean {
        return data.state.contractState.message.lowercase().contains("hello")     // It returns true if the 'message' field in the state's contractState is lowercase and contains "hello".
    } //return type is a boolean that will be checking for which entry passes and which fails. The query return type is still a StateAndRef
}


// for transforming states of type ChatState into a String.
class CustomQueryTransformer : VaultNamedQueryStateAndRefTransformer<ChatState, String> {
    // It returns the 'message' field from the state's contractState.
    override fun transform(data: StateAndRef<ChatState>, parameters: MutableMap<String, Any>): String {
        return data.state.contractState.message
    } //return type is a String. This also alters the return type for the query to a String. Ensure that the query serializes the result to String
}


// to collect transformed data into a final result. It operates on String inputs and produces an Int result.
class CustomQueryCollector : VaultNamedQueryCollector<String, Int> {
    // It returns a result containing the size of the resultSet
    override fun collect(resultSet: MutableList<String>, parameters: MutableMap<String, Any>):
        VaultNamedQueryCollector.Result<Int> {
            return VaultNamedQueryCollector.Result(listOf(resultSet.size), true)
   }
    //return type is a int. This also alters the return type for the query to a int. Ensure that the query serializes the result to integer

}
