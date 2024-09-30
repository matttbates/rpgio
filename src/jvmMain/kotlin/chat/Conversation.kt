package chat

import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
    val participants: List<Int>,
    val messages: ArrayList<Message>,
){
    fun addMessage(message: Message) {
        messages.add(message)
    }
}
