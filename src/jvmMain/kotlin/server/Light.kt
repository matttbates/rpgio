package server

import server.maps.LightMode
import server.maps.MapData
import kotlin.math.cos

class Light(
    private val time: RpgIoTime
) {

    fun calculateLightLevel(mapData: MapData): Float {
        return when (mapData.lightMode) {
            LightMode.NATURAL -> lightFromTicks()
            LightMode.LIGHT -> 1.0f
            LightMode.DARK -> 0.5f
        }
    }

    private fun lightFromTicks(): Float {
        val time = time.getPercentOfDay()
        val verticalStretch = 0.75f
        val horizontalShift = 1/12f
        return ((1f - cos((time - horizontalShift) * (2 * Math.PI)).toFloat()) * verticalStretch).coerceIn(0.5f, 1.0f)
    }

}