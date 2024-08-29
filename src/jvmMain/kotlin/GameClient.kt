import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import entities.Entity
import kotlinx.coroutines.flow.Flow
import entities.EntityPlayer
import tiles.*
import kotlin.math.atan

class GameClient(
    private val world: World
){
    private var gameState: Flow<GameState>? by mutableStateOf(null)
    private var isLoggedIn by mutableStateOf(false)

    companion object {
        const val cellSize = 20
        private val painterMap = mutableMapOf<String, Painter>()
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

                val (playerX, playerY) = player.coords
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
                        it.coords.second
                    }.forEach {
                        val (x, y) = it.coords
                        //player coords are in the center of the screen
                        //so we need to adjust the entity coords to be relative to the player
                        val adjustedX = playerOffsetX - (playerX - x)
                        val adjustedY = playerOffsetY - (playerY - y)
                        val cellX = adjustedX * cellSize
                        val cellY = adjustedY * cellSize
                        Entity(it, cellX, cellY)
                    }

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
                    val rotation = when {
                        dX == 1 && dY == 0 -> 0f
                        dX == 1 && dY == 1 -> 45f
                        dX == 0 && dY == 1 -> 90f
                        dX == -1 && dY == 1 -> 135f
                        dX == -1 && dY == 0 -> 180f
                        dX == -1 && dY == -1 -> 225f
                        dX == 0 && dY == -1 -> 270f
                        dX == 1 && dY == -1 -> 315f
                        else -> null
                    }
                    rotation?.let {
                        world.enqueueAction(
                            gameState.playerId, Action.RotateEntity(
                                id = gameState.playerId,
                                x = playerX,
                                y = playerY,
                                rotation = it
                            )
                        )
                    }
                }
            }
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
        Column {
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
                        painter = with("tile_${tile.sprite}.png"){ painterMap[this]?:painterResource(this).also { painterMap[this] = it } },
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

    private fun calculateAngle(player: Pair<Float, Float>, pointer: Pair<Float, Float>): Float {
        val (playerX, playerY) = player
        val (pointerX, pointerY) = pointer
        val o = pointerY - playerY
        val a = pointerX - playerX
        val angle = atan(o / a)
        return Math.toDegrees(angle.toDouble()).toFloat() + (if (a < 0) 180 else 0)
    }

    @Composable
    fun Entity(entity: Entity, x: Float, y: Float){
        val direction = when(entity.rotation){
            in 0f .. 89f -> "right"
            90f -> "down"
            in 91f .. 269f -> "left"
            270f -> "up"
            else -> "right"
        }
        Image(
            painter = painterResource("walk_${direction}_${entity.animI}.png"),
            contentDescription = null,
            modifier = Modifier
                .size(width = cellSize.dp, height = cellSize.dp)
                .offset(
                    x = x.dp,
                    y = y.dp
                )
        )
    }

}
