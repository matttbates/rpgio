package server.maps

import kotlinx.serialization.Serializable

@Serializable
data class MapsJson(
    val maps: List<MapData>
)
