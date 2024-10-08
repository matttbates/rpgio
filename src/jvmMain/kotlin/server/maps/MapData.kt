package server.maps

import common.Location
import common.entities.Entity
import kotlinx.serialization.Serializable
import common.tiles.Tile

@Serializable
data class MapData(
    val name: String,
    val file: String,
    val lightMode: LightMode = LightMode.LIGHT,
    val defaultTile: Tile = Tile.Wall,
){
    private var _rawMap: RawMapJson? = null
    fun setRawMap(rawMap: RawMapJson?){
        _rawMap = rawMap
    }
    val rawMap get() = _rawMap
    val tilesMaps = hashMapOf<Pair<Int, Int>, HashMap<Pair<Int, Int>, Tile>>()
    val entityMaps = hashMapOf<Pair<Int, Int>, ArrayList<Entity>>()
    val spawnLocations = arrayListOf<Location>()
}
