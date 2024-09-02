enum class Facing {
    UP, RIGHT, DOWN, LEFT;

    override fun toString(): String {
        return when (this) {
            UP -> "up"
            RIGHT -> "right"
            DOWN -> "down"
            LEFT -> "left"
        }
    }
}