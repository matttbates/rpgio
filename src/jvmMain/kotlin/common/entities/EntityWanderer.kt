package common.entities

import common.Action
import common.Facing
import common.HitBox
import common.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import server.World
import kotlin.math.roundToInt

@Serializable
class EntityWanderer(
    override val id: Int,
    override var speed: Float = 0.2f,
    override var location: Location,
    override var facing: Facing = Facing.DOWN,
    override var chatState: Chatter.State = Chatter.State.IDLE
) : Entity, Chatter, Walker {
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

    fun runAI(world: World){
        CoroutineScope(Dispatchers.IO).launch {
            val stateFlow = world.connectAI(id)
            while (true){
                val dx = Math.random().times(2).roundToInt() - 1
                val dy = Math.random().times(2).roundToInt() - 1
                //println("Wanderer $id moving $dx $dy")
                world.enqueueAction(
                    playerId = id,
                    Action.MoveEntity(
                        dx = dx,
                        dy = dy
                    )
                )
                Thread.sleep(100)
            }
        }
    }

}