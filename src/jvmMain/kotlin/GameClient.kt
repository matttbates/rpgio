import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
import kotlinx.coroutines.flow.map
import tiles.*
import kotlin.math.atan

class GameClient(
    private val world: World
){
    private var gameState: Flow<GameState>? by mutableStateOf(null)
    private var isLoggedIn by mutableStateOf(false)

    companion object {
        private const val cellSize = 20
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
        val playerPosition = remember { mutableStateOf(0f to 0f) }
        val pointerPosition = remember { mutableStateOf(0f to 0f) }
        val requester = remember { FocusRequester() }
        var disconnecting by remember { mutableStateOf(false) }
        val player = gameState.entities.find { it is EntityPlayer && it.id == gameState.playerId } as EntityPlayer?
        Box(
            modifier = Modifier
                .onKeyEvent {
                    when(it.type){
                        KeyEventType.KeyDown -> keysDown.value.add(it.key)
                        KeyEventType.KeyUp -> keysDown.value.remove(it.key)
                    }
                    true
                }
                .onPointerEvent(eventType = PointerEventType.Move){
                    pointerPosition.value = with(it.changes.first().position){ x to y }
                }
                .focusRequester(requester)
                .focusable()
                .fillMaxSize()
        ) {
            Column {
                Row {
                    Text("Game logged in as player ${gameState.playerId}")
                    Button(
                        onClick = {
                            world.disconnect(gameState.playerId)
                            disconnecting = true
                            onDisconnect()
                        }
                    ){
                        Text("Logout")
                    }
                }
                /*Text("Tick: ${gameState.tick}")
                gameState.entities.filterIsInstance<EntityPlayer>().sortedBy { it.id }.forEach { player ->
                    Text("Player ${player.id} at ${player.coords} facing ${player.rotation}")
                }*/
                val (width, height) = world.getDisplaySize()
                if(player != null){
                    val playerOffsetX = (width - 1) / 2
                    val playerOffsetY = (height - 1) / 2

                    val (playerX, playerY) = player.coords
                    val displayXOffset = playerX % 1
                    val displayYOffset = playerY % 1
                    Box(
                        modifier = Modifier
                            .padding(cellSize.dp)
                            .size(width = ((width - 1) * cellSize).dp, height = ((height - 1) * cellSize).dp)
                            .border(1.dp, Color.Black)
                            .clipToBounds()
                    ){
                        Box(
                            modifier = Modifier
                                .size(width = (width * cellSize).dp, height = (height * cellSize).dp)
                                .align(Alignment.Center)
                                .offset(
                                    x = (-displayXOffset * cellSize).dp,
                                    y = (-displayYOffset * cellSize).dp
                                )
                                .onGloballyPositioned {
                                    val offset = it.localToWindow(Offset.Zero)
                                    playerPosition.value = (it.size.width / 2) + offset.x to (it.size.height / 2) + offset.y
                                }
                        ){
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
                        //Entity(player, (playerOffsetX * cellSize).toFloat(), (playerOffsetY * cellSize).toFloat())

                        /*val deg = calculateAngle(playerPosition.value, pointerPosition.value)
                        println("angle: $deg")
                        world.enqueueAction(gameState.playerId, Action.RotateEntity(
                            id = gameState.playerId,
                            x = playerX,
                            y = playerY,
                            rotation = deg.takeUnless { it.isNaN() }?.toFloat()?:0f
                        ))*/

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
                        if(dX != 0 || dY != 0){
                            world.enqueueAction(gameState.playerId, Action.MovePlayer(dX, dY))
                        }
                        val rotation = when{
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
                            world.enqueueAction(gameState.playerId, Action.RotateEntity(
                                id = gameState.playerId,
                                x = playerX,
                                y = playerY,
                                rotation = it
                            ))
                        }
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            requester.requestFocus()
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

                    if(tile is TileWall || tile is TileGrass || tile is TileWater){
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
                    }else{
                        Text(
                            text = tile.appearance.toString(),
                            textAlign = TextAlign.Center,
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
        /*Text(
            text = entity.appearance.toString(),
            modifier = Modifier
                .size(cellSize.dp)
                .offset(
                    x = x.dp,
                    y = y.dp
                )
                .rotate(entity.rotation)
                //.border(1.dp, MaterialTheme.colors.onSurface),
        )*/

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
