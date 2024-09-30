package server.maps

import kotlinx.serialization.Serializable

@Serializable
data class RawMapJson(
    val nw: List<List<Int>> = emptyList(),
    val ne: List<List<Int>> = emptyList(),
    val sw: List<List<Int>> = emptyList(),
    val se: List<List<Int>> = emptyList(),
)
