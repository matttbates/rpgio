package chat

data class Conversation(
    val participants: List<Int>,
    val messages: ArrayList<Message>,
){
    fun addMessage(message: Message) {
        messages.add(message)
    }
}
