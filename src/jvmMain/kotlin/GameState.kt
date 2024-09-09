import entities.Entity
import tiles.Tile

data class GameState(
    val playerId: Int = -1,
    val tiles: List<List<Tile>> = emptyList(),
    val entities: List<Entity> = emptyList(),
    val tick: Int = 0,
    val map: String = "",
    val lightLevel: Float = 1.0f,
    val time: String = "00:00",
)
