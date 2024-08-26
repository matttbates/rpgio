import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import entities.Entity
import kotlinx.coroutines.flow.Flow
import entities.EntityPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.skia.impl.Log
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.tan

class GameClient(
    private val world: World
){
    private var gameState: Flow<GameState>? by mutableStateOf(null)

    companion object {
        private const val cellSize = 20
    }

    @Composable
    fun Display(){
        gameState?.let {
            val state by it.collectAsState(GameState())
            Game(state){
                gameState = null
            }
        } ?: Login()
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
        gameState: GameState,
        onDisconnect: () -> Unit = {}
    ){
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
                Text("Tick: ${gameState.tick}")
                gameState.entities.filterIsInstance<EntityPlayer>().sortedBy { it.id }.forEach { player ->
                    Text("Player ${player.id} at ${player.coords} facing ${player.rotation}")
                }
                val tiles = gameState.tiles
                if(tiles.isNotEmpty() && player != null){
                    val playerOffsetX = (tiles.first().size - 1) / 2
                    val playerOffsetY = (tiles.size - 1) / 2

                    val (playerX, playerY) = player.coords
                    val displayXOffset = playerX % 1
                    val displayYOffset = playerY % 1
                    val width = tiles.first().size
                    val height = tiles.size
                    Box{
                        Box(
                            modifier = Modifier
                                .size(width = (width * cellSize).dp, height = (height * cellSize).dp)
                                .offset(
                                    x = (-displayXOffset * cellSize).dp,
                                    y = (-displayYOffset * cellSize).dp
                                )
                                .background(Color.Cyan)
                                .onGloballyPositioned {
                                    val offset = it.localToWindow(Offset.Zero)
                                    playerPosition.value = (it.size.width / 2) + offset.x to (it.size.height / 2) + offset.y
                                }
                        ) {
                            tiles.forEachIndexed { r, row ->
                                val rowY = r * cellSize
                                row.forEachIndexed { c, tile ->
                                    val cellX = c * cellSize
                                    Text(
                                        text = tile.appearance.toString(),
                                        modifier = Modifier
                                            .size(cellSize.dp)
                                            .offset(
                                                x = cellX.dp,
                                                y = rowY.dp
                                            )
                                            //.border(1.dp, MaterialTheme.colors.onSurface),
                                    )
                                }
                            }

                        }
                        gameState.entities.filterNot {
                            it is EntityPlayer && it.id == gameState.playerId
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
                        Entity(player, (playerOffsetX * cellSize).toFloat(), (playerOffsetY * cellSize).toFloat())

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
        Text(
            text = entity.appearance.toString(),
            modifier = Modifier
                .size(cellSize.dp)
                .offset(
                    x = x.dp,
                    y = y.dp
                )
                .rotate(entity.rotation)
                //.border(1.dp, MaterialTheme.colors.onSurface),
        )
    }

}
