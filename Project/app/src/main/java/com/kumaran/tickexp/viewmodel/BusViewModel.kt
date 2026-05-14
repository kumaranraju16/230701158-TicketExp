package com.kumaran.tickexp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class Bus(
    val id: String,
    val name: String,
    val type: String,
    val departure: String,
    val arrival: String,
    val duration: String,
    val price: Int,
    val rating: Double,
    val seatsAvailable: Int,
    val amenities: List<String> = listOf("wifi", "charging_station", "water_drop")
)

data class BusSeat(
    val id: String,
    val number: String,
    val isAvailable: Boolean,
    val isSleeper: Boolean = false
)

data class BusUiState(
    val fromCity: String = "Chennai",
    val toCity: String = "Bangalore",
    val journeyDate: String = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date()),
    val passengers: Int = 1,
    val buses: List<Bus> = emptyList(),
    val isLoading: Boolean = false,
    val selectedBus: Bus? = null,
    val seats: List<BusSeat> = emptyList(),
    val selectedSeats: List<String> = emptyList(),
    val bookingId: String = ""
)

class BusViewModel : ViewModel() {
    var state by mutableStateOf(BusUiState())
        private set

    private val indianBusNames = listOf(
        "KPN Travels", "SRS Travels", "Parveen Travels", "National Travels", 
        "Orange Tours", "VRL Travels", "Jabbar Travels", "SVKDT Travels"
    )

    private val busTypes = listOf(
        "AC Sleeper (2+1)", "Semi-Sleeper Luxury", "Business Class AC", "Scania Multi-Axle AC"
    )

    fun updateFrom(city: String) { state = state.copy(fromCity = city) }
    fun updateTo(city: String) { state = state.copy(toCity = city) }
    fun updateDate(date: String) { state = state.copy(journeyDate = date) }
    fun swapCities() { state = state.copy(fromCity = state.toCity, toCity = state.fromCity) }

    fun searchBuses() {
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            delay(1500) // Shimmer effect simulation
            
            val simulatedBuses = List(8) { index ->
                Bus(
                    id = index.toString(),
                    name = indianBusNames.random(),
                    type = busTypes.random(),
                    departure = String.format("%02d:%02d", Random.nextInt(18, 23), listOf(0, 15, 30, 45).random()),
                    arrival = String.format("%02d:%02d", Random.nextInt(4, 9), listOf(0, 15, 30, 45).random()),
                    duration = "${Random.nextInt(6, 10)}h ${listOf(0, 15, 30, 45).random()}m",
                    price = Random.nextInt(800, 1500),
                    rating = 3.5 + Random.nextDouble(1.5),
                    seatsAvailable = Random.nextInt(2, 25)
                )
            }.sortedBy { it.price }
            
            state = state.copy(buses = simulatedBuses, isLoading = false)
        }
    }

    fun selectBus(bus: Bus) {
        state = state.copy(selectedBus = bus, selectedSeats = emptyList())
        generateSeats()
    }

    private fun generateSeats() {
        val totalSeats = 36
        val seats = List(totalSeats) { i ->
            val row = (i / 4) + 1
            val col = when (i % 4) {
                0 -> "A"
                1 -> "B"
                2 -> "C"
                else -> "D"
            }
            BusSeat(
                id = "$row$col",
                number = "$row$col",
                isAvailable = Random.nextFloat() > 0.3f,
                isSleeper = Random.nextBoolean()
            )
        }
        state = state.copy(seats = seats)
    }

    fun toggleSeat(seatId: String) {
        val current = state.selectedSeats.toMutableList()
        if (current.contains(seatId)) {
            current.remove(seatId)
        } else {
            current.add(seatId)
        }
        state = state.copy(selectedSeats = current)
    }

    fun confirmBooking() {
        state = state.copy(bookingId = "TXE-" + Random.nextInt(1000000, 9999999) + "-B")
    }
}
