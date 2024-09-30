package common

import common.entities.Entity
import common.tiles.Tile

data class GameState(
    val playerId: Int = -1,
    val location: Location = Location(Coords(0f, 0f), ""),
    val tiles: List<List<Tile>> = emptyList(),
    val entities: List<Entity> = emptyList(),
    val tick: Int = 0,
    val lightLevel: Float = 1.0f,
    val time: String = "",
)
