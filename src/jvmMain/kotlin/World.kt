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
    private val tiles0maps = hashMapOf<Pair<Int, Int>, HashMap<Pair<Int, Int>, Tile>>()
    private val tiles1maps = hashMapOf<Pair<Int, Int>, HashMap<Pair<Int, Int>, Tile>>()
    private val entityMaps = hashMapOf<Pair<Int, Int>, ArrayList<Entity>>()

    private val players0 = hashMapOf<Int, Pair<Pair<Int, Int>, EntityPlayer>>()
    private val spawnLocations = arrayListOf<Pair<Int, Int>>()

    companion object {
        private const val CHUNK_SIZE = 20
        const val TPS = 20
        private val map0Raw = arrayOf(
            "##########",
            "#........#",
            "#........#",
            "#........#",
            "#........#",
            "#........#",
            "#...B....#",
            "#........#",
            "#........#",
            "##########"
        )
        private val map1Raw = arrayOf(
            "          ",
            "    ====  ",
            "       =  ",
            "    ====  ",
            "          ",
            "          ",
            "          ",
            "          ",
            "          ",
            "          "
        )
    }

    private val clientStates = arrayListOf<MutableStateFlow<GameState>>()
    private val pendingActions = hashMapOf<Int, ArrayList<Action>>()

    fun start(
        rX: Int = 12,
        rY: Int = 4
    ) {
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
            getTile(x, y, 1) == null
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

    private fun setTile(x: Int, y: Int, z: Int, tile: Tile){
        val (chunkX, chunkY) = getChunkCoords(x, y)
        val maps = when(z){
            0 -> tiles0maps
            1 -> tiles1maps
            else -> return
        }
        maps[Pair(chunkX, chunkY)] = (maps[Pair(chunkX, chunkY)] ?: hashMapOf()).apply {
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

    private fun removeTile(x: Int, y: Int, z: Int){
        val (chunkX, chunkY) = getChunkCoords(x, y)
        val maps = when(z){
            0 -> tiles0maps
            1 -> tiles1maps
            else -> return
        }
        maps[Pair(chunkX, chunkY)]?.remove(Pair(x, y))
    }

    private fun getTile(x: Int, y: Int, z: Int): Tile?{
        val (chunkX, chunkY) = getChunkCoords(x, y)
        val maps = when(z){
            0 -> tiles0maps
            1 -> tiles1maps
            else -> return null
        }
        return maps[Pair(chunkX, chunkY)]?.get(Pair(x, y))
    }

    private fun getEntities(x: Int, y: Int): List<Entity>{
        val (chunkX, chunkY) = getChunkCoords(x, y)
        return entityMaps[Pair(chunkX, chunkY)]?.filter { x == it.coords.first.toInt() && y == it.coords.second.toInt() }?: emptyList()
    }

    init {
        for (y in map0Raw.indices) {
            for (x in 0 until map0Raw[y].length) {
                when (map0Raw[y][x]) {
                    '#' -> TilePath()
                    '.' -> TileGrass()
                    'B' -> TileBed()
                    else -> null
                }?.let{tile ->
                    setTile(x, y, 0, tile)
                    if (tile is TileBed) {
                        spawnLocations.add(Pair(x, y))
                    }
                    /*for (i in 1 .. 100){
                        setTile(x + (i*20), y, 0, tile)
                        setTile(x - (i*20), y, 0, tile)
                        setTile(x, y + (i*20), 0, tile)
                        setTile(x, y - (i*20), 0, tile)
                    }*/
                }
            }
        }

        for (y in map1Raw.indices) {
            for (x in 0 until map1Raw[y].length) {
                when (map1Raw[y][x]) {
                    '=' -> TileWall()
                    else -> null
                }?.let { tile ->
                    setTile(x, y, 1, tile)
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
        val destTile = getTile((to.first + 0.5f).toInt(), (to.second + 0.5f).toInt(), 1)
        if (destTile != null) {
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
                val tile = getTile(x, y, 1) ?: getTile(x, y, 0) ?: TileAir()
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