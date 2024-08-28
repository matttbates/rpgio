package tiles

class TileGrass : Tile {
    override val appearance: Char = ' '
    override val sprite: String = "grass"
    override val isSolid: Boolean = false
}