sealed class Action {
    data class MovePlayer(val dx: Int, val dy: Int) : Action()
}