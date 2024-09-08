import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import entities.Entity
import kotlinx.coroutines.flow.Flow
import entities.EntityPlayer
import tiles.*

class GameClient(
    private val world: World
){
    private var gameState: Flow<GameState>? by mutableStateOf(null)
    private var isLoggedIn by mutableStateOf(false)

    companion object {
        const val DEBUG_VIEW = false
        const val cellSize = 20
        private val painterMap = mutableMapOf<String, Painter>()

        @Composable
        fun getPainter(name: String): Painter {
            return painterMap[name]?:painterResource(name).also { painterMap[name] = it }
        }
    }

    @Composable
    fun Display(){
        println("Rendering display")
        if(isLoggedIn){
            Game(
                onDisconnect = {
                    isLoggedIn = false
                }
            )
        } else {
            Login()
        }
    }

    @Composable
    fun Login(){
        var input by remember { mutableStateOf("") }
        Column {
            TextField(
                value = input,
                onValueChange = {
                    input = it
                }
            )
            Button(
                onClick = {
                    input.toInt().let { playerId ->
                        gameState = world.connect(playerId)
                        isLoggedIn = true
                    }
                },
                enabled = input.isNotEmpty() && input.toIntOrNull() != null
            ){
                Text("Login")
            }
        }

    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Game(
        onDisconnect: () -> Unit = {}
    ){
        val gameState by gameState?.collectAsState(GameState())?:return
        val keysDown = remember { mutableStateOf<MutableSet<Key>>(HashSet()) }
        val requester = remember { FocusRequester() }
        var showMenu by remember { mutableStateOf(false) }
        var disconnecting by remember { mutableStateOf(false) }
        val player = gameState.entities.find { it is EntityPlayer && it.id == gameState.playerId } as EntityPlayer?
        Box(
            modifier = Modifier
                .onKeyEvent {
                    when(it.type){
                        KeyEventType.KeyDown -> {
                            keysDown.value.add(it.key)
                            when(it.key){
                                Key.E -> showMenu = !showMenu
                                Key.Enter -> world.enqueueAction(playerId = gameState.playerId, action = Action.Interact)
                            }
                        }
                        KeyEventType.KeyUp -> keysDown.value.remove(it.key)
                    }
                    true
                }
                .focusRequester(requester)
                .focusable()
                .fillMaxSize()
        ) {
            val (width, height) = world.getDisplaySize()
            if (player != null) {
                val playerOffsetX = (width - 1) / 2
                val playerOffsetY = (height - 1) / 2

                val (playerX, playerY) = player.location.coords
                val displayXOffset = playerX % 1
                val displayYOffset = playerY % 1
                Box(
                    modifier = Modifier
                        .size(width = ((width - 2) * cellSize).dp, height = ((height - 2) * cellSize).dp)
                        .clipToBounds()
                        .offset(
                            x = -cellSize.dp,
                            y = -cellSize.dp
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = (width * cellSize).dp, height = (height * cellSize).dp)
                            .align(Alignment.Center)
                            .offset(
                                x = (-displayXOffset * cellSize).dp,
                                y = (-displayYOffset * cellSize).dp
                            )
                    ) {
                        Tiles(tiles = gameState.tiles)
                    }
                    gameState.entities.sortedBy {
                        it.location.coords.second
                    }.forEach {
                        val (x, y) = it.location.coords
                        //player coords are in the center of the screen
                        //so we need to adjust the entity coords to be relative to the player
                        val adjustedX = playerOffsetX - (playerX - x)
                        val adjustedY = playerOffsetY - (playerY - y)
                        val cellX = adjustedX * cellSize
                        val cellY = adjustedY * cellSize
                        Entity(it, it.getSprite(), cellX, cellY)
                    }

                    //Handle key presses
                    var dX = 0
                    var dY = 0
                    keysDown.value.forEach { key ->
                        when (key) {
                            Key.W -> {
                                dY -= 1
                            }

                            Key.A -> {
                                dX -= 1
                            }

                            Key.S -> {
                                dY += 1
                            }

                            Key.D -> {
                                dX += 1
                            }
                        }
                    }
                    if (dX != 0) {
                        world.enqueueAction(gameState.playerId, Action.MovePlayer(dX, 0))
                    }
                    if (dY != 0) {
                        world.enqueueAction(gameState.playerId, Action.MovePlayer(0, dY))
                    }
                    val facing = when {
                        dX == 1 && dY == 0 -> Facing.RIGHT
                        dX == 1 && dY == 1 -> Facing.RIGHT
                        dX == 0 && dY == 1 -> Facing.DOWN
                        dX == -1 && dY == 1 -> Facing.LEFT
                        dX == -1 && dY == 0 -> Facing.LEFT
                        dX == -1 && dY == -1 -> Facing.LEFT
                        dX == 0 && dY == -1 -> Facing.UP
                        dX == 1 && dY == -1 -> Facing.RIGHT
                        else -> null
                    }
                    facing?.let {
                        world.enqueueAction(
                            gameState.playerId, Action.RotateEntity(
                                id = gameState.playerId,
                                location = Location(
                                    coords = playerX to playerY,
                                    map = gameState.map
                                ),
                                facing = it
                            )
                        )
                    }
                }
            }

            //Lighting
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 1 - gameState.lightLevel))
            )

            //Clock
            DigitalClock(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
                tick = gameState.tick
            )

        }

        if(showMenu){
            Menu(
                gameState = gameState,
                onDisconnect = {
                    world.disconnect(gameState.playerId)
                    disconnecting = true
                    onDisconnect()
                }
            )
        }

        LaunchedEffect(Unit) {
            requester.requestFocus()
        }
    }

    @Composable
    private fun Menu(
        gameState: GameState,
        onDisconnect: () -> Unit
    ){
        Column(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.2f))
        ) {
            Row {
                Text("Game logged in as player ${gameState.playerId}")
                Button(
                    onClick = onDisconnect
                ) {
                    Text("Logout")
                }
            }
        }
    }

    @Composable
    private fun DigitalClock(
        modifier: Modifier = Modifier,
        tick: Int
    ){
        val hour = ((tick % World.TICKS_PER_DAY) / World.TICKS_PER_DAY * 24).toInt()
        val amPm = if(hour < 12) "AM" else "PM"
        val hour12 = (hour % 12).let { if(it == 0) 12 else it }
        val minute = (((tick % World.TICKS_PER_DAY) / World.TICKS_PER_DAY * 24 * 60) % 60).toInt()
        val minuteString = if(minute < 10) "0$minute" else "$minute"
        Text(modifier = modifier
            .background(Color.White.copy(alpha = 0.2f)),
            text = "$hour12:$minuteString $amPm"
        )
    }

    @Composable
    private fun AnalogClock(
        tick: Int
    ){
        val size = 100
        val hour = ((tick % World.TICKS_PER_DAY) / World.TICKS_PER_DAY * 24).toInt()
        val minute = ((tick % World.TICKS_PER_DAY) / World.TICKS_PER_DAY * 24 * 60).toInt()
        Text("Time: $hour")
        Box(
            modifier = Modifier
                .size(size.dp)
                .border(width = 1.dp, color = Color.Black, shape = CircleShape)
        ){
            Box(modifier = Modifier
                .align(Alignment.Center)
                .size((size * 0.1).dp)
                .background(Color.Black, shape = CircleShape),
            )
            val hourAngle = (hour % 12f / 12f) * 360
            Box(modifier = Modifier
                .align(Alignment.Center)
                .height((size * 0.5).dp)
                .width((size * 0.1).dp)
                .rotate(hourAngle)
            ){
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .background(Color.Black, shape = CircleShape)
                )
            }
            val minuteAngle = (minute % 60f / 60f) * 360
            Box(modifier = Modifier
                .align(Alignment.Center)
                .height((size * 0.7).dp)
                .width((size * 0.1).dp)
                .rotate(minuteAngle)
            ){
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .background(Color.Black, shape = CircleShape)
                )
            }
        }
    }

    @Composable
    private fun Tiles(
        modifier: Modifier = Modifier,
        tiles: List<List<Tile>>
    ){
        Box(
            modifier = modifier
        ) {
            tiles.forEachIndexed { r, row ->
                val rowY = r * cellSize
                row.forEachIndexed { c, tile ->
                    val cellX = c * cellSize

                    Image(
                        painter = getPainter("tiles/tile_${tile.sprite}.png"),
                        contentDescription = null,
                        modifier = Modifier
                            .size(cellSize.dp)
                            .offset(
                                x = cellX.dp,
                                y = rowY.dp
                            )
                    )
                }
            }

        }
    }

    @Composable
    fun Entity(entity: Entity, sprite: String, x: Float, y: Float){
        //val sprite = entity.getSprite()
        Image(
            painter = getPainter("$sprite ".trim()),
            contentDescription = null,
            modifier = Modifier
                .size(width = cellSize.dp, height = cellSize.dp)
                .offset(
                    x = x.dp,
                    y = y.dp
                )
        )
        if(DEBUG_VIEW){
            Box(modifier = Modifier
                .size(width = cellSize.dp, height = cellSize.dp)
                .offset(
                    x = x.dp,
                    y = y.dp
                )
                .border(1.dp, color = Color.Cyan)
                .padding(
                    start = (entity.hitBox.fromLeft * cellSize).dp,
                    top = (entity.hitBox.fromTop * cellSize).dp,
                    end = (entity.hitBox.fromRight * cellSize).dp,
                    bottom = (entity.hitBox.fromBottom * cellSize).dp
                )
                .border(1.dp, color = Color.Red)
            )
        }
        if (entity is EntityPlayer && entity.state is EntityPlayer.State.INTERACTING) {
            Image(
                painter = getPainter("interact.png"),
                contentDescription = null,
                modifier = Modifier
                    .size(width = cellSize.dp, height = cellSize.dp)
                    .offset(
                        x = x.dp,
                        y = y.dp - cellSize.dp
                    )
            )
        }
    }

}
