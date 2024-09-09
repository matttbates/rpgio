package chat

class ChatManager {
    private val conversations = mutableListOf<Conversation>()

    fun getConversationByIds(playerId: Int, otherId: Int): Conversation = conversations.find {
        it.participants.contains(playerId) && it.participants.contains(otherId)
    }?: Conversation(listOf(playerId, otherId), arrayListOf()).also { conversations.add(it) }

}