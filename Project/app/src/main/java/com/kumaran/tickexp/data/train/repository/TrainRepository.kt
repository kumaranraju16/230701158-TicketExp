package com.kumaran.tickexp.data.train.repository

import com.kumaran.tickexp.data.train.model.TrainData
import com.kumaran.tickexp.data.train.remote.TrainApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TrainRepository {
    private val searchApi = Retrofit.Builder()
        .baseUrl("https://irctc1.p.rapidapi.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TrainApiService::class.java)

    private val statusApi = Retrofit.Builder()
        .baseUrl("https://indian-railway-irctc.p.rapidapi.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TrainApiService::class.java)

    private var cachedResults: List<TrainData>? = null
    private var lastQuery: String? = null

    suspend fun getTrains(apiKey: String, from: String, to: String, date: String): Result<List<TrainData>> {
        val query = "$from|$to|$date"
        if (query == lastQuery && cachedResults != null) {
            return Result.success(cachedResults!!)
        }

        return try {
            val response = searchApi.searchTrains(apiKey = apiKey, fromStationCode = from, toStationCode = to, dateOfJourney = date)
            if (response.status && response.data != null) {
                cachedResults = response.data
                lastQuery = query
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to fetch trains"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLiveStatus(apiKey: String, trainNo: String, date: String): Result<com.kumaran.tickexp.data.train.model.TrainStatusResponse> {
        return try {
            val response = statusApi.getTrainStatus(apiKey = apiKey, trainNo = trainNo, date = date)
            if (response.status) {
                Result.success(response)
            } else {
                Result.failure(Exception("Failed to fetch live status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
