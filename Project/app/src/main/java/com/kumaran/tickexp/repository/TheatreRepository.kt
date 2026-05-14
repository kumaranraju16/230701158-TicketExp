package com.kumaran.tickexp.repository

import com.kumaran.tickexp.data.model.TheatreResponse
import com.kumaran.tickexp.data.remote.RetrofitInstance

class TheatreRepository {
    suspend fun getTheatres(lat: Double, lng: Double): TheatreResponse {
        return RetrofitInstance.api.getTheatres(lat, lng)
    }
}
