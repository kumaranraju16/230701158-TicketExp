package com.kumaran.tickexp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kumaran.tickexp.data.model.Movie
import com.kumaran.tickexp.repository.MovieRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.*

class MovieViewModel : ViewModel() {

    var movies by mutableStateOf<List<Movie>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    private val repository = MovieRepository()

    init {
        fetchMovies()
    }

    private fun fetchMovies() {
        viewModelScope.launch {
            try {
                // No longer passing API key here; the backend handles it!
                movies = repository.getMovies()
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }
}
