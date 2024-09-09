package maps

import Location
import createBitmapFromFile
import entities.Entity
import entities.EntityDoor
import kotlinx.serialization.Serializable
import tiles.Tile
import tiles.TileWall
import tiles.TileWater

@Serializable
data class MapData(
    val name: String,
    val file: String,
    val lightMode: LightMode = LightMode.LIGHT,
    val portals: List<Portal> = emptyList()
){
    val rawMap = createBitmapFromFile(file)
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
    val defaultTile: Tile = when (file){
        "src/jvmMain/resources/maps/0.png" -> TileWater()
        else -> TileWall()
    }
}
