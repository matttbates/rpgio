package common.entities

import common.Coords
import common.Facing
import common.HitBox
import common.Location
import kotlinx.serialization.Serializable

@Serializable
sealed interface Entity {
    val id: Int
    var location: Location
    var facing: Facing
    fun getSprite(): String
    var animI: Int
    val hitBox: HitBox
    fun intersectsWith(domain: Pair<Float, Float>, range: Pair<Float, Float>): Boolean{
        val (domainMin, domainMax) = getDomain()
        val (rangeMin, rangeMax) = getRange()
        val (otherDomainMin, otherDomainMax) = domain
        val (otherRangeMin, otherRangeMax) = range

        val otherCornerInside = listOf(
            otherDomainMin to otherRangeMin,
            otherDomainMin to otherRangeMax,
            otherDomainMax to otherRangeMin,
            otherDomainMax to otherRangeMax
        ).any { (oX, oY) ->
            oX in domainMin..domainMax && oY in rangeMin .. rangeMax
        }

        val cornerInsideOther = listOf(
            getTopLeft(),
            getTopRight(),
            getBottomLeft(),
            getBottomRight()
        ).any { (x, y) ->
            x in otherDomainMin .. otherDomainMax && y in otherRangeMin .. otherRangeMax
        }

        return otherCornerInside || cornerInsideOther
    }
    fun getDomain(): Pair<Float, Float>{
        return location.coords.x + hitBox.fromLeft to location.coords.x + 1 - hitBox.fromRight
    }
    fun getDomainAt(newCoords: Coords): Pair<Float, Float>{
        return newCoords.x + hitBox.fromLeft to newCoords.x + 1 - hitBox.fromRight
    }
    fun getRange(): Pair<Float, Float>{
        return location.coords.y + hitBox.fromTop to location.coords.y + 1 - hitBox.fromBottom
    }
    fun getRangeAt(newCoords: Coords): Pair<Float, Float>{
        return newCoords.y + hitBox.fromTop to newCoords.y + 1 - hitBox.fromBottom
    }
    fun getCenter(): Pair<Float, Float>{
        return location.coords.x + hitBox.fromLeft + (((1 - hitBox.fromRight) - hitBox.fromLeft) / 2) to location.coords.y + hitBox.fromTop + (((1 - hitBox.fromBottom) - hitBox.fromTop) / 2)
    }
    fun getTopLeft(): Pair<Float, Float>{
        return location.coords.x + hitBox.fromLeft to location.coords.y + hitBox.fromTop
    }
    fun getTopRight(): Pair<Float, Float>{
        return location.coords.x + 1 - hitBox.fromRight to location.coords.y + hitBox.fromTop
    }
    fun getBottomLeft(): Pair<Float, Float>{
        return location.coords.x + hitBox.fromLeft to location.coords.y + 1 - hitBox.fromBottom
    }
    fun getBottomRight(): Pair<Float, Float>{
        return location.coords.x + 1 - hitBox.fromRight to location.coords.y + 1 - hitBox.fromBottom
    }
}