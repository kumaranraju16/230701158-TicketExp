package com.kumaran.tickexp.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

data class Movie(
    val name: String,
    val genre: String,
    val duration: String,
    val rating: String,
    val format: String = "2D",
    val pricePerSeat: Int = 200,
    val synopsis: String = "In a future where love is guaranteed by corporate conglomerates, a rogue insurance agent discovers a glitch in the algorithm that predicts human heartbreak.",
    val posterUrl: String? = null
)

data class BookingDate(
    val dayLabel: String,
    val dateLabel: String,
    val fullLabel: String
)

data class ShowTime(
    val time: String,
    val status: String // AVAILABLE, FAST, SOLD
)

data class Theatre(
    val id: String,
    val name: String,
    val location: String,
    val distance: String,
    val rating: Double? = null,
    val matchScore: Int? = null,
    val shows: List<ShowTime>
)

enum class SeatState { AVAILABLE, SELECTED, BOOKED }

data class Seat(
    val id: String,
    val row: String,
    val number: Int,
    val state: SeatState = SeatState.AVAILABLE
)

data class BookingRecord(
    val title: String,
    val theatre: String,
    val timing: String,
    val seats: List<String>,
    val totalAmount: Int,
    val status: String = "CONFIRMED"
)

data class BookingUiState(
    val currentMovie: Movie? = null,
    val seatLayout: List<Seat> = emptyList(),
    val bookingHistory: List<BookingRecord> = emptyList(),
    val isLoading: Boolean = false,
    val theatres: List<Theatre> = emptyList(),
    val availableDates: List<BookingDate> = emptyList(),
    val selectedDateIndex: Int = 0,
    val selectedTheatre: Theatre? = null,
    val selectedShowTime: ShowTime? = null,
    val latestBooking: BookingRecord? = null,
    val bookingError: String? = null,
    val nearbyTheatres: List<Theatre> = emptyList()
) {
    val selectedSeats: List<String> get() = seatLayout.filter { it.state == SeatState.SELECTED }.map { it.id }
    val totalAmount: Int get() = (currentMovie?.pricePerSeat ?: 0) * selectedSeats.size
    val selectedDate: BookingDate? get() = availableDates.getOrNull(selectedDateIndex)
    val showId: String? get() {
        val m = currentMovie ?: return null
        val t = selectedTheatre ?: return null
        val d = selectedDate ?: return null
        val s = selectedShowTime ?: return null
        return buildShowId(m.name, t.name, d.fullLabel, s.time)
    }
}

val movieCatalog = listOf(
    Movie("LIK: Love Insurance Kompany", "Action / Sci-Fi", "2h 45m", "U/A", "2D", 150),
    Movie("Project Hail Mary", "Sci-Fi", "2h 14m", "UA13+", "IMAX 2D", 200),
    Movie("Leader", "Drama", "1h 58m", "U/A", "4K Atmos", 180),
    Movie("The Kinetic", "Action", "2h 30m", "A", "Laser", 250),
    Movie("Neon Pulse", "Thriller", "2h 02m", "UA16+", "Dolby 7.1", 220)
)

class BookingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BookingUiState(
        availableDates = listOf(
            BookingDate("Mon", "18", "Mon, 18 Mar 2026"),
            BookingDate("Tue", "19", "Tue, 19 Mar 2026"),
            BookingDate("Wed", "20", "Wed, 20 Mar 2026"),
            BookingDate("Thu", "21", "Thu, 21 Mar 2026"),
            BookingDate("Fri", "22", "Fri, 22 Mar 2026"),
            BookingDate("Sat", "23", "Sat, 23 Mar 2026")
        )
    ))
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()
    private val repository = BookingRepository()

    init { fetchBookingHistory() }

    fun fetchBookingHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getBookingHistory().onSuccess { history ->
                _uiState.update { it.copy(bookingHistory = history, isLoading = false) }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectDate(index: Int) { 
        _uiState.update { it.copy(selectedDateIndex = index, selectedTheatre = null, selectedShowTime = null, seatLayout = emptyList()) } 
    }

    fun selectShow(theatre: Theatre, show: ShowTime) {
        _uiState.update { it.copy(selectedTheatre = theatre, selectedShowTime = show, isLoading = true) }
        viewModelScope.launch {
            val sId = _uiState.value.showId ?: return@launch
            repository.getBookedSeats(sId).onSuccess { booked ->
                _uiState.update { it.copy(seatLayout = buildSeatLayout(booked), isLoading = false) }
            }.onFailure {
                _uiState.update { it.copy(seatLayout = buildSeatLayout(emptySet()), isLoading = false) }
            }
        }
    }

    fun startMovieBooking(movieName: String, posterUrl: String? = null) {
        val movie = movieCatalog.firstOrNull { it.name == movieName } ?: Movie(movieName, "Trending", "2h 15m", "U/A", posterUrl = posterUrl)
        val theatreList = if (_uiState.value.nearbyTheatres.isNotEmpty()) _uiState.value.nearbyTheatres else buildTheatres()
        _uiState.update { it.copy(currentMovie = movie, theatres = theatreList, selectedTheatre = null, selectedShowTime = null, seatLayout = emptyList(), bookingError = null) }
    }

    fun updateNearbyTheatres(theatres: List<com.kumaran.tickexp.data.model.NearbyTheatre>) {
        val mapped = theatres.map { nt ->
            Theatre(
                id = nt.id,
                name = nt.name,
                location = nt.address,
                distance = "Nearby",
                rating = nt.rating,
                matchScore = (nt.rating * 20).toInt(),
                shows = listOf(
                    ShowTime("02:30 PM", "AVAILABLE"),
                    ShowTime("06:15 PM", "FAST"),
                    ShowTime("10:00 PM", "AVAILABLE")
                )
            )
        }
        _uiState.update { it.copy(nearbyTheatres = mapped) }
    }

    fun toggleSeat(seatId: String) {
        _uiState.update { current ->
            val updated = current.seatLayout.map { seat ->
                if (seat.id == seatId && seat.state != SeatState.BOOKED) {
                    seat.copy(state = if (seat.state == SeatState.SELECTED) SeatState.AVAILABLE else SeatState.SELECTED)
                } else seat
            }
            current.copy(seatLayout = updated)
        }
    }

    fun confirmCurrentBooking() {
        val current = _uiState.value
        val record = BookingRecord(
            title = current.currentMovie?.name ?: "",
            theatre = current.selectedTheatre?.name ?: "",
            timing = "${current.selectedDate?.fullLabel} • ${current.selectedShowTime?.time}",
            seats = current.selectedSeats,
            totalAmount = current.totalAmount
        )
        val sId = current.showId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.saveBooking(record, sId).onSuccess {
                _uiState.update { state -> 
                    state.copy(
                        latestBooking = record,
                        isLoading = false,
                        seatLayout = state.seatLayout.map { if (it.state == SeatState.SELECTED) it.copy(state = SeatState.BOOKED) else it }
                    )
                }
                fetchBookingHistory()
            }.onFailure { _uiState.update { it.copy(isLoading = false) } }
        }
    }

    private fun buildTheatres(): List<Theatre> = listOf(
        Theatre(id = "1", name = "Sangam Cinemas", location = "Anna Salai", distance = "2.4 KM", rating = null, matchScore = 98, shows = listOf(ShowTime("03:10 PM", "SOLD"), ShowTime("06:45 PM", "FAST"), ShowTime("10:30 PM", "AVAILABLE"))),
        Theatre(id = "2", name = "EGA Cinemas", location = "Kilpauk", distance = "4.1 KM", rating = null, matchScore = null, shows = listOf(ShowTime("02:30 PM", "AVAILABLE"), ShowTime("07:15 PM", "AVAILABLE"))),
        Theatre(id = "3", name = "INOX National", location = "Mylapore", distance = "5.8 KM", rating = null, matchScore = null, shows = listOf(ShowTime("05:20 PM", "SOLD"), ShowTime("09:40 PM", "FAST")))
    )

    private fun buildSeatLayout(booked: Set<String>): List<Seat> {
        return ('A'..'P').flatMap { row -> (1..18).map { num ->
            val id = "$row$num"
            Seat(id, row.toString(), num, if (id in booked) SeatState.BOOKED else SeatState.AVAILABLE)
        } }
    }
}

private fun buildShowId(movie: String, theatre: String, date: String, show: String): String {
    return "$movie-$theatre-$date-$show".lowercase().replace(Regex("[^a-z0-9]"), "-").replace(Regex("-+"), "-").trim('-')
}
