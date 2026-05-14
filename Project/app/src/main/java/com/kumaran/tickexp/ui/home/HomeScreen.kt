package com.kumaran.tickexp.ui.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

//////////////////////////////////////////////////////
// 🏠 HOME SCREEN
//////////////////////////////////////////////////////

@Composable
fun HomeScreen() {

    val movies = listOf("Leo", "Jailer", "Vikram", "Master")
    val events = listOf("Concert", "Standup", "Festival")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        //////////////////////////////////////////////////////
        // 👋 GREETING
        //////////////////////////////////////////////////////
        Text(
            text = "Hello, Kumaran 👋",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        //////////////////////////////////////////////////////
        // 🔍 SEARCH BAR
        //////////////////////////////////////////////////////
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color(0xFF121212), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("Search movies, events...", color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(24.dp))

        //////////////////////////////////////////////////////
        // 🎬 TRENDING MOVIES
        //////////////////////////////////////////////////////
        Text(
            "Trending Movies",
            color = Color(0xFFC084FC),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow {
            items(movies) { movie ->
                MovieCard(movie)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        //////////////////////////////////////////////////////
        // 🎤 EVENTS
        //////////////////////////////////////////////////////
        Text(
            "Events & Concerts",
            color = Color(0xFFC084FC),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow {
            items(events) { event ->
                EventCard(event)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        //////////////////////////////////////////////////////
        // 🚆 TRANSPORT
        //////////////////////////////////////////////////////
        Text(
            "Quick Booking",
            color = Color(0xFFC084FC),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            TransportButton("Train")
            TransportButton("Bus")
        }
    }
}

//////////////////////////////////////////////////////
// 🎬 MOVIE CARD
//////////////////////////////////////////////////////

@Composable
fun MovieCard(title: String) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .padding(end = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .height(180.dp)
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, color = Color.White)
    }
}

//////////////////////////////////////////////////////
// 🎤 EVENT CARD
//////////////////////////////////////////////////////

@Composable
fun EventCard(title: String) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(100.dp)
            .padding(end = 12.dp)
            .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(title, color = Color.White)
    }
}

//////////////////////////////////////////////////////
// 🚆 TRANSPORT BUTTON
//////////////////////////////////////////////////////

@Composable
fun TransportButton(title: String) {
    Box(
        modifier = Modifier
            .width(150.dp)
            .height(80.dp)
            .background(Color(0xFFC084FC), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(title, color = Color.Black, fontWeight = FontWeight.Bold)
    }
}
