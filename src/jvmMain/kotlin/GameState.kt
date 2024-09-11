import chat.Conversation
import entities.Entity
import tiles.Tile

data class GameState(
    val playerId: Int = -1,
    val location: Location = Location(0f to 0f, ""),
    val tiles: List<List<Tile>> = emptyList(),
    val entities: List<Entity> = emptyList(),
    val tick: Int = 0,
    val lightLevel: Float = 1.0f,
    val time: String = "",
)
