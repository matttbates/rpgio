import RpgIoTime.Companion.TPS
import chat.ChatManager
import chat.Message
import entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import maps.MapData
import maps.MapsJson
import maps.Quadrant
import tiles.*
import kotlin.math.abs

class World {

    companion object {
        private val maps: MutableMap<String, MapData> = hashMapOf()//"src/jvmMain/resources/maps/map.png" to maps.MapData("src/jvmMain/resources/maps/map.png"))
        private const val CHUNK_SIZE = 20

        private fun checkSign(num: Int) = (num shr 31 or 1)

        fun getChunkCoords(x: Float, y: Float): Pair<Int, Int>{
            val qX = checkSign(x.toInt())
            val qY = checkSign(y.toInt())
            return Pair((x.toInt() / CHUNK_SIZE) + qX, (y.toInt() / CHUNK_SIZE) + qY)
        }
    }
    private val fileIO = FileIO()
    private val time = RpgIoTime()
    private val light = Light(time)
    private val chatManager = ChatManager()
    private val json = Json{
        prettyPrint = true
    }

    private var displaySize = Pair(0, 0)
    fun getDisplaySize() = displaySize

    fun getMaps() = maps.values.toList().map { it.file }

    private val clientStates = arrayListOf<MutableStateFlow<GameState>>()
    private val pendingActions = hashMapOf<Int, ArrayList<Action>>()



    init {
        //load maps
        val mapsJsonString = fileIO.readTextFile("src/jvmMain/resources/maps/maps.json")
        val mapsJson = Json.decodeFromString<MapsJson>(mapsJsonString)
        mapsJson.maps.forEach { mapData ->
            mapData.setRawMap(Json.decodeFromString(fileIO.readTextFile(mapData.file)))
            maps[mapData.file] = mapData
        }
        maps.values.forEach { mapData ->
            mapData.rawMap?.let { rawMap ->
                rawMap.se.forEachIndexed { y, row ->
                    row.forEachIndexed { x, pixel ->
                        Tile.getById(pixel)?.let { tile ->
                            setTile(Location(
                                coords = Coords(x.toFloat(), y.toFloat()),
                                map = mapData.file
                            ), tile)
                        }
                    }
                }
                rawMap.nw.forEachIndexed { y, row ->
                    row.forEachIndexed { x, pixel ->
                        Tile.getById(pixel)?.let { tile ->
                            setTile(Location(
                                coords = Coords(-x.inc().toFloat(), -y.inc().toFloat()),
                                map = mapData.file
                            ), tile)
                        }
                    }
                }
                rawMap.ne.forEachIndexed { y, row ->
                    row.forEachIndexed { x, pixel ->
                        Tile.getById(pixel)?.let { tile ->
                            setTile(Location(
                                coords = Coords(x.toFloat(), -y.inc().toFloat()),
                                map = mapData.file
                            ), tile)
                        }
                    }
                }
                rawMap.sw.forEachIndexed { y, row ->
                    row.forEachIndexed { x, pixel ->
                        Tile.getById(pixel)?.let { tile ->
                            setTile(Location(
                                coords = Coords(-x.inc().toFloat(), y.toFloat()),
                                map = mapData.file
                            ), tile)
                        }
                    }
                }
            }
        }
        //load world
        val worldJsonString = fileIO.readTextFile("src/jvmMain/resources/world/world.json")
        val worldJson = Json.decodeFromString<WorldJson>(worldJsonString)
        worldJson.tick?.let { time.setTick(it) }
        //load entities
        val entitiesJsonString = fileIO.readTextFile("src/jvmMain/resources/world/entities.json")
        val entitiesJson = Json.decodeFromString<EntitiesJson>(entitiesJsonString)
        entitiesJson.entities.forEach { entity ->
            setEntity(entity)
        }
    }

