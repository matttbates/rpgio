package tiles

enum class Tile(
    val sprite: String,
    val isSolid: Boolean,
    val inFrontOf: Tile? = null
) {
    TileGrass(
        sprite = "grass",
        isSolid = false
    ),
    TilePath(
        sprite = "path",
        isSolid = false
    ),
    TileSpawner(
        sprite = "spawner",
        isSolid = false
    ),
    TileWall(
        sprite = "wall",
        isSolid = true
    ),
    TileWater(
        sprite = "water",
        isSolid = true
    ),
    TileTreeTrunk(
        sprite = "tree_trunk",
        isSolid = true
    ),
    TileTreeTop(
        sprite = "tree_top",
        isSolid = false,
        inFrontOf = TileGrass
    ),
    TileTreeTopDense(
        sprite = "tree_top",
        isSolid = true,
        inFrontOf = TileTreeTrunk
    );
    companion object {
        fun getById(id: Int): Tile? {
            return values().firstOrNull { it.ordinal == id }
        }
    }
}