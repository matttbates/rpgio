import entities.Entity
import entities.EntityPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import tiles.*

class World {
    //key is the furthest distance from the origin within the chunk
    private val tilesMaps = hashMapOf<Pair<Int, Int>, HashMap<Pair<Int, Int>, Tile>>()
    private val entityMaps = hashMapOf<Pair<Int, Int>, ArrayList<Entity>>()

    private val spawnLocations = arrayListOf<Pair<Int, Int>>()

    companion object {
        private const val CHUNK_SIZE = 20
        const val TPS = 20
        private val mapRaw = createBitmapFromFile("src/jvmMain/resources/map.png")

        private val defaultTile = TileWater()
    }

    private var displaySize = Pair(0, 0)
    fun getDisplaySize() = displaySize

    private val clientStates = arrayListOf<MutableStateFlow<GameState>>()
    private val pendingActions = hashMapOf<Int, ArrayList<Action>>()

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
                    val (x, y) = player.coords.let { (x, y) -> Pair(x.toInt(), y.toInt()) }
                    val from = Pair(x - rX, y - rY)
                    val to = Pair(x + rX, y + rY)
                    val tiles = getTopTiles(from, to)
                    val entities = getEntities(from, to)
                    it.value = GameState(playerId, tiles, entities, tick)
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
        val emptySpawns = spawnLocations.filter { (x, y) ->
            getEntities(x, y).isEmpty()
        }
        if (emptySpawns.isEmpty()) {
            return null
        }
        val coords = emptySpawns.random()
        val entityPlayer = EntityPlayer(
            id = id,
            coords = Pair(coords.first.toFloat(), coords.second.toFloat())
        )
        setEntity(entityPlayer)
        return entityPlayer
    }

    private fun checkSign(num: Int) = (num shr 31 or 1)

    private fun getChunkCoords(x: Int, y: Int): Pair<Int, Int>{
        val qX = checkSign(x)
        val qY = checkSign(y)
        return Pair((x / CHUNK_SIZE) + qX, (y / CHUNK_SIZE) + qY)
    }

    private fun setTile(x: Int, y: Int, tile: Tile){
        val (chunkX, chunkY) = getChunkCoords(x, y)
        tilesMaps[Pair(chunkX, chunkY)] = (tilesMaps[Pair(chunkX, chunkY)] ?: hashMapOf()).apply {
            this[Pair(x, y)] = tile
        }
    }

    private fun setEntity(entity: Entity){
        val (x, y) = entity.coords
        val (chunkX, chunkY) = getChunkCoords(x.toInt(), y.toInt())
        entityMaps[Pair(chunkX, chunkY)] = (entityMaps[Pair(chunkX, chunkY)] ?: arrayListOf()).apply {
            this.add(entity)
        }
    }

    private fun getTile(x: Int, y: Int): Tile?{
        val (chunkX, chunkY) = getChunkCoords(x, y)
        return tilesMaps[Pair(chunkX, chunkY)]?.get(Pair(x, y))
    }

    private fun getEntities(x: Int, y: Int): List<Entity>{
        val (chunkX, chunkY) = getChunkCoords(x, y)
        return entityMaps[Pair(chunkX, chunkY)]?.filter { x == it.coords.first.toInt() && y == it.coords.second.toInt() }?: emptyList()
    }

    init {
        mapRaw?.let { imageBitmap ->
            val buffer = IntArray(imageBitmap.width * imageBitmap.height)
            imageBitmap.readPixels(buffer)
            for (y in 0 until imageBitmap.height) {
                for (x in 0 until imageBitmap.width) {
                    when (buffer[y * imageBitmap.width + x]) {
                        0xFF00FF00.toInt() -> TileGrass()
                        0xFF0000FF.toInt() -> TileWater()
                        0xFF00FFFF.toInt() -> TilePath()
                        0xFFFF0000.toInt() -> TileSpawner()
                        0xFF000000.toInt() -> TileWall()
                        else -> null
                    }?.let { tile ->
                        setTile(x, y, tile)
                        if (tile is TileSpawner) {
                            spawnLocations.add(Pair(x, y))
                        }
                    }
                }
            }
        }
    }

    fun getPlayer(id: Int): EntityPlayer? {
        return entityMaps.values.flatten().find { it is EntityPlayer && it.id == id } as EntityPlayer?
    }

    private fun popPlayer(id: Int): EntityPlayer? {
        val player = getPlayer(id)
        if (player != null) {
            val (x, y) = player.coords
            val (chunkX, chunkY) = getChunkCoords(x.toInt(), y.toInt())
            entityMaps[Pair(chunkX, chunkY)]?.remove(player)
        }
        return player
    }

    private fun movePlayer(id: Int, action: Action.MovePlayer) {
        val player = popPlayer(id) ?: return
        val (x, y) = player.coords
        val newX = x + (action.dx * player.speed)
        val newY = y + (action.dy * player.speed)
        moveEntity(player, Pair(newX, newY))
        setEntity(player)
    }

    private fun rotateEntity(action: Action.RotateEntity){
        val chunk = getChunkCoords(action.x.toInt(), action.y.toInt())
        entityMaps[chunk]?.find { it.id == action.id }?.rotation = action.rotation
    }

    private fun moveEntity(entity: Entity, to: Pair<Float, Float>): Boolean {
        val destTile = getTile((to.first + 0.5f).toInt(), (to.second + 0.5f).toInt())
        if (destTile?.isSolid != false) {
            return false
        }
        val entityAtDestination = getEntities(to.first.toInt(), to.second.toInt()).find { it.id != entity.id }
        if (entityAtDestination != null) {
            return false
        }
        entity.coords = to
        entity.animI = (entity.animI + 1) % 8
        return true
    }

    private fun getTopTiles(from: Pair<Int, Int>, to: Pair<Int, Int>): ArrayList<ArrayList<Tile>> {
        val (fromX, fromY) = from
        val (toX, toY) = to
        val result = ArrayList<ArrayList<Tile>>()
        for (y in fromY..toY) {
            val row = ArrayList<Tile>()
            for (x in fromX..toX) {
                val tile = getTile(x, y) ?: defaultTile
                row.add(tile)
            }
            result.add(row)
        }
        return result
    }

    private fun getEntities(from: Pair<Int, Int>, to: Pair<Int, Int>): List<Entity> {
        val (fromX, fromY) = from
        val (toX, toY) = to
        val result = arrayListOf<Entity>()
        for (y in fromY..toY) {
            for (x in fromX..toX) {
                result.addAll(getEntities(x, y))
            }
        }
        return result
    }
}