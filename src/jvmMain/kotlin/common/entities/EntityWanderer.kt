package common.entities

import common.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import server.World
import kotlin.math.abs
import kotlin.math.roundToInt

@Serializable
class EntityWanderer(
    override val id: Int,
    override var speed: Float = 0.1f,
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
        var gameState: GameState? = null
        CoroutineScope(Dispatchers.IO).launch {
            world.connectAI(id).collect {
                gameState = it
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            while (true){
                gameState?.let { state ->
                    move(world, state)
                }
                Thread.sleep(50)
            }
        }
    }

    private var targetId: Int? = null

    private fun move(world: World, gameState: GameState){
        if(targetId != null){
            targetId?.let {
                gameState.entities.firstOrNull { e -> e.id == it }?.let {entity ->
                    val distX = (entity.location.coords.x - location.coords.x).roundToInt()
                    val distY = (entity.location.coords.y - location.coords.y).roundToInt()
                    if (distX == 0 && distY == 0){
                        targetId = null
                    } else {
                        val dx = if (distX > 0) 1 else if (distX < 0) -1 else 0
                        val dy = if (distY > 0) 1 else if (distY < 0) -1 else 0
                        if(dx != 0){
                            world.enqueueAction(
                                playerId = id,
                                Action.MoveEntity(
                                    dx = dx,
                                    dy = 0
                                )
                            )
                        }
                        if(dy != 0){
                            world.enqueueAction(
                                playerId = id,
                                Action.MoveEntity(
                                    dx = 0,
                                    dy = dy
                                )
                            )
                        }
                    }
                }
            }
        }else{
            gameState.entities.filterIsInstance<EntityPlayer>().firstOrNull()?.let {
                targetId = it.id
            }
        }
    }

}