package tiles

class TileWall : Tile {
    override val appearance: Char = '='
    override val sprite: String = "wall"
    override val isSolid: Boolean = true
}