package com.r3.developers.customStateJSONandQuery.states

import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.ledger.utxo.query.json.ContractStateVaultJsonFactory

// This class represents a custom JSON factory for serializing/deserializing ChatState objects to JSON format after each use.
class ChatJsonFactory : ContractStateVaultJsonFactory<ChatState> {

    // This function specifies the type of state this factory is responsible for.
    override fun getStateType(): Class<ChatState> = ChatState::class.java

    // Companion object containing constants used for JSON key names.
    companion object {
        const val ID = "Id"
        const val CHATNAME = "chatName"
        const val MESSAGE = "messageContent"
        const val MESSAGEFROM = "messageContentFrom"
    }

    // This function creates a JSON representation of a ChatState object.
    override fun create(state: ChatState, jsonMarshallingService: JsonMarshallingService): String {

        // Constructs a map representing the ChatState object using the provided constants.
        val jsonMap = mapOf(
            Pair(ID, state.id),
            Pair(CHATNAME, state.chatName),
            Pair(MESSAGE, state.message),
            Pair(MESSAGEFROM, state.messageFrom)
        )

        // Uses the JsonMarshallingService's format() function to serialize the map to JSON.
        return jsonMarshallingService.format(jsonMap)
    }
}
