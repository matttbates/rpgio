package maps

import Location
import World
import androidx.compose.ui.graphics.ImageBitmap
import entities.Entity
import entities.EntityDoor
import kotlinx.serialization.Serializable
import tiles.Tile

@Serializable
data class MapData(
    val name: String,
    val file: String,
    val lightMode: LightMode = LightMode.LIGHT,
    val portals: List<Portal> = emptyList()
){
    private var _rawMap: ImageBitmap? = null
    fun setRawMap(imageBitmap: ImageBitmap?){
        _rawMap = imageBitmap
    }
    val rawMap get() = _rawMap
    val tilesMaps = hashMapOf<Pair<Int, Int>, HashMap<Pair<Int, Int>, Tile>>()
    val entityMaps = hashMapOf<Pair<Int, Int>, ArrayList<Entity>>().apply {
        portals.forEach {
            val x = it.x
            val y = it.y
            getOrPut(World.getChunkCoords(x.toFloat(), y.toFloat())){ arrayListOf() }.add(EntityDoor(
                location = Location(Pair(it.x.toFloat(), it.y.toFloat()), file),
                destination = Location(Pair(it.toX.toFloat(), it.toY.toFloat()), it.toMap?:file),
            ))
        }
    }
    val spawnLocations = arrayListOf<Location>()
    val defaultTile: Tile = when (file){//todo: pull into json
        "src/jvmMain/resources/maps/0.png" -> Tile.TileWater
        else -> Tile.TileWall
    }
}
