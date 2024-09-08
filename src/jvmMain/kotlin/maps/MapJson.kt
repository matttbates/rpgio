package maps

import kotlinx.serialization.Serializable

@Serializable
data class MapJson(
    val name: String,
    val file: String,
    val lightMode: String
)
