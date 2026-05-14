package com.kumaran.tickexp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kumaran.tickexp.data.train.model.TrainData
import com.kumaran.tickexp.data.train.repository.TrainRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Station(val code: String, val name: String)

data class TrainUiState(
    val trains: List<TrainData> = emptyList(),
    val liveStatus: com.kumaran.tickexp.data.train.model.TrainStatusResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val fromStation: String = "MAS",
    val toStation: String = "SBC",
    val fromStationName: String = "Chennai Central",
    val toStationName: String = "Bangalore City",
    val fromSuggestions: List<Station> = emptyList(),
    val toSuggestions: List<Station> = emptyList(),
    val journeyDate: String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
    val selectedTrain: TrainData? = null,
    val selectedClass: String? = null,
    val passengerName: String = "KUMARAN",
    val passengerEmail: String = "kumaran@tickexp.com",
    val passengerPhone: String = "+91 98765 43210",
    val fare: Int = 0
)

class TrainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TrainRepository()
    private val _uiState = MutableStateFlow(TrainUiState())
    val uiState: StateFlow<TrainUiState> = _uiState.asStateFlow()

    private var allStations: List<Station> = emptyList()

    init {
        loadStations()
    }

    private fun loadStations() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = getApplication<Application>().assets.open("stations.json").bufferedReader().use { it.readText() }
                val type = object : TypeToken<List<Station>>() {}.type
                allStations = Gson().fromJson(json, type)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun searchFromStation(query: String) {
        if (query.length < 2) {
            _uiState.update { it.copy(fromSuggestions = emptyList()) }
            return
        }
        val filtered = allStations.filter { 
            it.name.contains(query, ignoreCase = true) || it.code.contains(query, ignoreCase = true)
        }.take(8)
        _uiState.update { it.copy(fromSuggestions = filtered) }
    }

    fun searchToStation(query: String) {
        if (query.length < 2) {
            _uiState.update { it.copy(toSuggestions = emptyList()) }
            return
        }
        val filtered = allStations.filter { 
            it.name.contains(query, ignoreCase = true) || it.code.contains(query, ignoreCase = true)
        }.take(8)
        _uiState.update { it.copy(toSuggestions = filtered) }
    }

    fun selectFromStation(station: Station) {
        _uiState.update { it.copy(fromStation = station.code, fromStationName = station.name, fromSuggestions = emptyList()) }
    }

    fun selectToStation(station: Station) {
        _uiState.update { it.copy(toStation = station.code, toStationName = station.name, toSuggestions = emptyList()) }
    }

    fun updateFromStation(station: String) {
        _uiState.update { it.copy(fromStation = station) }
    }

    fun updateToStation(station: String) {
        _uiState.update { it.copy(toStation = station) }
    }

    fun updateJourneyDate(date: String) {
        _uiState.update { it.copy(journeyDate = date) }
    }

    fun swapStations() {
        _uiState.update { it.copy(fromStation = it.toStation, toStation = it.fromStation) }
    }

    fun selectTrain(train: TrainData) {
        _uiState.update { it.copy(selectedTrain = train) }
    }

    fun selectClass(className: String, fare: Int) {
        _uiState.update { it.copy(selectedClass = className, fare = fare) }
    }

    fun updatePassengerDetails(name: String, email: String, phone: String) {
        _uiState.update { it.copy(passengerName = name, passengerEmail = email, passengerPhone = phone) }
    }

    private val routeMap = mapOf(
        "VM-MS" to listOf("12636", "12606", "12634"),
        "MS-VM" to listOf("12635", "12605", "12633"),
        "MAS-SBC" to listOf("12028", "12608", "12639"),
        "SBC-MAS" to listOf("12027", "12607", "12640"),
        "MS-TBM" to listOf("66001", "66003"),
        "MAS-CBE" to listOf("12673", "12675"),
        "MAS-TPJ" to listOf("12635", "12605")
    )

    private fun formatDateForStatus(apiDate: String): String {
        return apiDate.replace("-", "")
    }

    fun searchTrains() {
        val currentState = _uiState.value
        if (currentState.fromStation.isBlank() || currentState.toStation.isBlank()) {
            _uiState.update { it.copy(error = "Please enter both stations") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, trains = emptyList()) }
            
            // Primary strategy: Route Map + Live Status
            val routeKey = "${currentState.fromStation}-${currentState.toStation}"
            val trainNumbers = routeMap[routeKey] ?: emptyList()
            
            if (trainNumbers.isEmpty()) {
                // Fallback: Try general search API
                repository.getTrains(
                    apiKey = "735fb86b9a227cc4908d8a6e913f1d3c",
                    from = currentState.fromStation,
                    to = currentState.toStation,
                    date = currentState.journeyDate
                ).onSuccess { results ->
                    _uiState.update { it.copy(trains = results, isLoading = false) }
                }.onFailure { e ->
                    _uiState.update { it.copy(error = "No trains found for this route", isLoading = false) }
                }
                return@launch
            }

            val resultsList = mutableListOf<TrainData>()
            trainNumbers.forEach { tNo ->
                repository.getLiveStatus(
                    apiKey = "735fb86b9a227cc4908d8a6e913f1d3c",
                    trainNo = tNo,
                    date = formatDateForStatus(currentState.journeyDate)
                ).onSuccess { status ->
                    resultsList.add(
                        TrainData(
                            trainName = status.body.currentStation + " EXP",
                            trainNumber = tNo,
                            fromStation = currentState.fromStation,
                            toStation = currentState.toStation,
                            runDays = listOf("M", "T", "W", "T", "F", "S", "S"),
                            trainType = "Express",
                            duration = "4h 20m",
                            departureTime = "06:00",
                            arrivalTime = "10:20",
                            classes = listOf("SL", "3A", "2A")
                        )
                    )
                }
            }

            if (resultsList.isNotEmpty()) {
                _uiState.update { it.copy(trains = resultsList, isLoading = false) }
            } else {
                _uiState.update { it.copy(error = "Unable to fetch live data for this route", isLoading = false) }
            }
        }
    }

    fun fetchLiveStatus(trainNo: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getLiveStatus(
                apiKey = "735fb86b9a227cc4908d8a6e913f1d3c",
                trainNo = trainNo,
                date = "20260425" // Format YYYYMMDD
            ).onSuccess { status ->
                _uiState.update { it.copy(liveStatus = status, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: "Failed to fetch status", isLoading = false) }
            }
        }
    }
}
