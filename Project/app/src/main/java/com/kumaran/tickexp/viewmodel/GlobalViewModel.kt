package com.kumaran.tickexp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.kumaran.tickexp.data.model.Ticket

data class GlobalUiState(
    val tickets: List<Ticket> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val points: Int = 0,
    val walletBalance: Int = 0,
    val isDemoMode: Boolean = false
)

class GlobalViewModel : ViewModel() {
    var state by mutableStateOf(GlobalUiState())
        private set

    fun toggleDemoMode() {
        state = state.copy(isDemoMode = !state.isDemoMode)
    }

    fun rechargeWallet(amount: Int) {
        state = state.copy(walletBalance = state.walletBalance + amount)
    }

    fun deductWallet(amount: Int): Boolean {
        if (state.walletBalance >= amount) {
            state = state.copy(walletBalance = state.walletBalance - amount)
            return true
        }
        return false
    }

    fun addTicket(ticket: Ticket) {
        state = state.copy(
            tickets = listOf(ticket) + state.tickets,
            points = state.points + (ticket.price / 10) // Earn 10% points
        )
    }

    fun addRecentSearch(search: String) {
        if (search.isBlank()) return
        val current = state.recentSearches.toMutableList()
        current.remove(search)
        current.add(0, search)
        state = state.copy(recentSearches = current.take(5))
    }
}
