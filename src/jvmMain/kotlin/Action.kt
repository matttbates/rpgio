import tiles.Tile

sealed class Action {
    data class MovePlayer(val dx: Int, val dy: Int) : Action()
    data class RotateEntity(val id: Int, val location: Location, val facing: Facing) : Action()
    object Interact: Action()
    data class SendMessage(val message: String): Action()
    object CloseConversation: Action()
    data class EditTile(val x: Int, val y: Int, val tile: Tile): Action()
}