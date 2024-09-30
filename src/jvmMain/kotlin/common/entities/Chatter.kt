package common.entities

import common.chat.Conversation
import kotlinx.serialization.Serializable

interface Chatter {
    var chatState: State

    @Serializable
    sealed class State{
        object IDLE: State()
        //Do not serialize the conversation, state should be set to IDLE when serializing
        data class TALKING(val conversation: Conversation): State()
    }
}