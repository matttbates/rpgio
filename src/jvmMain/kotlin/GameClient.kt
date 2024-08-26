import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import entities.Entity
import kotlinx.coroutines.flow.Flow
import entities.EntityPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.skia.impl.Log

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
        val requester = remember { FocusRequester() }
        var disconnecting by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .onKeyEvent {
                    when(it.type){
                        KeyEventType.KeyDown -> keysDown.value.add(it.key)
                        KeyEventType.KeyUp -> keysDown.value.remove(it.key)
                    }
                    true
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
                    Text("Player ${player.id} at ${player.coords}")
                }
                val tiles = gameState.tiles
                val player = gameState.entities.find { it is EntityPlayer && it.id == gameState.playerId } as EntityPlayer?
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
                    }
                }
            }
        }

        keysDown.value.forEach { key ->
            when (key) {
                Key.W -> {
                    world.enqueueAction(gameState.playerId, Action.MovePlayer(0, -1))
                }
                Key.A -> {
                    world.enqueueAction(gameState.playerId, Action.MovePlayer(-1, 0))
                }
                Key.S -> {
                    world.enqueueAction(gameState.playerId, Action.MovePlayer(0, 1))
                }
                Key.D -> {
                    world.enqueueAction(gameState.playerId, Action.MovePlayer(1, 0))
                }
            }
        }

        LaunchedEffect(Unit) {
            requester.requestFocus()
        }
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
                //.border(1.dp, MaterialTheme.colors.onSurface),
        )
    }

}
