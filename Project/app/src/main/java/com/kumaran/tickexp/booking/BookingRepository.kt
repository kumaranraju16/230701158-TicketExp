package com.kumaran.tickexp.booking

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class BookingRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val userId: String? get() = auth.currentUser?.uid

    suspend fun getBookedSeats(showId: String): Result<Set<String>> {
        return try {
            val snapshot = firestore.collection("showSeats").document(showId).get().await()
            val booked = snapshot.get("bookedSeats")
                ?.let { value -> (value as? List<*>)?.filterIsInstance<String>() }
                .orEmpty()
                .toSet()
            Result.success(booked)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveBooking(record: BookingRecord, showId: String): Result<Unit> {
        val uid = userId ?: return Result.failure(Exception("User not logged in"))

        return try {
            firestore.runTransaction { transaction ->
                val seatsRef = firestore.collection("showSeats").document(showId)
                val seatsSnapshot = transaction.get(seatsRef)
                val alreadyBooked = seatsSnapshot.get("bookedSeats")
                    ?.let { value -> (value as? List<*>)?.filterIsInstance<String>() }
                    .orEmpty()
                    .toSet()

                val conflictingSeats = record.seats.filter { it in alreadyBooked }
                if (conflictingSeats.isNotEmpty()) {
                    throw IllegalStateException("Seats no longer available: ${conflictingSeats.joinToString()}")
                }

                val bookingRef = firestore.collection("bookings").document()
                val bookingData = hashMapOf(
                    "userId" to uid,
                    "title" to record.title,
                    "theatre" to record.theatre,
                    "timing" to record.timing,
                    "seats" to record.seats,
                    "totalAmount" to record.totalAmount,
                    "status" to record.status,
                    "showId" to showId,
                    "timestamp" to Timestamp.now()
                )

                transaction.set(bookingRef, bookingData)
                transaction.set(
                    seatsRef,
                    mapOf(
                        "bookedSeats" to FieldValue.arrayUnion(*record.seats.toTypedArray()),
                        "theatre" to record.theatre,
                        "timing" to record.timing,
                        "movieTitle" to record.title,
                        "updatedAt" to Timestamp.now()
                    ),
                    SetOptions.merge()
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBookingHistory(): Result<List<BookingRecord>> {
        val uid = userId ?: return Result.failure(Exception("User not logged in"))

        return try {
            val snapshot = firestore.collection("bookings")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val history = snapshot.documents.map { doc ->
                BookingRecord(
                    title = doc.getString("title") ?: "",
                    theatre = doc.getString("theatre") ?: "",
                    timing = doc.getString("timing") ?: "",
                    seats = doc.get("seats")
                        ?.let { value -> (value as? List<*>)?.filterIsInstance<String>() }
                        .orEmpty(),
                    totalAmount = doc.getLong("totalAmount")?.toInt() ?: 0,
                    status = doc.getString("status") ?: "CONFIRMED"
                )
            }
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
