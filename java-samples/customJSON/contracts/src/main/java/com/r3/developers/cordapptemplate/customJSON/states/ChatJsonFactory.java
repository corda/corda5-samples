package com.r3.developers.cordapptemplate.customJSON.states;

import net.corda.v5.application.marshalling.JsonMarshallingService;
import net.corda.v5.ledger.utxo.query.json.ContractStateVaultJsonFactory;

import java.util.HashMap;
import java.util.Map;

// This class represents a custom JSON factory for serializing/deserializing ChatState objects to JSON format after each use.
public class ChatJsonFactory implements ContractStateVaultJsonFactory<ChatState> {

    // This function specifies the type of state this factory is responsible for.
    @Override
    public Class<ChatState> getStateType() {
        return ChatState.class;
    }

    // Constants used for JSON key names.
    private static final String ID = "Id";
    private static final String CHATNAME = "chatName";
    private static final String MESSAGE = "messageContent";
    private static final String MESSAGEFROM = "messageContentFrom";

    // This function creates a JSON representation of a ChatState object.
    @Override
    public String create(ChatState state, JsonMarshallingService jsonMarshallingService) {
        // Constructs a map representing the ChatState object using the provided constants.
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put(ID, state.getId());
        jsonMap.put(CHATNAME, state.getChatName());
        jsonMap.put(MESSAGE, state.getMessage());
        jsonMap.put(MESSAGEFROM, state.getMessageFrom());

        // Uses the JsonMarshallingService's format() function to serialize the map to JSON.
        return jsonMarshallingService.format(jsonMap);
    }
}
