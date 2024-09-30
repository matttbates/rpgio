package common.chat

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val senderId: Int,
    val message: String,
    val time: String,
)
