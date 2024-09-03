package entities

import Facing
import HitBox
import Location

interface Entity {
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
        return location.coords.first + hitBox.fromLeft to location.coords.first + 1 - hitBox.fromRight
    }
    fun getDomainAt(newCoords: Pair<Float, Float>): Pair<Float, Float>{
        return newCoords.first + hitBox.fromLeft to newCoords.first + 1 - hitBox.fromRight
    }
    fun getRange(): Pair<Float, Float>{
        return location.coords.second + hitBox.fromTop to location.coords.second + 1 - hitBox.fromBottom
    }
    fun getRangeAt(newCoords: Pair<Float, Float>): Pair<Float, Float>{
        return newCoords.second + hitBox.fromTop to newCoords.second + 1 - hitBox.fromBottom
    }
    fun getCenter(): Pair<Float, Float>{
        return location.coords.first + hitBox.fromLeft + (((1 - hitBox.fromRight) - hitBox.fromLeft) / 2) to location.coords.second + hitBox.fromTop + (((1 - hitBox.fromBottom) - hitBox.fromTop) / 2)
    }
    fun getTopLeft(): Pair<Float, Float>{
        return location.coords.first + hitBox.fromLeft to location.coords.second + hitBox.fromTop
    }
    fun getTopRight(): Pair<Float, Float>{
        return location.coords.first + 1 - hitBox.fromRight to location.coords.second + hitBox.fromTop
    }
    fun getBottomLeft(): Pair<Float, Float>{
        return location.coords.first + hitBox.fromLeft to location.coords.second + 1 - hitBox.fromBottom
    }
    fun getBottomRight(): Pair<Float, Float>{
        return location.coords.first + 1 - hitBox.fromRight to location.coords.second + 1 - hitBox.fromBottom
    }
}