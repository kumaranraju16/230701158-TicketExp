package com.kumaran.tickexp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumaran.tickexp.data.model.NearbyTheatre
import com.kumaran.tickexp.repository.TheatreRepository
import kotlinx.coroutines.launch

class TheatreViewModel : ViewModel() {
    var theatres by mutableStateOf<List<NearbyTheatre>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    private val repository = TheatreRepository()

    fun loadTheatres(lat: Double, lng: Double) {
        viewModelScope.launch {
            isLoading = true
            try {
                val res = repository.getTheatres(lat, lng)
                theatres = res.theatres
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isLoading = false
        }
    }
}
