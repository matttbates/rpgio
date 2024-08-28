import androidx.compose.material.MaterialTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
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
    world.start(8, 8)
    Window(onCloseRequest = ::exitApplication) {
        App(world = world)
    }
    /*Window(onCloseRequest = ::exitApplication) {
        App(world = world)
    }*/
}
