package entities

import World.Companion.TPS
import tiles.Tile

class EntityPlayer(
    val id: Int,
    val speed: Float = 0.2f,
    override var coords: Pair<Float, Float>
) : Entity {
    override val appearance: Char = '@'
}