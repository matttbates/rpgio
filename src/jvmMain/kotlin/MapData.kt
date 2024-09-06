import entities.Entity
import tiles.Tile
import tiles.TileWall
import tiles.TileWater

data class MapData(
    val map: String,
){
    val rawMap = createBitmapFromFile(map)
    val tilesMaps = hashMapOf<Pair<Int, Int>, HashMap<Pair<Int, Int>, Tile>>()
    val entityMaps = hashMapOf<Pair<Int, Int>, ArrayList<Entity>>()
    val spawnLocations = arrayListOf<Location>()
    val defaultTile: Tile = when (map){
        "src/jvmMain/resources/maps/0.png" -> TileWater()
        else -> TileWall()
    }
}