    fun start(
        rX: Int = 12,
        rY: Int = 4
    ) {
        displaySize = Pair(rX * 2 + 1, rY * 2 + 1)
        CoroutineScope(Dispatchers.IO).launch {
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
                    val (x, y) = player.location.coords.let { (x, y) -> Pair(x.toInt().toFloat(), y.toInt().toFloat()) }
                    val from = Coords(x - rX, y - rY)
                    val to = Coords(x + rX, y + rY)
                    val tiles = getTopTiles(from, to, player.location.map)
                    val entities = getEntities(from, to, player.location.map)
                    it.value = GameState(
                        playerId = playerId,
                        location = Location(from, player.location.map),
                        tiles = tiles,
                        entities = entities,
                        tick = time.getTick(),
                        lightLevel = maps[player.location.map]?.let { map ->  light.calculateLightLevel(map) }?:1f,
                        time = time.getTimeString(),
                    )
                }
                time.advanceTime()
                Thread.sleep(sleepMillis.toLong())
            }
        }
    }

    fun save() {
        saveEntityData()
        saveWorldData()
    }

    private fun saveEntityData(){
        fileIO.writeTextFile("src/jvmMain/resources/world/entities.json", json.encodeToString(EntitiesJson(
            entities = maps.values.flatMap { it.entityMaps.values }.flatten()
        )))
    }

    private fun saveWorldData(){
        fileIO.writeTextFile("src/jvmMain/resources/world/world.json", json.encodeToString(WorldJson(
            tick = time.getTick()
        )))
    }

    private fun performAction(playerId: Int, action: Action){
        when(action){
            is Action.MovePlayer -> movePlayer(playerId, action)
            is Action.RotateEntity -> rotateEntity(action)
            Action.Interact -> interactBy(playerId)
            Action.CloseConversation -> closeConversation(playerId)
            is Action.SendMessage -> sendMessage(playerId, action)
            is Action.EditTile -> editTile(playerId, action.x, action.y, action.tile)
            is Action.GoToMap -> goToMap(playerId, action.file)
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

    fun connectInEditMode(): Flow<GameState> {
        val playerId = -1
        val player = getOrSpawnPlayerEntity(playerId)//for later use
        player.speed = 0.5f
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

    private fun setTile(location: Location, tile: Tile){
        val (x, y) = location.coords
        val (chunkX, chunkY) = getChunkCoords(x, y)
        maps[location.map]?.apply {
            tilesMaps[Pair(chunkX, chunkY)] = (tilesMaps[Pair(chunkX, chunkY)] ?: hashMapOf()).apply {
                if(this[Pair(x.toInt(), y.toInt())] == Tile.Spawner){
                    spawnLocations.remove(location)
                }
                this[Pair(x.toInt(), y.toInt())] = tile
            }
            if (tile == Tile.Spawner) {
                spawnLocations.add(location)
            }
        }
    }

    private fun removeEntity(entity: Entity){
        val (x, y) = entity.location.coords
        val (chunkX, chunkY) = getChunkCoords(x, y)
        maps[entity.location.map]?.apply {
            entityMaps[Pair(chunkX, chunkY)]?.remove(entity)
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
        return maps[location.map]?.entityMaps?.get(Pair(chunkX, chunkY))?.filter { x.toInt() == it.location.coords.x.toInt() && y.toInt() == it.location.coords.y.toInt() }?: emptyList()
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

    private fun goToMap(playerId: Int, fileName: String){
        val player = getPlayer(playerId) ?: return
        removeEntity(player)
        player.location = player.location.copy(map = fileName)
        setEntity(player)
    }

    private fun movePlayer(id: Int, action: Action.MovePlayer) {
        val player = popPlayer(id) ?: return
        val (x, y) = player.location.coords
        val newX = x + (action.dx * player.speed)
        val newY = y + (action.dy * player.speed)
        if(player.isEditing){
            player.location = player.location.copy(coords = Coords(newX, newY))
        }else{
            moveEntity(player, player.location.copy(coords = Coords(newX, newY)))
        }
        setEntity(player)
    }

    private fun rotateEntity(action: Action.RotateEntity){
        val (x, y) = action.location.coords
        val chunk = getChunkCoords(x, y)
        maps[action.location.map]?.entityMaps?.get(chunk)?.find { it.id == action.id }?.facing = action.facing
    }

    private fun moveEntity(entity: Entity, to: Location): Boolean {
        val (toX, toY) = to.coords
        fun Float.adjustNegative(): Float{
            return if(this < 0) dec() else this
        }
        val nwTile = getTile(to.copy(coords = Coords(
            (toX + entity.hitBox.fromLeft).adjustNegative(),
            (toY + entity.hitBox.fromTop).adjustNegative()
        )))
        val neTile = getTile(to.copy(coords = Coords(
            (toX + 1 - entity.hitBox.fromRight).adjustNegative(),
            (toY + entity.hitBox.fromTop).adjustNegative()
        )))
        val swTile = getTile(to.copy(coords = Coords(
            (toX + entity.hitBox.fromLeft).adjustNegative(),
            (toY + 1 - entity.hitBox.fromBottom).adjustNegative()
        )))
        val seTile = getTile(to.copy(coords = Coords(
            (toX + 1 - entity.hitBox.fromRight).adjustNegative(),
            (toY + 1 - entity.hitBox.fromBottom).adjustNegative()
        )))
        if(listOf(nwTile, neTile, swTile, seTile).any { tile -> tile == null || tile.isSolid }){
            return false
        }

        val entitiesInRange = getEntities(
            from = Coords((toX.toInt() - 1).toFloat(), (toY.toInt() - 1).toFloat()),
            to = Coords((toX.toInt() + 2).toFloat(), (toY.toInt() + 2).toFloat()),
            map = to.map
        ).filter { it.id != entity.id && (it !is EntityPlayer || !it.isEditing) }//entities in 3x3 centered on entity
        val (minX, maxX) = entity.getDomain()
        val newDomain = entity.getDomainAt(to.coords)
        val (newMinX, newMaxX) = newDomain
        val (minY, maxY) = entity.getRange()
        val newRange = entity.getRangeAt(to.coords)
        val (newMinY, newMaxY) = newRange
        val motionDomain = with(listOf(minX, maxX, newMinX, newMaxX)){min() to max()}
        val motionRange = with(listOf(minY, maxY, newMinY, newMaxY)){min() to max()}
        if(entitiesInRange.any { e ->
            e.intersectsWith(
                domain = if(entity.location.map == to.map) motionDomain else newDomain,
                range = if(entity.location.map == to.map) motionRange else newRange
            )
        }){
            return false
        }
        
        entity.location = to
        entity.animI = (entity.animI + 1) % 8

        return true
    }

    private fun getFacingTile(player: EntityPlayer): Tile? {
        fun Float.adjustNegative(): Float{
            return if(this < 0) dec() else this
        }
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
        return getTile(player.location.copy(coords = Coords(tileTargetX.adjustNegative(), tileTargetY.adjustNegative())))
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
        return getEntities(player.location.copy(coords = Coords(targetX, targetY)), 2).filter {
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

    private fun closeConversation(playerId: Int){
        val player = getPlayer(playerId) ?: return
        val otherId = (player.state as? EntityPlayer.State.TALKING)?.conversation?.participants?.find { it != playerId }
        otherId?.let {
            val entity = getPlayer(it)
            entity?.state = EntityPlayer.State.IDLE
        }
        player.state = EntityPlayer.State.IDLE
    }

    private fun sendMessage(playerId: Int, action: Action.SendMessage){
        val player = getPlayer(playerId) ?: return
        if (player.isEditing) {
            return
        }
        if(player.state is EntityPlayer.State.TALKING){
            val conversation = (player.state as EntityPlayer.State.TALKING).conversation
            conversation.addMessage(Message(
                senderId = playerId,
                message = action.message,
                time = time.getTimeStringShort()
            ))
        }
    }

    private fun editTile(playerId: Int, x: Int, y: Int, tile: Tile){
        val player = getPlayer(playerId) ?: return
        val map = player.location.map

        //update local state
        setTile(Location(coords = Coords(x.toFloat(), y.toFloat()), map), tile)

        //update map file
        val mapData = maps[map]?: return
        val qI = when{
            x >= 0 && y >= 0 -> Quadrant.SE
            x >= 0 -> Quadrant.NE
            x < 0 && y >= 0 -> Quadrant.SW
            else -> Quadrant.NW
        }
        val quadrant: List<List<Int>> = when(qI){
            Quadrant.SE -> mapData.rawMap?.se?:emptyList()
            Quadrant.NE -> mapData.rawMap?.ne?:emptyList()
            Quadrant.SW -> mapData.rawMap?.sw?:emptyList()
            Quadrant.NW -> mapData.rawMap?.nw?:emptyList()
        }
        val width = quadrant.getOrNull(0)?.size?:0
        val height = quadrant.size
        val qx = if (x >= 0) x else abs(x.inc())
        val qy = if (y >= 0) y else abs(y.inc())
        val newQuadrant = List(maxOf(height, qy+1)){ yIndex ->
            List(maxOf(width, qx+1)){ xIndex ->
                if (xIndex == qx && yIndex == qy) {
                    tile.ordinal
                } else {
                    quadrant.getOrNull(yIndex)?.getOrNull(xIndex)?:mapData.defaultTile.ordinal
                }
            }
        }
        mapData.setRawMap(mapData.rawMap?.let {
            when(qI){
                Quadrant.SE -> it.copy(se = newQuadrant)
                Quadrant.NE -> it.copy(ne = newQuadrant)
                Quadrant.SW -> it.copy(sw = newQuadrant)
                Quadrant.NW -> it.copy(nw = newQuadrant)
            }
        })
        fileIO.writeTextFile(mapData.file, Json.encodeToString(mapData.rawMap))
    }

    private fun interactBy(playerId: Int){
        val player = getPlayer(playerId) ?: return
        if(player.isEditing){
            return
        }
        val entity = getFacingEntity(player)
        if(entity != null){
            println("Interacting with $entity")
            when(entity){
                is EntityPlayer -> {
                    if(entity.state is EntityPlayer.State.IDLE){
                        val conversation = chatManager.getConversationByIds(playerId, entity.id)
                        player.state = EntityPlayer.State.TALKING(conversation)
                        entity.state = EntityPlayer.State.TALKING(conversation)
                    }
                }
                is EntityDoor -> {
                    removeEntity(player)
                    moveEntity(player, entity.destination)
                    setEntity(player)
                }
            }
            return
        }
        val tile = getFacingTile(player)
        println("Interacting with $tile")
    }

    private fun getTopTiles(from: Coords, to: Coords, map: String): ArrayList<ArrayList<Tile>> {
        if(!maps.keys.contains(map)){
            println("Map $map not found")
            return arrayListOf()
        }
        val (fromX, fromY) = from
        val (toX, toY) = to
        val result = ArrayList<ArrayList<Tile>>()
        for (y in fromY.toInt()..toY.toInt()) {
            val row = ArrayList<Tile>()
            for (x in fromX.toInt()..toX.toInt()) {
                val tile = getTile(Location(coords = Coords(x.toFloat(), y.toFloat()), map = map))?: Tile.Wall
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
                        coords = location.coords.let { (x, y) -> Coords(x + i, y + j) }
                    )
                ))
            }
        }
        return result
    }

    private fun getEntities(from: Coords, to: Coords, map: String): List<Entity> {
        val (fromX, fromY) = from
        val (toX, toY) = to
        val result = arrayListOf<Entity>()
        for (y in fromY.toInt()..toY.toInt()) {
            for (x in fromX.toInt()..toX.toInt()) {
                result.addAll(getEntities(Location(coords = Coords(x.toFloat(), y.toFloat()), map)))
            }
        }
        return result
    }
}