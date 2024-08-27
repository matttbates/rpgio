package entities

import World.Companion.TPS
import tiles.Tile

class EntityPlayer(
    override val id: Int,
    val speed: Float = 0.2f,
    override var coords: Pair<Float, Float>,
    override var rotation: Float = 0f,
) : Entity {
    override val appearance: Char = '>'
    override fun getSprite(): String {
        return when (rotation) {
            in 0f .. 89f -> "walk_right_7.png"
            90f -> "walk_down_7.png"
            in 91f .. 269f -> "walk_left_7.png"
            270f -> "walk_up_7.png"
            else -> "walk_right_7.png"
        }
    }

    override var animI: Int = 7
}