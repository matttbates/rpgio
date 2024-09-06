import entities.Entity
import entities.EntityPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import tiles.*
import java.nio.file.Files
import java.nio.file.Paths

class World {

    companion object {
        private val maps: MutableMap<String, MapData> = hashMapOf()//"src/jvmMain/resources/maps/map.png" to MapData("src/jvmMain/resources/maps/map.png"))
        private const val CHUNK_SIZE = 20
        const val TPS = 20
    }

    private var displaySize = Pair(0, 0)
    fun getDisplaySize() = displaySize

    private val clientStates = arrayListOf<MutableStateFlow<GameState>>()
    private val pendingActions = hashMapOf<Int, ArrayList<Action>>()

    private fun getAllMaps(): List<String>{
        val projectDirAbsolutePath = Paths.get("").toAbsolutePath().toString()
        val resourcesPath = Paths.get(projectDirAbsolutePath, "/src/jvmMain/resources/maps")
        val list = arrayListOf<String>()
        Files.walk(resourcesPath)
            .filter { item -> Files.isRegularFile(item) }
            .filter { item -> item.toString().endsWith(".png") }
            .forEach { item ->
                list.add("src/jvmMain/resources/maps/${item.toFile().name}")
            }
        return list
    }

    init {
        getAllMaps().forEach { map ->
            maps[map] = MapData(map)
        }
        maps.values.forEach { mapData ->
            mapData.rawMap?.let { imageBitmap ->
                val buffer = IntArray(imageBitmap.width * imageBitmap.height)
                imageBitmap.readPixels(buffer)
                for (y in 0 until imageBitmap.height) {
                    for (x in 0 until imageBitmap.width) {
                        val pixel = buffer[y * imageBitmap.width + x]
                        when {
                            pixel == 0xFF00FF00.toInt() -> TileGrass()
                            pixel == 0xFF0000FF.toInt() -> TileWater()
                            pixel == 0xFF00FFFF.toInt() -> TilePath()
                            pixel == 0xFFFF0000.toInt() -> TileSpawner()
                            pixel == 0xFF000000.toInt() -> TileWall()
                            //check if starts with 0xee
                            pixel and 0xFF000000.toInt() == 0xEE000000.toInt() -> {
                                //mapNumber = pixel and 0x00FF0000
                                val mapNumber: Int = (pixel and 0x00FF0000) shr 16
                                val destX: Int = (pixel and 0x0000FF00) shr 8
                                val destY: Int = pixel and 0x000000FF
                                println("door to map $mapNumber at $destX, $destY")
                                TileDoor(
                                    destination = Location(
                                        coords = destX.toFloat() to destY.toFloat(),
                                        map = maps["src/jvmMain/resources/maps/$mapNumber.png"]?.map ?: ""
                                    )
                                )
                            }
                            else -> null
                        }?.let { tile ->
                            setTile(Location(
                                coords = x.toFloat() to y.toFloat(),
                                map = mapData.map
                            ), tile)
                            if (tile is TileSpawner) {
                                mapData.spawnLocations.add(
                                    Location(
                                        coords = Pair(x.toFloat(), y.toFloat()),
                                        map = mapData.map
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun start(
        rX: Int = 12,
        rY: Int = 4
    ) {
        displaySize = Pair(rX * 2 + 1, rY * 2 + 1)
        CoroutineScope(Dispatchers.IO).launch {
            var tick = 0
            val sleepMillis = 1000 / TPS
            while (true) {
                clientStates.forEach {
                    //get player
                    val playerId = it.value.playerId
                    //perform actions
                    val actions = pendingActions[playerId] ?: arrayListOf()
                    pendingActions[playerId] = arrayListOf()
                    actions.forEach { action ->
                        performAction(playerId, action)
                    }
                    //update state
                    val player = getPlayer(playerId) ?: return@forEach
                    val (x, y) = player.location.coords.let { (x, y) -> Pair(x.toInt(), y.toInt()) }
                    val from = Pair(x - rX, y - rY)
                    val to = Pair(x + rX, y + rY)
                    val tiles = getTopTiles(from, to, player.location.map)
                    val entities = getEntities(from, to, player.location.map)
                    it.value = GameState(
                        playerId = playerId,
                        tiles = tiles,
                        entities = entities,
                        tick = tick,
                        map = player.location.map
                    )
                }
                tick++
                Thread.sleep(sleepMillis.toLong())
            }
        }
    }

    private fun performAction(playerId: Int, action: Action){
        when(action){
            is Action.MovePlayer -> movePlayer(playerId, action)
            is Action.RotateEntity -> rotateEntity(action)
            Action.Interact -> interactBy(playerId)
        }
    }

    fun enqueueAction(playerId: Int, action: Action){
        pendingActions[playerId] = (pendingActions[playerId] ?: arrayListOf()).apply {
            add(action)
        }
    }

    fun connect(playerId: Int): Flow<GameState> {
        val player = getOrSpawnPlayerEntity(playerId)//for later use
        val state = MutableStateFlow(GameState(
            playerId = playerId,
            tiles = listOf(),
            tick = 0
        ))
        clientStates.add(state)
        return state
    }

    private fun getOrSpawnPlayerEntity(playerId: Int): EntityPlayer {
        return getPlayer(playerId) ?: spawnNewPlayer(playerId) ?: throw Exception("No spawn locations available")
    }

    fun disconnect(playerId: Int) {
        clientStates.removeIf { it.value.playerId == playerId }
    }

    fun isConnected(playerId: Int): Boolean {
        return clientStates.any { it.value.playerId == playerId }
    }

    private fun spawnNewPlayer(id: Int): EntityPlayer? {
        val emptySpawns = maps.values.flatMap { mapData ->
            mapData.spawnLocations.filter { location ->
                getEntities(location).isEmpty()
            }
        }
        if (emptySpawns.isEmpty()) {
            return null
        }
        val entityPlayer = EntityPlayer(
            id = id,
            location = emptySpawns.random()
        )
        setEntity(entityPlayer)
        return entityPlayer
    }

    private fun checkSign(num: Int) = (num shr 31 or 1)

    private fun getChunkCoords(x: Float, y: Float): Pair<Int, Int>{
        val qX = checkSign(x.toInt())
        val qY = checkSign(y.toInt())
        return Pair((x.toInt() / CHUNK_SIZE) + qX, (y.toInt() / CHUNK_SIZE) + qY)
    }

    private fun setTile(location: Location, tile: Tile){
        val (x, y) = location.coords
        val (chunkX, chunkY) = getChunkCoords(x, y)
        maps[location.map]?.apply {
            tilesMaps[Pair(chunkX, chunkY)] = (tilesMaps[Pair(chunkX, chunkY)] ?: hashMapOf()).apply {
                this[Pair(x.toInt(), y.toInt())] = tile
            }
        }
    }

    private fun setEntity(entity: Entity){
        val (x, y) = entity.location.coords
        val (chunkX, chunkY) = getChunkCoords(x, y)
        maps[entity.location.map]?.apply {
            entityMaps[Pair(chunkX, chunkY)] = (entityMaps[Pair(chunkX, chunkY)] ?: arrayListOf()).apply {
                this.add(entity)
            }
        }
    }

    private fun getTile(location: Location): Tile?{
        val (x, y) = location.coords
        val (chunkX, chunkY) = getChunkCoords(x, y)
        return maps[location.map]?.tilesMaps?.get(Pair(chunkX, chunkY))?.get(x.toInt() to y.toInt())?: maps[location.map]?.defaultTile
    }

    private fun getEntities(location: Location): List<Entity>{
        val (x, y) = location.coords
        val (chunkX, chunkY) = getChunkCoords(x, y)
        return maps[location.map]?.entityMaps?.get(Pair(chunkX, chunkY))?.filter { x.toInt() == it.location.coords.first.toInt() && y.toInt() == it.location.coords.second.toInt() }?: emptyList()
    }

    fun getPlayer(id: Int): EntityPlayer? {
        return maps.values.flatMap { it.entityMaps.values }.flatten().find { it is EntityPlayer && it.id == id } as EntityPlayer?
    }

    private fun popPlayer(id: Int): EntityPlayer? {
        val player = getPlayer(id)
        if (player != null) {
            val (x, y) = player.location.coords
            val (chunkX, chunkY) = getChunkCoords(x, y)
            maps.values.find { it.entityMaps[chunkX to chunkY]?.contains(player) == true }?.entityMaps?.let { entityMaps ->
                entityMaps[Pair(chunkX, chunkY)]?.remove(player)
            }
        }
        return player
    }

    private fun movePlayer(id: Int, action: Action.MovePlayer) {
        val player = popPlayer(id) ?: return
        player.state.let { state ->
            if (state is EntityPlayer.State.INTERACTING) {
                if (state.target is EntityPlayer) {
                    state.target.state.let { targetState ->
                        if (targetState is EntityPlayer.State.INTERACTING && targetState.target == player) {
                            player.state = EntityPlayer.State.IDLE
                            state.target.state = EntityPlayer.State.IDLE
                        }
                    }
                }
            }
        }
        val (x, y) = player.location.coords
        val newX = x + (action.dx * player.speed)
        val newY = y + (action.dy * player.speed)
        moveEntity(player, player.location.copy(coords = newX to newY))
        setEntity(player)
    }

    private fun rotateEntity(action: Action.RotateEntity){
        val (x, y) = action.location.coords
        val chunk = getChunkCoords(x, y)
        maps[action.location.map]?.entityMaps?.get(chunk)?.find { it.id == action.id }?.facing = action.facing
    }

    private fun moveEntity(entity: Entity, to: Location): Boolean {
        val (toX, toY) = to.coords
        val nwTile = getTile(to.copy(coords = toX + entity.hitBox.fromLeft to toY + entity.hitBox.fromTop))
        val neTile = getTile(to.copy(coords = toX + 1 - entity.hitBox.fromRight to toY + entity.hitBox.fromTop))
        val swTile = getTile(to.copy(coords = toX + entity.hitBox.fromLeft to toY + 1 - entity.hitBox.fromBottom))
        val seTile = getTile(to.copy(coords = toX + 1 - entity.hitBox.fromRight to toY + 1 - entity.hitBox.fromBottom))
        if(listOf(nwTile, neTile, swTile, seTile).any { tile -> tile == null || tile.isSolid }){
            return false
        }

        val entitiesInRange = getEntities(
            from = Pair(toX.toInt() - 1, toY.toInt() - 1),
            to = Pair(toX.toInt() + 2, toY.toInt() + 2),
            map = to.map
        )//entities in 3x3 centered on entity
        val (minX, maxX) = entity.getDomain()
        val (newMinX, newMaxX) = entity.getDomainAt(to.coords)
        val (minY, maxY) = entity.getRange()
        val (newMinY, newMaxY) = entity.getRangeAt(to.coords)
        val motionDomain = with(listOf(minX, maxX, newMinX, newMaxX)){min() to max()}
        val motionRange = with(listOf(minY, maxY, newMinY, newMaxY)){min() to max()}
        if(entitiesInRange.any { e ->
            e.intersectsWith(
                domain = motionDomain,
                range = motionRange
            )
        }){
            return false
        }
        
        entity.location = to
        entity.animI = (entity.animI + 1) % 8

        listOf(nwTile, neTile, swTile, seTile).filterIsInstance<TileDoor>().firstOrNull()?.let { door ->
            return moveEntity(entity, door.destination)
        }
        return true
    }

    private fun getFacingTile(player: EntityPlayer): Tile? {
        val (centerX, centerY) = player.getCenter()
        val tileDistance = 1f
        val tileTargetX = centerX + when(player.facing){
            Facing.LEFT -> -tileDistance
            Facing.RIGHT -> tileDistance
            else -> 0f
        }
        val tileTargetY = centerY + when(player.facing){
            Facing.UP -> -tileDistance
            Facing.DOWN -> tileDistance
            else -> 0f
        }
        return getTile(player.location.copy(coords = tileTargetX to tileTargetY))
    }

    private fun getFacingEntity(player: EntityPlayer): Entity? {
        val facing = player.facing
        val (x, y) = player.getCenter()
        val maxDistance = 1f
        val targetX = x + when(facing){
            Facing.LEFT -> -maxDistance
            Facing.RIGHT -> maxDistance
            else -> 0f
        }
        val targetY = y + when(facing){
            Facing.UP -> -maxDistance
            Facing.DOWN -> maxDistance
            else -> 0f
        }
        return getEntities(player.location.copy(coords = targetX to targetY), 2).filter {
            it.id != player.id && when (facing) {
                Facing.LEFT -> it.getRange().let { (min, max) -> y in min..max } && it.getCenter().first < x
                Facing.RIGHT -> it.getRange().let { (min, max) -> y in min..max } && it.getCenter().first > x
                Facing.UP -> it.getDomain().let { (min, max) -> x in min..max } && it.getCenter().second < y
                Facing.DOWN -> it.getDomain().let { (min, max) -> x in min..max } && it.getCenter().second > y
            }
        }.minByOrNull {
            when (facing) {
                Facing.RIGHT -> it.getDomain().let { (min, _) -> min - x }
                Facing.LEFT -> it.getDomain().let { (_, max) -> x - max }
                Facing.DOWN -> it.getRange().let { (min, _) -> min - y }
                Facing.UP -> it.getRange().let { (_, max) -> y - max }
            }
        }
    }

    private fun interactBy(playerId: Int){
        val player = getPlayer(playerId) ?: return
        val entity = getFacingEntity(player)
        if(entity != null){
            println("Interacting with $entity")
            player.state = EntityPlayer.State.INTERACTING(entity)
            if (entity is EntityPlayer) {
                entity.state = EntityPlayer.State.INTERACTING(player)
            }
            return
        }
        val tile = getFacingTile(player)
        println("Interacting with $tile")
    }

    private fun getTopTiles(from: Pair<Int, Int>, to: Pair<Int, Int>, map: String): ArrayList<ArrayList<Tile>> {
        val (fromX, fromY) = from
        val (toX, toY) = to
        val result = ArrayList<ArrayList<Tile>>()
        for (y in fromY..toY) {
            val row = ArrayList<Tile>()
            for (x in fromX..toX) {
                val tile = getTile(Location(coords = x.toFloat() to y.toFloat(), map = map))?: TileWall()
                row.add(tile)
            }
            result.add(row)
        }
        return result
    }

    private fun getEntities(location: Location, r: Int): List<Entity> {
        val result = arrayListOf<Entity>()
        for (i in -r..r) {
            for (j in -r..r) {
                result.addAll(getEntities(
                    location = location.copy(
                        coords = location.coords.let { (x, y) -> x + i to y + j }
                    )
                ))
            }
        }
        return result
    }

    private fun getEntities(from: Pair<Int, Int>, to: Pair<Int, Int>, map: String): List<Entity> {
        val (fromX, fromY) = from
        val (toX, toY) = to
        val result = arrayListOf<Entity>()
        for (y in fromY..toY) {
            for (x in fromX..toX) {
                result.addAll(getEntities(Location(coords = x.toFloat() to y.toFloat(), map)))
            }
        }
        return result
    }
}