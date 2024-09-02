sealed class Action {
    data class MovePlayer(val dx: Int, val dy: Int) : Action()
    data class RotateEntity(val id: Int, val x: Float, val y: Float, val facing: Facing) : Action()
    object Interact: Action()
}