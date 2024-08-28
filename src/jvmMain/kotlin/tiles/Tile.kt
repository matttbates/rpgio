package tiles

interface Tile {
    val appearance: Char
    val sprite: String
    val isSolid: Boolean
}