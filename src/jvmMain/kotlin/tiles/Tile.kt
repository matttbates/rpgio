package tiles

enum class Tile(
    val id: Int,
    val sprite: String,
    val isSolid: Boolean,
) {
    TileGrass(
        id = 0xFF00FF00.toInt(),
        sprite = "grass",
        isSolid = false
    ),
    TilePath(
        id = 0xFF00FFFF.toInt(),
        sprite = "path",
        isSolid = false
    ),
    TileSpawner(
        id = 0xFFFF0000.toInt(),
        sprite = "spawner",
        isSolid = false
    ),
    TileWall(
        id = 0xFF000000.toInt(),
        sprite = "wall",
        isSolid = true
    ),
    TileWater(
        id = 0xFF0000FF.toInt(),
        sprite = "water",
        isSolid = true
    );
    companion object {
        fun getById(id: Int): Tile? {
            return values().find { it.id == id }
        }
    }
}