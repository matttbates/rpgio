import entities.Entity
import tiles.Tile

data class MapData(
    val map: String
){
    val rawMap = createBitmapFromFile(map)
    val tilesMaps = hashMapOf<Pair<Int, Int>, HashMap<Pair<Int, Int>, Tile>>()
    val entityMaps = hashMapOf<Pair<Int, Int>, ArrayList<Entity>>()
    val spawnLocations = arrayListOf<Location>()
}
