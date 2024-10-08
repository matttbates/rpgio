package common

import kotlinx.serialization.Serializable

@Serializable
data class HitBox(
    val fromLeft: Float,
    val fromTop: Float,
    val fromRight: Float,
    val fromBottom: Float
)
