package common

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val coords: Coords,
    val map: String
)

@Serializable
data class Coords(
    val x: Float,
    val y: Float
)