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
}