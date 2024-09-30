package common.tiles

enum class Tile(
    val sprite: String,
    val isSolid: Boolean,
    val inFrontOf: Tile? = null
) {
    Grass(
        sprite = "grass",
        isSolid = false
    ),
    Path(
        sprite = "path",
        isSolid = false
    ),
    Spawner(
        sprite = "spawner",
        isSolid = false
    ),
    Wall(
        sprite = "wall",
        isSolid = true
    ),
    Water(
        sprite = "water",
        isSolid = true
    ),
    TreeTrunk(
        sprite = "tree_trunk",
        isSolid = true
    ),
    TreeTop(
        sprite = "tree_top",
        isSolid = false,
        inFrontOf = Grass
    ),
    TreeTopDense(
        sprite = "tree_top",
        isSolid = true,
        inFrontOf = TreeTrunk
    ),
    BuildingBottomLeft(
        sprite = "building_bottom_left",
        isSolid = true
    ),
    BuildingBottom(
        sprite = "building_bottom",
        isSolid = true
    ),
    BuildingBottomRight(
        sprite = "building_bottom_right",
        isSolid = true
    ),
    Flowers(
        sprite = "flowers",
        isSolid = false
    ),
    BuildingTopLeft(
        sprite = "building_top_left",
        isSolid = false,
        inFrontOf = Grass
    ),
    BuildingTop(
        sprite = "building_top",
        isSolid = false,
        inFrontOf = Grass
    ),
    BuildingTopRight(
        sprite = "building_top_right",
        isSolid = false,
        inFrontOf = Grass
    ),
    BuildingMiddleLeft(
        sprite = "building_middle_left",
        isSolid = true,
        inFrontOf = Grass
    ),
    BuildingMiddle(
        sprite = "building_middle",
        isSolid = true
    ),
    BuildingMiddleRight(
        sprite = "building_middle_right",
        isSolid = true,
        inFrontOf = Grass
    ),
    Sand(
        sprite = "sand",
        isSolid = false
    );
    companion object {
        fun getById(id: Int): Tile? {
            return values().firstOrNull { it.ordinal == id }
        }
    }
}