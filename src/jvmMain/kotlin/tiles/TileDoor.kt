package tiles

import Location

class TileDoor(
    val destination: Location
) : Tile {
    override val sprite: String = "door"
    override val isSolid: Boolean = false
}