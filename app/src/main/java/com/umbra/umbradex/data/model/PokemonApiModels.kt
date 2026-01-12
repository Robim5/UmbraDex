package com.umbra.umbradex.data.model

import com.google.gson.annotations.SerializedName

data class PokemonApiDto(
    val id: Int,
    val name: String,
    val height: Int,
    val weight: Int,
    val types: List<PokemonTypeSlot>,
    val sprites: PokemonSprites,
    val stats: List<PokemonStatDto>,
    val abilities: List<AbilitySlot>,
    val species: NamedApiResource,
    val cries: PokemonCries?
)

data class PokemonTypeSlot(
    val slot: Int,
    val type: NamedApiResource
)

data class NamedApiResource(
    val name: String,
    val url: String
)

data class PokemonSprites(
    @SerializedName("front_default") val frontDefault: String?,
    @SerializedName("other") val other: OtherSprites?
)

data class OtherSprites(
    @SerializedName("official-artwork") val officialArtwork: OfficialArtwork?
)

data class OfficialArtwork(
    @SerializedName("front_default") val frontDefault: String?,
    @SerializedName("front_shiny") val frontShiny: String?
)

data class PokemonStatDto(
    @SerializedName("base_stat") val baseStat: Int,
    val effort: Int,
    val stat: NamedApiResource
)

data class AbilitySlot(
    @SerializedName("is_hidden") val isHidden: Boolean,
    val slot: Int,
    val ability: NamedApiResource
)

data class PokemonCries(
    val latest: String?,
    val legacy: String?
)
data class PokemonSpeciesDto(
    val id: Int,
    val name: String,
    @SerializedName("flavor_text_entries") val flavorTextEntries: List<FlavorText>,
    @SerializedName("evolution_chain") val evolutionChain: EvolutionChainUrl,
    val color: NamedApiResource,
    val habitat: NamedApiResource?,
    @SerializedName("is_legendary") val isLegendary: Boolean,
    @SerializedName("is_mythical") val isMythical: Boolean,
    val generation: NamedApiResource
)

data class FlavorText(
    @SerializedName("flavor_text") val flavorText: String,
    val language: NamedApiResource,
    val version: NamedApiResource
)

data class EvolutionChainUrl(
    val url: String
)
data class EvolutionChainDto(
    val id: Int,
    val chain: ChainLink
)

data class ChainLink(
    val species: NamedApiResource,
    @SerializedName("evolution_details") val evolutionDetails: List<EvolutionDetail>,
    @SerializedName("evolves_to") val evolvesTo: List<ChainLink>
)

data class EvolutionDetail(
    @SerializedName("min_level") val minLevel: Int?,
    @SerializedName("trigger") val trigger: NamedApiResource,
    val item: NamedApiResource?,
    @SerializedName("held_item") val heldItem: NamedApiResource?,
    @SerializedName("known_move") val knownMove: NamedApiResource?,
    @SerializedName("time_of_day") val timeOfDay: String?
)