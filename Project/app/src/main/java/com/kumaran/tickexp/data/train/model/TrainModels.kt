package com.kumaran.tickexp.data.train.model

import com.google.gson.annotations.SerializedName

data class TrainSearchResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: List<TrainData>?
)

data class TrainData(
    @SerializedName("train_name") val trainName: String,
    @SerializedName("train_number") val trainNumber: String,
    @SerializedName("from_station_name") val fromStation: String,
    @SerializedName("to_station_name") val toStation: String,
    @SerializedName("run_days") val runDays: List<String>,
    @SerializedName("train_type") val trainType: String,
    @SerializedName("duration") val duration: String,
    @SerializedName("from_std") val departureTime: String,
    @SerializedName("to_std") val arrivalTime: String,
    @SerializedName("class_type") val classes: List<String>
)

data class TrainStatusResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("body") val body: TrainStatusBody
)

data class TrainStatusBody(
    @SerializedName("current_station") val currentStation: String,
    @SerializedName("stations") val stations: List<TrainStationStatus>
)

data class TrainStationStatus(
    @SerializedName("station_name") val stationName: String,
    @SerializedName("arrival_time") val arrivalTime: String
)
