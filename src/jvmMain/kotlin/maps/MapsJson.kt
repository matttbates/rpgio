package maps

import kotlinx.serialization.Serializable

@Serializable
data class MapsJson(
    val maps: List<MapJson>
)
