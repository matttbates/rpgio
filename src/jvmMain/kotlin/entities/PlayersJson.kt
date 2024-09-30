package entities

import kotlinx.serialization.Serializable

@Serializable
data class PlayersJson(
    val players: List<EntityPlayer>
)