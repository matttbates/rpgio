import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val coords: Pair<Float, Float>,
    val map: String
)
