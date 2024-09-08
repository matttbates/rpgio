import World.Companion.STARTING_HOUR
import World.Companion.TICKS_PER_DAY
import entities.Entity
import tiles.Tile
import entities.EntityPlayer

data class GameState(
    val playerId: Int = -1,
    val tiles: List<List<Tile>> = emptyList(),
    val entities: List<Entity> = emptyList(),
    val tick: Int = (TICKS_PER_DAY * (STARTING_HOUR / 24f)).toInt(),
    val map: String = "",
    val lightLevel: Float = 1.0f,
)
