package entities

import HitBox

interface Entity {
    val id: Int
    var coords: Pair<Float, Float>
    var rotation: Float
    val appearance: Char
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
        return coords.first + hitBox.fromLeft to coords.first + 1 - hitBox.fromRight
    }
    fun getDomainAt(newCoords: Pair<Float, Float>): Pair<Float, Float>{
        return newCoords.first + hitBox.fromLeft to newCoords.first + 1 - hitBox.fromRight
    }
    fun getRange(): Pair<Float, Float>{
        return coords.second + hitBox.fromTop to coords.second + 1 - hitBox.fromBottom
    }
    fun getRangeAt(newCoords: Pair<Float, Float>): Pair<Float, Float>{
        return newCoords.second + hitBox.fromTop to newCoords.second + 1 - hitBox.fromBottom
    }
    fun getTopLeft(): Pair<Float, Float>{
        return coords.first + hitBox.fromLeft to coords.second + hitBox.fromTop
    }
    fun getTopRight(): Pair<Float, Float>{
        return coords.first + 1 - hitBox.fromRight to coords.second + hitBox.fromTop
    }
    fun getBottomLeft(): Pair<Float, Float>{
        return coords.first + hitBox.fromLeft to coords.second + 1 - hitBox.fromBottom
    }
    fun getBottomRight(): Pair<Float, Float>{
        return coords.first + 1 - hitBox.fromRight to coords.second + 1 - hitBox.fromBottom
    }
}