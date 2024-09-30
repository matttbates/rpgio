package entities

import kotlinx.serialization.Serializable

@Serializable
data class EntitiesJson(
    val entities: List<Entity> = listOf(),
)