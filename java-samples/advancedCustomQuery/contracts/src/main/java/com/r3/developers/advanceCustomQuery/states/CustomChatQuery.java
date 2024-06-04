package com.r3.developers.advanceCustomQuery.states;

import net.corda.v5.ledger.utxo.StateAndRef;
import net.corda.v5.ledger.utxo.query.VaultNamedQueryCollector;
import net.corda.v5.ledger.utxo.query.VaultNamedQueryFactory;
import net.corda.v5.ledger.utxo.query.VaultNamedQueryStateAndRefFilter;
import net.corda.v5.ledger.utxo.query.VaultNamedQueryStateAndRefTransformer;
import net.corda.v5.ledger.utxo.query.registration.VaultNamedQueryBuilderFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class CustomChatQuery implements VaultNamedQueryFactory {


    @Override
    public void create(@NotNull VaultNamedQueryBuilderFactory vaultNamedQueryBuilderFactory) {

        //Returns all the chat states that stores in the vNode
        vaultNamedQueryBuilderFactory.create("GET_ALL_MSG")
                .whereJson(
                        "WHERE visible_states.custom_representation ? 'com.r3.developers.advanceCustomQuery.states.ChatState' "
                )
                .register();

        //Returns all the chat states that stores in the vNode that were sent from a particular sender.
        //(not used in this sample app, used in the basic custom query app)
        vaultNamedQueryBuilderFactory.create("GET_MSG_FROM")
                .whereJson(
                        "WHERE visible_states.custom_representation -> 'com.r3.developers.advanceCustomQuery.states.ChatState' ->> 'messageContentFrom' = :nameOfSender"
                )
                .register();

        //Returns all the chat states that their messages contains the word "hello" via filter feature
        vaultNamedQueryBuilderFactory.create("GET_MSGS_CONTAINING_HELLO")
                .whereJson(
                        "WHERE visible_states.custom_representation ? 'com.r3.developers.advanceCustomQuery.states.ChatState' "
                )
                .filter(new CustomQueryFilter()) //applies the filter to this query
                .register();

        //Returns only the chat messages of all chat entries stored in the vNode via transformer feature
        vaultNamedQueryBuilderFactory.create("GET_ALL_MSGS_CONTENT")
                .whereJson(
                        "WHERE visible_states.custom_representation ? 'com.r3.developers.advanceCustomQuery.states.ChatState' "
                )
                .map(new CustomQueryTransformer()) //applies the transformer to this query
                .register();

        //Returns the total amount of chat entries stored in the vNode via collector feature
        vaultNamedQueryBuilderFactory.create("GET_MSG_AMOUNT")
                .whereJson(
                        "WHERE visible_states.custom_representation ? 'com.r3.developers.advanceCustomQuery.states.ChatState' "
                )
                .collect(new CustomQueryCollector()) //applies the collector to this query
                .register();

        //Returns the total amount of chat entries that has the word "hello" in the chat message
        //via filter + collector feature
        vaultNamedQueryBuilderFactory.create("GET_MSG_AMOUNT_HAS_HELLO")
                .whereJson(
                        "WHERE visible_states.custom_representation ? 'com.r3.developers.advanceCustomQuery.states.ChatState' "
                )
                .filter(new CustomQueryFilter()) //applies the filter to this query
                .collect(new CustomQueryCollector()) //applies the collector after the filter has been applied
                .register();

        //Returns only the chat messages of all chat entries that has the word "hello" in the chat message
        //via filter + transformer feature
        vaultNamedQueryBuilderFactory.create("GET_ALL_MSGS_CONTENT_HAS_HELLO")
                .whereJson(
                        "WHERE visible_states.custom_representation ? 'com.r3.developers.advanceCustomQuery.states.ChatState' "
                )
                .filter(new CustomQueryFilter()) //applies the filter to this query
                .map(new CustomQueryTransformer()) //applies the transformer after the filter has been applied
                .register();
    }
}

//Filtering states of type ChatState.
class CustomQueryFilter implements VaultNamedQueryStateAndRefFilter<ChatState> {
    @NotNull
    @Override
    // It returns true if the 'message' field in the state's contractState is lowercase and contains "hello".
    public Boolean filter(@NotNull StateAndRef<ChatState> data, @NotNull Map<String, Object> parameters) {
        return data.getState().getContractState().getMessage().toLowerCase().contains("hello");
    }
}

//Transforming states of type ChatState into a String.
class CustomQueryTransformer implements VaultNamedQueryStateAndRefTransformer<ChatState, String> {
    @NotNull
    @Override
    // It returns the 'message' field from the state's contractState.
    public String transform(@NotNull StateAndRef<ChatState> data, @NotNull Map<String, Object> parameters) {
        return data.getState().getContractState().getMessage();
    }
}

//Collect resultSet data into a final result. It operates on String inputs and produces an Int result.
class CustomQueryCollector implements VaultNamedQueryCollector<String, Integer> {
    @NotNull
    @Override
    // It returns a result containing the size of the resultSet
    public Result<Integer> collect(@NotNull List<String> resultSet, @NotNull Map<String, Object> parameters) {
        return new Result<>(
                List.of(resultSet.size()),
                true
        );
    }
}

