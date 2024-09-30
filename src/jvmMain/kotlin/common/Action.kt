package common

import common.tiles.Tile

sealed class Action {
    data class MoveEntity(val dx: Int, val dy: Int) : Action()
    data class GoToMap(val file: String) : Action()
    object Interact: Action()
    data class SendMessage(val message: String): Action()
    object CloseConversation: Action()
    data class EditTile(val x: Int, val y: Int, val tile: Tile): Action()
}