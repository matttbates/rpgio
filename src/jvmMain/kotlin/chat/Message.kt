package chat

data class Message(
    val senderId: Int,
    val message: String,
    val time: String,
)
