package com.kumaran.tickexp.data.train.remote

import com.kumaran.tickexp.data.train.model.TrainSearchResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface TrainApiService {
    @GET("trains/betweenStations")
    suspend fun searchTrains(
        @Header("X-RapidAPI-Key") apiKey: String,
        @Header("X-RapidAPI-Host") apiHost: String = "irctc1.p.rapidapi.com",
        @Query("fromStationCode") fromStationCode: String,
        @Query("toStationCode") toStationCode: String,
        @Query("dateOfJourney") dateOfJourney: String // YYYY-MM-DD
    ): TrainSearchResponse

    @GET("api/trains/v1/train/status")
    suspend fun getTrainStatus(
        @Header("X-RapidAPI-Key") apiKey: String,
        @Header("X-RapidAPI-Host") apiHost: String = "indian-railway-irctc.p.rapidapi.com",
        @Query("train_number") trainNo: String,
        @Query("departure_date") date: String // YYYYMMDD
    ): com.kumaran.tickexp.data.train.model.TrainStatusResponse
}
