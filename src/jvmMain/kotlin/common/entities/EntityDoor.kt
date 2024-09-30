package common.entities

import common.Facing
import common.HitBox
import common.Location
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class EntityDoor(
    override var location: Location,
    val destination: Location,
): Entity{

    override val id: Int = UUID.randomUUID().hashCode()
    override var facing = Facing.DOWN

    override fun getSprite() = "entities/door/entity_door.png"

    override var animI = 0

    override val hitBox = HitBox(
        0f, 0f, 0f, 0f
    )
}
