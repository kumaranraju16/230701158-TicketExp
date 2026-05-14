package com.kumaran.tickexp.data.model

data class Ticket(
    val id: String,
    val type: String, // "Movie", "Bus", "Train"
    val title: String,
    val source: String = "",
    val destination: String = "",
    val date: String,
    val time: String = "",
    val seat: String,
    val price: Int,
    val qrData: String,
    val theatre: String = "",
    val status: String = "Confirmed",
    val bookingTime: Long = System.currentTimeMillis()
)
