import androidx.compose.material.MaterialTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

@Composable
@Preview
fun App(
    world: World,
    client: GameClient = GameClient(world)
) {
    MaterialTheme {
        client.Display()
    }
}

fun main() = application {
    val world = World()
    world.start(12, 8)
    val (w, h) = world.getDisplaySize()
    val screenW = ((w - 2) * GameClient.CELL_SIZE).dp
    val screenH = ((h - 2) * GameClient.CELL_SIZE).dp
    val titleHeight = 35.dp
    val mysteryWidth = 12.dp
    Window(
        onCloseRequest = {
            world.stop()
            exitApplication()
        },
        state = WindowState(
            size = DpSize(screenW + mysteryWidth, screenH + titleHeight),
        ),
        resizable = false
    ) {
        App(world = world)
    }
    /*Window(
        onCloseRequest = ::exitApplication,
        state = WindowState(
            size = DpSize(screenW + mysteryWidth, screenH + titleHeight),
        ),
        resizable = false
    ) {
        App(world = world)
    }*/
}
