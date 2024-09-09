package maps
import kotlinx.serialization.Serializable

@Serializable
data class Portal(
    val x: Int,
    val y: Int,
    val toX: Int,
    val toY: Int,
    val toMap: String? = null
)