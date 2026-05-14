package com.kumaran.tickexp.repository

import com.kumaran.tickexp.data.model.Movie
import com.kumaran.tickexp.data.remote.RetrofitInstance

class MovieRepository {

    suspend fun getMovies(): List<Movie> {
        return RetrofitInstance.api.getNowPlayingMovies().results
    }
}
