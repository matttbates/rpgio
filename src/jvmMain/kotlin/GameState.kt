import entities.Entity
import tiles.Tile
import entities.EntityPlayer

data class GameState(
    val playerId: Int = -1,
    val tiles: List<List<Tile>> = emptyList(),
    val entities: List<Entity> = emptyList(),
    val tick: Int = 0,
    val map: String = ""
)
