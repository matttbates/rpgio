package common.chat

import server.FileIO
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ChatManager(
    private val fileIO: FileIO
) {
    private val json = Json {
        prettyPrint = true
    }

    fun getConversationByIds(playerId: Int, otherId: Int): Conversation {
        val firstId = minOf(playerId, otherId)
        val secondId = maxOf(playerId, otherId)
        val fileName = "src/jvmMain/resources/conversations/$firstId-$secondId.json"
        val conversationJson = fileIO.readTextFile(fileName)
        return if (conversationJson != null) {
            json.decodeFromString<Conversation>(conversationJson)
        } else {
            Conversation(listOf(playerId, otherId), arrayListOf())
        }
    }

    fun saveConversation(conversation: Conversation) {
        val firstId = conversation.participants.min()
        val secondId = conversation.participants.max()
        val fileName = "src/jvmMain/resources/conversations/$firstId-$secondId.json"
        val conversationJson = json.encodeToString(conversation)
        fileIO.writeTextFile(fileName, conversationJson)
    }

}