package com.kumaran.tickexp.data.model

data class NearbyTheatre(
    val id: String,
    val name: String,
    val address: String,
    val rating: Double
)

data class TheatreResponse(
    val theatres: List<NearbyTheatre>
)
