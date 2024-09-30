package server

import kotlinx.serialization.Serializable

@Serializable
data class WorldJson(
    val tick: Int? = null,
)
