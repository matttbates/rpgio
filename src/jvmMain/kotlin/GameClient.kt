import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
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
            Game(state)
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
    fun Game(gameState: GameState){
        val requester = remember { FocusRequester() }
        Box(
            modifier = Modifier
                .onKeyEvent {
                    if(it.type == KeyEventType.KeyDown){
                        return@onKeyEvent when (it.key) {
                            Key.W -> {
                                world.enqueueAction(gameState.playerId, Action.MovePlayer(0, -1))
                                true
                            }
                            Key.A -> {
                                world.enqueueAction(gameState.playerId, Action.MovePlayer(-1, 0))
                                true
                            }
                            Key.S -> {
                                world.enqueueAction(gameState.playerId, Action.MovePlayer(0, 1))
                                true
                            }
                            Key.D -> {
                                world.enqueueAction(gameState.playerId, Action.MovePlayer(1, 0))
                                true
                            }
                            else -> false
                        }
                    }
                    false
                }
                .focusRequester(requester)
                .focusable()
                .fillMaxSize()
        ) {
            Column {
                Text("Game logged in as player ${gameState.playerId}")
                Text("Tick: ${gameState.tick}")
                Text("Current position: ${world.getPlayer(gameState.playerId)?.coords}")
                val tiles = gameState.tiles
                val player = gameState.entities.find { it is EntityPlayer && it.id == gameState.playerId } as EntityPlayer?
                if(tiles.isNotEmpty() && player != null){
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

                            gameState.entities.filterNot {
                                it is EntityPlayer && it.id == gameState.playerId
                            }.forEach {
                                val (x, y) = it.coords
                                val cellX = x * cellSize
                                val cellY = y * cellSize
                                Entity(it, cellX, cellY)
                            }
                        }

                        val offsetX = ((tiles.first().size - 1) * cellSize) / 2
                        val offsetY = ((tiles.size - 1) * cellSize) / 2
                        Entity(player, offsetX.toFloat(), offsetY.toFloat())
                    }
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
