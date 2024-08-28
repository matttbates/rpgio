package tiles

class TileWater : Tile {
    override val appearance: Char = '~'
    override val sprite: String = "water"
    override val isSolid: Boolean = true
}