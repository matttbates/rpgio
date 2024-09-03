package entities

import Facing
import HitBox
import Location

class EntityPlayer(
    override val id: Int,
    val speed: Float = 0.2f,
    override var location: Location,
    override var facing: Facing = Facing.DOWN,
    var state: State = State.IDLE
) : Entity {
    override fun getSprite(): String {
        return when (facing) {
            Facing.RIGHT -> "walk_right_$animI.png"
            Facing.DOWN -> "walk_down_$animI.png"
            Facing.LEFT -> "walk_left_$animI.png"
            Facing.UP -> "walk_up_$animI.png"
        }
    }

    override var animI: Int = 7
    override val hitBox = HitBox(
        fromLeft = 0.2f,
        fromTop = 0.7f,
        fromRight = 0.2f,
        fromBottom = 0.1f
    )
    sealed class State{
        object IDLE: State()
        data class INTERACTING(val target: Entity): State()
    }
}