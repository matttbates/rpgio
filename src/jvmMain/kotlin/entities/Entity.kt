package entities

interface Entity {
    val id: Int
    var coords: Pair<Float, Float>
    var rotation: Float
    val appearance: Char
}