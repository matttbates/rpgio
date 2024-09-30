package entities

import Facing
import HitBox
import Location
import chat.Conversation
import kotlinx.serialization.Serializable

@Serializable
class EntityPlayer(
    override val id: Int,
    var speed: Float = 0.2f,
    override var location: Location,
    override var facing: Facing = Facing.DOWN,
    var state: State = State.IDLE
) : Entity {
    val isEditing = id == -1
    override fun getSprite(): String {
        return when (facing) {
            Facing.RIGHT -> "walk/walk_right_$animI.png"
            Facing.DOWN -> "walk/walk_down_$animI.png"
            Facing.LEFT -> "walk/walk_left_$animI.png"
            Facing.UP -> "walk/walk_up_$animI.png"
        }
    }

    override var animI: Int = 7
    override val hitBox = HitBox(
        fromLeft = 0.2f,
        fromTop = 0.7f,
        fromRight = 0.2f,
        fromBottom = 0.1f
    )
    @Serializable
    sealed class State{
        object IDLE: State()
        data class TALKING(val conversation: Conversation): State()
    }
}