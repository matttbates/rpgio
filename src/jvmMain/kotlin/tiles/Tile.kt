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
    ),
    TileBuildingBottomLeft(
        sprite = "building_bottom_left",
        isSolid = true
    ),
    TileBuildingBottom(
        sprite = "building_bottom",
        isSolid = true
    ),
    TileBuildingBottomRight(
        sprite = "building_bottom_right",
        isSolid = true
    ),
    TileFlowers(
        sprite = "flowers",
        isSolid = false
    ),
    TileBuildingTopLeft(
        sprite = "building_top_left",
        isSolid = false,
        inFrontOf = TileGrass
    ),
    TileBuildingTop(
        sprite = "building_top",
        isSolid = false,
        inFrontOf = TileGrass
    ),
    TileBuildingTopRight(
        sprite = "building_top_right",
        isSolid = false,
        inFrontOf = TileGrass
    ),
    TileBuildingMiddleLeft(
        sprite = "building_middle_left",
        isSolid = true,
        inFrontOf = TileGrass
    ),
    TileBuildingMiddle(
        sprite = "building_middle",
        isSolid = true
    ),
    TileBuildingMiddleRight(
        sprite = "building_middle_right",
        isSolid = true,
        inFrontOf = TileGrass
    ),
    TileSand(
        sprite = "sand",
        isSolid = false
    );
    companion object {
        fun getById(id: Int): Tile? {
            return values().firstOrNull { it.ordinal == id }
        }
    }
}