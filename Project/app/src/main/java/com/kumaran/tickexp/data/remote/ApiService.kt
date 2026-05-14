package com.kumaran.tickexp.data.remote

import com.kumaran.tickexp.data.model.MovieResponse
import com.kumaran.tickexp.data.model.TheatreResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    // Using your Vercel backend endpoint which already includes regional filtering
    @GET("movies")
    suspend fun getNowPlayingMovies(): MovieResponse

    @GET("theatres")
    suspend fun getTheatres(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): TheatreResponse
}
