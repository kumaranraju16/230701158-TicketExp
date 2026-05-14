package com.kumaran.tickexp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.vector.ImageVector
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.firebase.FirebaseApp
import com.kumaran.tickexp.booking.*
import com.kumaran.tickexp.data.model.Movie as TmdbMovie
import com.kumaran.tickexp.data.train.model.TrainData
import com.kumaran.tickexp.utils.QRCodeGenerator
import com.kumaran.tickexp.viewmodel.*
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.util.Locale

val BgBlack = Color(0xFF0E0E0E)
val PrimaryPurple = Color(0xFFCC97FF)
val PrimaryPurpleDim = Color(0xFF9C48EA)
val TextGray = Color(0xFFABABAB)
val SurfaceLow = Color(0xFF131313)
val SurfaceHigh = Color(0xFF1F1F1F)
val SurfaceHighest = Color(0xFF262626)
val SpaceGrotesk = FontFamily.Default

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Bookings : Screen("bookings")
    data object Profile : Screen("profile")
    data object Location : Screen("location")
    data object Movies : Screen("movies")
    data object Trains : Screen("trains")
    data object TrainResults : Screen("trainResults")
    data object PassengerDetails : Screen("passengerDetails")
    data object ReviewJourney : Screen("reviewJourney")
    data object Buses : Screen("buses")
    data object BusResults : Screen("busResults")
    data object BusSeatSelection : Screen("busSeatSelection")
    data object BusConfirmation : Screen("busConfirmation")
    data object TrainStatus : Screen("trainStatus")
    data object TrainTracker : Screen("trainTracker")
}

@Immutable
data class Category(val title: String, val subtitle: String, val imageRes: Int, val route: String)
@Immutable
data class ProfileItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val onClick: () -> Unit = {})

val categories = listOf(
    Category("Movies", "Blockbusters & premieres", R.drawable.movies, Screen.Movies.route),
    Category("Trains", "Travel across cities", R.drawable.trains, Screen.Trains.route),
    Category("Buses", "Comfort journeys", R.drawable.buses, Screen.Buses.route)
)

val benefitItems = listOf(
    ProfileItem("Offers", Icons.Default.LocalOffer),
    ProfileItem("Gift Cards", Icons.Default.Redeem),
    ProfileItem("Food & Beverages", Icons.Default.Fastfood)
)

val generalItems = listOf(
    ProfileItem("List your Show", Icons.Default.Movie),
    ProfileItem("Account & Settings", Icons.Default.Settings),
    ProfileItem("Share", Icons.Default.Share),
    ProfileItem("Rate Us", Icons.Default.Star),
    ProfileItem("Terms & Conditions", Icons.Default.Description),
    ProfileItem("Privacy Policy", Icons.Default.Security)
)

class CityViewModel : androidx.lifecycle.ViewModel() {
    private val _city = MutableStateFlow("Chennai")
    val city: StateFlow<String> = _city
    fun updateCity(newCity: String) { _city.value = newCity }
}

class MainActivity : ComponentActivity(), PaymentResultListener {
    lateinit var bookingViewModel: BookingViewModel
    lateinit var trainViewModel: TrainViewModel
    lateinit var globalViewModel: GlobalViewModel
    private var navController: NavController? = null
    private var pendingRechargeAmount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        Checkout.preload(applicationContext)
        setContent {
            val controller = rememberNavController()
            navController = controller
            TickExpApp(controller)
        }
    }

    fun startPayment(amount: Int, description: String, isRecharge: Boolean = false) {
        if (isRecharge) pendingRechargeAmount = amount
        val checkout = Checkout()
        checkout.setKeyID("rzp_test_SZ0Ep5dMZ3hlxf")
        try {
            val options = JSONObject()
            options.put("name", "TickExp")
            options.put("description", description)
            options.put("theme.color", "#CC97FF")
            options.put("currency", "INR")
            options.put("amount", (amount * 100).toString())
            checkout.open(this, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Error in payment: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        val currentRoute = navController?.currentDestination?.route
        if (currentRoute == Screen.Profile.route && pendingRechargeAmount > 0) {
            globalViewModel.rechargeWallet(pendingRechargeAmount)
            pendingRechargeAmount = 0
            Toast.makeText(this, "Wallet Recharged Successfully", Toast.LENGTH_SHORT).show()
        } else if (currentRoute == Screen.ReviewJourney.route) {
            navController?.navigate("success")
        } else if (currentRoute == Screen.BusSeatSelection.route) {
            navController?.navigate(Screen.BusConfirmation.route)
        } else if (currentRoute == "payment") {
            bookingViewModel.confirmCurrentBooking()
            navController?.navigate("success")
        } else {
            // Fallback for movies if not on payment screen
            bookingViewModel.confirmCurrentBooking()
            navController?.navigate("success")
        }
        Toast.makeText(this, "Payment Successful", Toast.LENGTH_SHORT).show()
    }

    override fun onPaymentError(code: Int, response: String?) {
        Toast.makeText(this, "Payment Failed: $response", Toast.LENGTH_LONG).show()
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TickExpApp(navController: androidx.navigation.NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val cityViewModel: CityViewModel = viewModel()
    val bookingViewModel: BookingViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val trainViewModel: TrainViewModel = viewModel()
    val theatreViewModel: TheatreViewModel = viewModel()
    val busViewModel: BusViewModel = viewModel()
    val globalViewModel: GlobalViewModel = viewModel()
    val context = LocalContext.current
    (context as? MainActivity)?.let {
        it.bookingViewModel = bookingViewModel
        it.trainViewModel = trainViewModel
        it.globalViewModel = globalViewModel
    }

    // Global sync: Whenever theatres are detected, update the booking system
    val detectedTheatres = theatreViewModel.theatres
    LaunchedEffect(detectedTheatres) {
        if (detectedTheatres.isNotEmpty()) {
            bookingViewModel.updateNearbyTheatres(detectedTheatres)
        }
    }

    LaunchedEffect(authViewModel.isLoggedIn) {
        if (!authViewModel.isLoggedIn) {
            context.startActivity(Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    Scaffold(
        bottomBar = {
            val hideBottomBarRoutes = listOf(
                Screen.Location.route, "theatre/{movieName}", "seatSelection", "confirm", "payment", "success",
                Screen.TrainResults.route, Screen.PassengerDetails.route, Screen.ReviewJourney.route,
                Screen.BusResults.route, Screen.BusSeatSelection.route, Screen.BusConfirmation.route
            )
            if (currentRoute !in hideBottomBarRoutes) {
                AnimatedBottomNav(navController)
            }
        },
        containerColor = BgBlack
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(navController = navController, startDestination = Screen.Home.route) {
                composable(Screen.Home.route) { ScreenTransition { TickExpHome(navController, cityViewModel, globalViewModel) } }
                composable(Screen.Bookings.route) { ScreenTransition { BookingScreen(navController, bookingViewModel, globalViewModel) } }
                composable(Screen.Profile.route) { ScreenTransition { ProfileScreen(navController, authViewModel, globalViewModel) } }
                composable(Screen.Location.route) { ScreenTransition { LocationScreen(navController, cityViewModel, theatreViewModel) } }
                composable(Screen.Movies.route) { ScreenTransition { MovieListScreen(navController, cityViewModel) } }
                composable("theatre/{movieName}") { backStackEntry ->
                    val movieName = Uri.decode(backStackEntry.arguments?.getString("movieName") ?: "")
                    ScreenTransition { TheatreScreen(navController, movieName, bookingViewModel) }
                }
                composable("seatSelection") { ScreenTransition { SeatSelectionScreen(navController, bookingViewModel) } }
                composable("confirm") { ScreenTransition { ConfirmScreen(navController, bookingViewModel, globalViewModel) } }
                composable("payment") { ScreenTransition { PaymentScreen(navController, bookingViewModel, globalViewModel) } }
                composable("success") { ScreenTransition { BookingSuccessScreen(navController, bookingViewModel, globalViewModel) } }
                
                // Train Flow
                composable(Screen.Trains.route) { ScreenTransition { TrainSearchScreen(navController, trainViewModel, globalViewModel) } }
                composable(Screen.TrainResults.route) { ScreenTransition { TrainResultsScreen(navController, trainViewModel) } }
                composable(Screen.PassengerDetails.route) { ScreenTransition { PassengerDetailsScreen(navController, trainViewModel) } }
                composable(Screen.ReviewJourney.route) { ScreenTransition { ReviewJourneyScreen(navController, trainViewModel, globalViewModel) } }
                composable(Screen.TrainStatus.route) { ScreenTransition { TrainStatusScreen(navController, trainViewModel) } }

                // Bus Flow
                composable(Screen.Buses.route) { ScreenTransition { BusSearchScreen(navController, busViewModel, globalViewModel) } }
                composable(Screen.BusResults.route) { ScreenTransition { BusResultsScreen(navController, busViewModel) } }
                composable(Screen.BusSeatSelection.route) { ScreenTransition { BusSeatSelectionScreen(navController, busViewModel, globalViewModel) } }
                composable(Screen.BusConfirmation.route) { ScreenTransition { BusConfirmationScreen(navController, busViewModel, globalViewModel) } }
            }
        }
    }
}

// --- Bus UI Components ---

@Composable
fun BusSearchScreen(navController: NavController, viewModel: BusViewModel, globalViewModel: GlobalViewModel) {
    val state = viewModel.state
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val context = LocalContext.current

    // Demo Mode Pre-fill
    LaunchedEffect(globalViewModel.state.isDemoMode) {
        if (globalViewModel.state.isDemoMode) {
            viewModel.updateFrom("Chennai")
            viewModel.updateTo("Bangalore")
        }
    }
    
    Column(modifier = Modifier.fillMaxSize().background(BgBlack).verticalScroll(rememberScrollState())) {
        // Custom Header
        Row(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(SurfaceLow).clickable { navController.popBackStack() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Menu, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(20.dp))
            }
            Text("Bus Search", style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White), modifier = Modifier.padding(start = 16.dp).weight(1f))
            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(28.dp))
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            // Hero Visual
            Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(24.dp))) {
                AsyncImage(model = "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&q=80&w=800", contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.6f)
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, BgBlack))))
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                    Text("SPEED. COMFORT.", style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Black, fontSize = 32.sp, color = Color.White, fontStyle = FontStyle.Italic))
                    Text("THE VELOCITY OF LIGHT", style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Light, fontSize = 12.sp, color = PrimaryPurple, letterSpacing = 2.sp))
                }
            }

            Spacer(Modifier.height(32.dp))

            // Search Bento
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SurfaceLow).border(1.dp, PrimaryPurple.copy(alpha = 0.1f), RoundedCornerShape(24.dp)).padding(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // From
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { /* Suggestion logic */ }) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(24.dp))
                        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                            Text("FROM", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            TextField(value = state.fromCity, onValueChange = { viewModel.updateFrom(it) }, placeholder = { Text("Enter departure city", color = TextGray) }, colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), textStyle = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold), modifier = Modifier.fillMaxWidth())
                        }
                    }
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    // To
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { /* Suggestion logic */ }) {
                        Icon(Icons.Default.NearMe, contentDescription = null, tint = Color(0xFFE197FC), modifier = Modifier.size(24.dp))
                        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                            Text("TO", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            TextField(value = state.toCity, onValueChange = { viewModel.updateTo(it) }, placeholder = { Text("Enter destination city", color = TextGray) }, colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), textStyle = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold), modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                
                // Swap Button
                Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).size(48.dp).clip(CircleShape).background(PrimaryPurple).clickable { viewModel.swapCities() }.shadow(10.dp, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.SwapVert, contentDescription = null, tint = Color.Black)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Date & Metadata
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    modifier = Modifier.weight(1f).clickable {
                        val calendar = java.util.Calendar.getInstance()
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val cal = java.util.Calendar.getInstance()
                                cal.set(year, month, dayOfMonth)
                                val dateStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(cal.time)
                                viewModel.updateDate(dateStr)
                            },
                            calendar.get(java.util.Calendar.YEAR),
                            calendar.get(java.util.Calendar.MONTH),
                            calendar.get(java.util.Calendar.DAY_OF_MONTH)
                        ).show()
                    }, 
                    shape = RoundedCornerShape(24.dp), 
                    color = SurfaceLow, 
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(20.dp))
                        Text(state.journeyDate, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp), color = SurfaceLow, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(20.dp))
                        Text("${state.passengers} Passenger", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            // Search CTA
            Button(
                onClick = { 
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    if (state.fromCity.isNotEmpty() && state.toCity.isNotEmpty()) {
                        globalViewModel.addRecentSearch("${state.fromCity} → ${state.toCity}")
                    }
                    viewModel.searchBuses()
                    navController.navigate(Screen.BusResults.route)
                },
                modifier = Modifier.fillMaxWidth().height(72.dp).shadow(20.dp, RoundedCornerShape(36.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(36.dp)
            ) {
                Text("Search Buses", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Black)
            }

            Spacer(Modifier.height(48.dp))
            
            // Recent Searches
            Text("RECENT SEARCHES", style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White, letterSpacing = 1.sp))
            Spacer(Modifier.height(16.dp))
            RecentBusSearchItem("Chennai → Bangalore", "2 Passengers • Sleeper")
            RecentBusSearchItem("Mumbai → Pune", "1 Passenger • AC Seater")
            RecentBusSearchItem("Delhi → Jaipur", "1 Passenger • Semi-Sleeper")
            
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
fun RecentBusSearchItem(title: String, subtitle: String) {
    Surface(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(16.dp), color = SurfaceLow, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(PrimaryPurple.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.History, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = TextGray, fontSize = 12.sp)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = TextGray, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun BusResultsScreen(navController: NavController, viewModel: BusViewModel) {
    val state = viewModel.state
    
    Column(modifier = Modifier.fillMaxSize().background(BgBlack)) {
        // Header
        Surface(modifier = Modifier.fillMaxWidth(), color = SurfaceLow.copy(alpha = 0.8f), shadowElevation = 8.dp) {
            Row(modifier = Modifier.padding(24.dp).padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = PrimaryPurple)
                }
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text("${state.fromCity} → ${state.toCity}".ifEmpty { "MAS → BLR" }, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("${state.journeyDate} • ${state.passengers} Passenger", color = TextGray, fontSize = 10.sp, letterSpacing = 1.sp)
                }
                Icon(Icons.Default.FilterList, contentDescription = null, tint = PrimaryPurple)
            }
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryPurple)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 24.dp)) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${state.buses.size} PREMIUM BUSES FOUND", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Surface(shape = RoundedCornerShape(12.dp), color = SurfaceLow, border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.2f))) {
                            Text("FASTEST", color = PrimaryPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                        }
                    }
                }
                
                items(state.buses) { bus ->
                    BusCard(bus) {
                        viewModel.selectBus(bus)
                        navController.navigate(Screen.BusSeatSelection.route)
                    }
                }
            }
        }
    }
}

@Composable
fun BusCard(bus: Bus, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(24.dp), color = SurfaceLow, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(bus.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Text(bus.type, color = TextGray, fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(TextGray))
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFF95A0), modifier = Modifier.size(14.dp))
                        Text(bus.rating.toString(), color = Color(0xFFFF95A0), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("₹${bus.price}", color = PrimaryPurple, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
                    Text("ALL INCLUSIVE", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Timeline
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(bus.departure, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Origin", color = TextGray, fontSize = 12.sp)
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(bus.duration, color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f))) {
                        Box(modifier = Modifier.align(Alignment.Center).size(6.dp).clip(CircleShape).background(PrimaryPurple).shadow(4.dp, CircleShape))
                    }
                    Text("DIRECT", color = PrimaryPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(bus.arrival, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Destination", color = TextGray, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EventSeat, contentDescription = null, tint = Color(0xFFFF6E84), modifier = Modifier.size(16.dp))
                    Text("${bus.seatsAvailable} Seats Left", color = Color(0xFFFF6E84), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                }
                Button(onClick = onClick, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)) {
                    Text("View Seats", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TrainSearchScreen(navController: NavController, viewModel: TrainViewModel, globalViewModel: GlobalViewModel) {
    val state by viewModel.uiState.collectAsState()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    // Demo Mode Pre-fill
    LaunchedEffect(globalViewModel.state.isDemoMode) {
        if (globalViewModel.state.isDemoMode) {
            viewModel.selectFromStation(com.kumaran.tickexp.viewmodel.Station("MS", "Chennai Egmore"))
            viewModel.selectToStation(com.kumaran.tickexp.viewmodel.Station("VM", "Villupuram Jn"))
        }
    }
    
    var showStationPicker by remember { mutableStateOf<String?>(null) } // "from" or "to"
    val stations = listOf("VM" to "Villupuram Jn", "MS" to "Chennai Egmore", "MAS" to "Chennai Central", "MDU" to "Madurai Jn", "CBE" to "Coimbatore Jn", "TPJ" to "Trichy Jn")
    
    val displayDates = listOf("Sat 25 Apr" to "2026-04-25", "Sun 26 Apr" to "2026-04-26", "Mon 27 Apr" to "2026-04-27", "Tue 28 Apr" to "2026-04-28")
    
    if (showStationPicker != null) {
        var query by remember { mutableStateOf("") }
        val suggestions = if (showStationPicker == "from") state.fromSuggestions else state.toSuggestions

        AlertDialog(
            onDismissRequest = { showStationPicker = null },
            containerColor = BgBlack,
            title = {
                Column {
                    Text("Select ${if (showStationPicker == "from") "Origin" else "Destination"}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    TextField(
                        value = query,
                        onValueChange = { 
                            query = it
                            if (showStationPicker == "from") viewModel.searchFromStation(it)
                            else viewModel.searchToStation(it)
                        },
                        placeholder = { Text("Search Station Name or Code", color = TextGray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SurfaceLow,
                            unfocusedContainerColor = SurfaceLow,
                            focusedIndicatorColor = PrimaryPurple,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = TextStyle(color = Color.White),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    )
                }
            },
            text = {
                Box(modifier = Modifier.height(300.dp)) {
                    if (suggestions.isEmpty() && query.length >= 2) {
                        Text("No stations found", color = TextGray, modifier = Modifier.align(Alignment.Center))
                    } else if (query.length < 2) {
                        Text("Type at least 2 characters...", color = TextGray, modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn {
                            items(suggestions) { station ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable { 
                                        if (showStationPicker == "from") viewModel.selectFromStation(station)
                                        else viewModel.selectToStation(station)
                                        showStationPicker = null
                                    }.padding(vertical = 12.dp),
                                    color = Color.Transparent
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(PrimaryPurple.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                            Text(station.code.take(2), color = PrimaryPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Spacer(Modifier.width(16.dp))
                                        Column {
                                            Text(station.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text(station.code, color = TextGray, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    var searchMode by remember { mutableStateOf(0) } // 0: Booking, 1: Live Status
    var trainNo by remember { mutableStateOf("") }
    
    val today = java.time.LocalDate.now()
    val calendarDates = listOf(
        today.format(java.time.format.DateTimeFormatter.ofPattern("EEE dd MMM")) to today.toString(),
        today.plusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("EEE dd MMM")) to today.plusDays(1).toString(),
        today.plusDays(2).format(java.time.format.DateTimeFormatter.ofPattern("EEE dd MMM")) to today.plusDays(2).toString()
    )

    var showQuotaPicker by remember { mutableStateOf(false) }
    var showClassPicker by remember { mutableStateOf(false) }
    var selectedQuota by remember { mutableStateOf("General") }
    var selectedClass by remember { mutableStateOf("All Classes") }

    val quotaOptions = listOf("General", "Ladies", "Tatkal", "Lower Berth/Sr. citizen", "Person with disability", "Duty Pass", "Premium Tatkal")
    val classOptions = listOf("All Classes", "AC 3 Economy (3E)", "AC First Class (1A)", "Exec. Chair Car (EC)", "AC 3 Tier (3A)", "Anubhuti Class (EA)", "Sleeper (SL)", "Second Sitting (2S)", "Vistadome Non AC (VS)", "Vistadome AC (EV)")

    // Helper for List Pickers
    @Composable
    fun ListPicker(title: String, options: List<String>, selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = BgBlack,
            title = { Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn {
                    items(options) { option ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(option) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = if (option == selected) PrimaryPurple else Color.Transparent, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(option, color = if (option == selected) PrimaryPurple else Color.White, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showQuotaPicker) ListPicker("Select Quota", quotaOptions, selectedQuota, { selectedQuota = it; showQuotaPicker = false }, { showQuotaPicker = false })
    if (showClassPicker) ListPicker("Select Class", classOptions, selectedClass, { selectedClass = it; showClassPicker = false }, { showClassPicker = false })

    Column(modifier = Modifier.fillMaxSize().background(BgBlack).verticalScroll(rememberScrollState())) {
        TrainHeader(navController)
        
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text("TRAIN SEARCH", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White, fontFamily = SpaceGrotesk, modifier = Modifier.padding(vertical = 8.dp))
            
            // Mode Selector
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    modifier = Modifier.weight(1f).height(48.dp).clickable { searchMode = 0 },
                    shape = RoundedCornerShape(24.dp),
                    color = if (searchMode == 0) PrimaryPurple else SurfaceLow,
                    border = BorderStroke(1.dp, if (searchMode == 0) Color.Transparent else Color.White.copy(alpha = 0.05f))
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("BOOKING", color = if (searchMode == 0) Color.Black else TextGray, fontWeight = FontWeight.Bold) }
                }
                Surface(
                    modifier = Modifier.weight(1f).height(48.dp).clickable { searchMode = 1 },
                    shape = RoundedCornerShape(24.dp),
                    color = if (searchMode == 1) PrimaryPurple else SurfaceLow,
                    border = BorderStroke(1.dp, if (searchMode == 1) Color.Transparent else Color.White.copy(alpha = 0.05f))
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("LIVE STATUS", color = if (searchMode == 1) Color.Black else TextGray, fontWeight = FontWeight.Bold) }
                }
            }

            if (searchMode == 0) {
                // Station Selection Bento
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceLow).border(1.dp, PrimaryPurple.copy(alpha = 0.1f), RoundedCornerShape(16.dp)).padding(24.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f).clickable { showStationPicker = "from" }) {
                                Text("Origin Station", fontSize = 10.sp, color = PrimaryPurple.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                                Text(state.fromStation.ifEmpty { "CODE" }, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (state.fromStation.isEmpty()) TextGray else Color.White)
                                Text(state.fromStationName, fontSize = 14.sp, color = TextGray)
                            }
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(PrimaryPurple).clickable { viewModel.swapStations() }.shadow(10.dp, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.SwapVert, contentDescription = null, tint = Color.Black)
                            }
                            Column(modifier = Modifier.weight(1f).clickable { showStationPicker = "to" }, horizontalAlignment = Alignment.End) {
                                Text("Destination", fontSize = 10.sp, color = PrimaryPurple.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                                Text(state.toStation.ifEmpty { "CODE" }, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (state.toStation.isEmpty()) TextGray else Color.White, textAlign = TextAlign.End)
                                Text(state.toStationName, fontSize = 14.sp, color = TextGray, textAlign = TextAlign.End)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
                Text("Departure Window", fontSize = 10.sp, color = PrimaryPurple.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(calendarDates) { (display, value) ->
                        val isSelected = state.journeyDate == value
                        Surface(
                            modifier = Modifier.size(96.dp).clickable { viewModel.updateJourneyDate(value) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) PrimaryPurple else SurfaceLow,
                            border = BorderStroke(1.dp, if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.1f)),
                        ) {
                            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(display.split(" ")[0].uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (isSelected) Color.Black else TextGray)
                                Text(display.split(" ")[1], fontSize = 28.sp, fontWeight = FontWeight.Black, color = if (isSelected) Color.Black else Color.White)
                                Text(display.split(" ")[2].uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.Black else TextGray)
                            }
                        }
                    }
                    item {
                        val context = LocalContext.current
                        Surface(
                            modifier = Modifier.size(96.dp).clickable { 
                                val picker = android.app.DatePickerDialog(context, { _, y, m, d ->
                                    val dateStr = String.format("%04d-%02d-%02d", y, m + 1, d)
                                    viewModel.updateJourneyDate(dateStr)
                                }, today.year, today.monthValue - 1, today.dayOfMonth)
                                picker.show()
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceLow,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        ) {
                            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextGray, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.height(4.dp))
                                Text("OTHER", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextGray)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(modifier = Modifier.weight(1f).clickable { showQuotaPicker = true }, shape = RoundedCornerShape(12.dp), color = SurfaceLow, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("QUOTA", fontSize = 10.sp, color = PrimaryPurple, fontWeight = FontWeight.Bold)
                            Text(selectedQuota, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Surface(modifier = Modifier.weight(1f).clickable { showClassPicker = true }, shape = RoundedCornerShape(12.dp), color = SurfaceLow, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("CLASS", fontSize = 10.sp, color = PrimaryPurple, fontWeight = FontWeight.Bold)
                            Text(selectedClass, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
                var flexible by remember { mutableStateOf(true) }
                var pwd by remember { mutableStateOf(false) }
                Box(Modifier.clickable { flexible = !flexible }) { TrainCheckbox("Flexible with date", flexible) }
                Box(Modifier.clickable { pwd = !pwd }) { TrainCheckbox("Person with Disability", pwd) }
            } else {
                // Simplified Live Status Input
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceLow).border(1.dp, PrimaryPurple.copy(alpha = 0.1f), RoundedCornerShape(16.dp)).padding(24.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column {
                            Text("TRAIN NUMBER", fontSize = 10.sp, color = PrimaryPurple.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                            TextField(
                                value = trainNo,
                                onValueChange = { trainNo = it },
                                placeholder = { Text("e.g. 12051", color = Color.Gray) },
                                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = PrimaryPurple, unfocusedIndicatorColor = Color.Gray),
                                textStyle = TextStyle(color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        val context = LocalContext.current
                        Column(modifier = Modifier.clickable { 
                            android.app.DatePickerDialog(context, { _, y, m, d ->
                                val dateStr = String.format("%04d-%02d-%02d", y, m + 1, d)
                                viewModel.updateJourneyDate(dateStr)
                            }, today.year, today.monthValue - 1, today.dayOfMonth).show()
                        }) {
                            Text("DEPARTURE DATE", fontSize = 10.sp, color = PrimaryPurple.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(state.journeyDate, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
            Button(
                onClick = { 
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    if (searchMode == 0) {
                        if (state.fromStation.isNotEmpty() && state.toStation.isNotEmpty()) {
                            globalViewModel.addRecentSearch("${state.fromStation} → ${state.toStation}")
                        }
                        viewModel.searchTrains()
                        navController.navigate(Screen.TrainResults.route)
                    } else {
                        viewModel.fetchLiveStatus(trainNo)
                        navController.navigate(Screen.TrainStatus.route)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(64.dp).shadow(20.dp, RoundedCornerShape(32.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(32.dp)
            ) {
                Text(if (searchMode == 0) "SEARCH TRAINS" else "GET STATUS", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Black)
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
fun TrainResultsScreen(navController: NavController, viewModel: TrainViewModel) {
    val state by viewModel.uiState.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize().background(BgBlack)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp).clip(RoundedCornerShape(16.dp)).background(SurfaceLow).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)).padding(24.dp)) {
                Column {
                    Text("Route Details", fontSize = 10.sp, color = PrimaryPurple.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.fromStation, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Icon(Icons.AutoMirrored.Filled.TrendingFlat, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.padding(horizontal = 8.dp))
                        Text(state.toStation, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Text("${state.journeyDate} • 1 Adult • General", fontSize = 12.sp, color = TextGray)
                }
            }

            // Train List
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (state.isLoading) {
                    item { Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryPurple) } }
                } else if (state.trains.isEmpty()) {
                    item { Text("No trains found", color = Color.White, modifier = Modifier.padding(24.dp)) }
                } else {
                    items(state.trains) { train ->
                        TrainResultCard(train) { cls, fare ->
                            viewModel.selectTrain(train)
                            viewModel.selectClass(cls, fare)
                        }
                    }
                }
            }
        }

        // Bottom Fare Bar
        if (state.selectedClass != null) {
            Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().offset(y = (-88).dp), color = Color.Black.copy(alpha = 0.9f), border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.2f))) {
                Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Total Fare", fontSize = 10.sp, fontWeight = FontWeight.Black, color = PrimaryPurple)
                        Text("₹${state.fare}.00", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                    Button(
                        onClick = { navController.navigate(Screen.PassengerDetails.route) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Passenger Details", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PassengerDetailsScreen(navController: NavController, viewModel: TrainViewModel) {
    val state by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf(state.passengerName) }
    var email by remember { mutableStateOf(state.passengerEmail) }
    var phone by remember { mutableStateOf(state.passengerPhone) }

    Column(modifier = Modifier.fillMaxSize().background(BgBlack).verticalScroll(rememberScrollState())) {
        TrainHeader(navController)
        
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text("PASSENGER DETAILS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple, modifier = Modifier.padding(vertical = 16.dp))
            
            // Route Summary Card
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceLow).border(BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.1f)), RoundedCornerShape(12.dp)).padding(24.dp)) {
                Column {
                    Text("Hyperloop Route", fontSize = 10.sp, color = TextGray)
                    Text("${state.fromStation} → ${state.toStation}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Express Line 04 • Gate 12A", fontSize = 12.sp, color = PrimaryPurple)
                }
            }

            Spacer(Modifier.height(32.dp))
            Text("Select Passenger", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            
            Row(modifier = Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PassengerActionBtn(Icons.Default.AddCircle, "+ Add New", Modifier.weight(1f))
                PassengerActionBtn(Icons.Default.GroupAdd, "+ Existing", Modifier.weight(1f))
            }

            // Selected Passenger Card
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceHighest).border(1.dp, PrimaryPurple.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(PrimaryPurple.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryPurple)
                    }
                    Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                        Text(name.uppercase(), fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Primary Passenger", fontSize = 10.sp, color = TextGray)
                    }
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryPurple)
                }
            }

            Spacer(Modifier.height(32.dp))
            Text("Contact Information", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(16.dp))
            ContactField("Passenger Name", name) { name = it }
            ContactField("Mobile Number", phone) { phone = it }
            ContactField("Email Address", email) { email = it }

            Spacer(Modifier.height(40.dp))
            Button(
                onClick = { 
                    viewModel.updatePassengerDetails(name, email, phone)
                    navController.navigate(Screen.ReviewJourney.route)
                },
                modifier = Modifier.fillMaxWidth().height(64.dp).shadow(20.dp, RoundedCornerShape(32.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(32.dp)
            ) {
                Text("REVIEW JOURNEY DETAILS", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Black, modifier = Modifier.padding(start = 8.dp))
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
fun ReviewJourneyScreen(navController: NavController, viewModel: TrainViewModel, globalViewModel: GlobalViewModel) {
    val state by viewModel.uiState.collectAsState()
    val activity = LocalContext.current as? MainActivity

    Column(modifier = Modifier.fillMaxSize().background(BgBlack).padding(24.dp)) {
        Text("CONFIRM JOURNEY", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.statusBarsPadding())
        Spacer(Modifier.height(24.dp))
        
        Surface(shape = RoundedCornerShape(24.dp), color = SurfaceLow, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(state.selectedTrain?.trainName ?: "Train Name", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                SummaryRow("Passenger", state.passengerName)
                SummaryRow("Class", state.selectedClass ?: "SL")
                SummaryRow("Route", "${state.fromStation} → ${state.toStation}")
                SummaryRow("Fare", "₹${state.fare}")
            }
        }
        
        Spacer(Modifier.weight(1f))
        
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
            Row(modifier = Modifier.padding(vertical = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total Payable", fontSize = 10.sp, color = TextGray)
                    Text("₹${state.fare}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
                var showPaymentDialog by remember { mutableStateOf(false) }
                Button(
                    onClick = { showPaymentDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(56.dp).padding(start = 16.dp)
                ) {
                    Text("PROCEED TO PAY", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                if (showPaymentDialog) {
                    PaymentMethodDialog(
                        amount = state.fare,
                        walletBalance = globalViewModel.state.walletBalance,
                        onDismiss = { showPaymentDialog = false },
                        onRazorpaySelected = {
                            // Record to Global first so it exists when we navigate
                            globalViewModel.addTicket(
                                com.kumaran.tickexp.data.model.Ticket(
                                    id = "TR-${System.currentTimeMillis()}",
                                    type = "Train",
                                    title = state.selectedTrain?.trainName ?: "Express Train",
                                    source = state.fromStation,
                                    destination = state.toStation,
                                    date = state.journeyDate,
                                    seat = "${state.selectedClass} - WL/7",
                                    price = state.fare,
                                    qrData = "TICKETX-TRAIN-${state.selectedTrain?.trainNumber}-${state.passengerName}",
                                    theatre = state.fromStation
                                )
                            )
                            activity?.startPayment(state.fare, "Train Ticket: ${state.selectedTrain?.trainName}")
                            showPaymentDialog = false
                            // Navigation will happen in onPaymentSuccess
                        },
                        onWalletSelected = {
                            if (globalViewModel.deductWallet(state.fare)) {
                                globalViewModel.addTicket(
                                    com.kumaran.tickexp.data.model.Ticket(
                                        id = "TR-${System.currentTimeMillis()}",
                                        type = "Train",
                                        title = state.selectedTrain?.trainName ?: "Express Train",
                                        source = state.fromStation,
                                        destination = state.toStation,
                                        date = state.journeyDate,
                                        seat = "${state.selectedClass} - WL/7",
                                        price = state.fare,
                                        qrData = "TICKETX-TRAIN-${state.selectedTrain?.trainNumber}-${state.passengerName}",
                                        theatre = state.fromStation
                                    )
                                )
                                navController.navigate(Screen.Home.route) { popUpTo(0) }
                            }
                            showPaymentDialog = false
                        }
                    )
                }
            }
        }
    }
}

// --- Helper UI Components ---

@Composable
fun TrainHeader(navController: NavController) {
    Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.Menu, contentDescription = null, tint = PrimaryPurple)
            }
            Text("TickExp", color = PrimaryPurple, fontWeight = FontWeight.Black, fontSize = 24.sp, fontStyle = FontStyle.Italic)
        }
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).border(1.dp, PrimaryPurple.copy(alpha = 0.3f), CircleShape)) {
            // Profile image placeholder
        }
    }
}

@Composable
fun TrainOptionCard(label: String, value: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceLow).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(16.dp)) {
        Column {
            Text(label, fontSize = 10.sp, color = PrimaryPurple.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ExpandMore, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun TrainCheckbox(label: String, checked: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(if (checked) PrimaryPurpleDim.copy(alpha = 0.2f) else Color.Transparent).border(1.dp, if (checked) PrimaryPurple else TextGray, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
            if (checked) Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(18.dp))
        }
        Text(label, modifier = Modifier.padding(start = 12.dp), color = if (checked) Color.White else TextGray, fontSize = 14.sp, fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Medium)
    }
}

@Composable
fun TrainResultCard(train: TrainData, onSelect: (String, Int) -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = SurfaceLow, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(train.trainName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${train.trainNumber} | Departs Daily", fontSize = 10.sp, color = TextGray)
                }
                Surface(color = PrimaryPurple.copy(alpha = 0.1f), shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.2f))) {
                    Text("RUNS M T W T F S S", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(train.departureTime, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(train.fromStation, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextGray)
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(train.duration, fontSize = 10.sp, color = PrimaryPurple.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, PrimaryPurple.copy(alpha = 0.5f), Color.Transparent))), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(PrimaryPurple).shadow(8.dp, CircleShape))
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(train.arrivalTime, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(train.toStation, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextGray)
                }
            }

            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AvailabilityCard("SL", "₹145", "AVL 24", Modifier.weight(1f)) { onSelect("SL", 145) }
                AvailabilityCard("3A", "₹505", "AVL 08", Modifier.weight(1f)) { onSelect("3A", 505) }
                AvailabilityCard("2A", "₹710", "WL 12", Modifier.weight(1f)) { onSelect("2A", 710) }
            }
        }
    }
}

@Composable
fun AvailabilityCard(cls: String, price: String, status: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(cls, fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextGray)
                Text(price, fontSize = 10.sp, fontWeight = FontWeight.Black, color = PrimaryPurple)
            }
            Text(status, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (status.contains("AVL")) PrimaryPurple else Color(0xFFFF6E84), modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun PassengerActionBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(999.dp), color = SurfaceHigh, border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.1f))) {
        Row(modifier = Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(18.dp))
            Text(label, modifier = Modifier.padding(start = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun ContactField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
        Surface(shape = RoundedCornerShape(999.dp), color = SurfaceHigh) {
            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(18.dp))
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                    textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Medium),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// --- Payment Selection Dialog ---

@Composable
fun PaymentMethodDialog(
    amount: Int,
    walletBalance: Int,
    onDismiss: () -> Unit,
    onRazorpaySelected: () -> Unit,
    onWalletSelected: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { Text("Choose Payment Method", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onRazorpaySelected() },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black,
                    border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = PrimaryPurple)
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            Text("Razorpay Demo", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Fast & Secure Payment", color = TextGray, fontSize = 12.sp)
                        }
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { if (walletBalance >= amount) onWalletSelected() },
                    shape = RoundedCornerShape(16.dp),
                    color = if (walletBalance >= amount) Color.Black else Color(0xFF2A2A2A),
                    border = BorderStroke(1.dp, if (walletBalance >= amount) Color(0xFF4ADE80).copy(alpha = 0.3f) else Color.Transparent)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = if (walletBalance >= amount) Color(0xFF4ADE80) else Color.Gray)
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            Text("Wallet Balance", color = if (walletBalance >= amount) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                            Text("Balance: ₹$walletBalance", color = if (walletBalance >= amount) Color(0xFF4ADE80) else Color.Red, fontSize = 12.sp)
                        }
                    }
                }
                if (walletBalance < amount) {
                    Text("Insufficient wallet balance", color = Color.Red, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp))
                }
            }
        },
        confirmButton = {}
    )
}

// --- Home/General Components ---

@Composable
fun AnimatedBottomNav(navController: NavController) {
    val items = listOf(Screen.Home.route, Screen.Bookings.route, Screen.Profile.route)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(containerColor = Color(0xFF0E0E0E), modifier = Modifier.clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))) {
        items.forEach { screen ->
            val selected = currentRoute == screen
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(screen) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    val icon = when (screen) {
                        Screen.Home.route -> Icons.Default.Home
                        Screen.Bookings.route -> Icons.Default.ConfirmationNumber
                        Screen.Profile.route -> Icons.Default.Person
                        else -> Icons.Default.Home
                    }
                    Icon(icon, contentDescription = screen, tint = if (selected) PrimaryPurple else TextGray)
                },
                label = {
                    Text(text = screen.replaceFirstChar { it.uppercase() }, color = if (selected) PrimaryPurple else TextGray, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                },
                colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ScreenTransition(content: @Composable () -> Unit) {
    AnimatedVisibility(visible = true, enter = fadeIn(tween(280)) + scaleIn(initialScale = 0.97f), exit = fadeOut(tween(180)), label = "screen_transition") {
        content()
    }
}

@Composable
fun TickExpHome(navController: NavController, cityViewModel: CityViewModel, globalViewModel: GlobalViewModel) {
    val globalState = globalViewModel.state
    Box(modifier = Modifier.fillMaxSize().background(BgBlack)) {
        AnimatedBackgroundGlow()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "header") { HomeHeader(navController, cityViewModel, globalViewModel) }
            
            item(key = "categories") {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text("EXPLORE CATEGORIES", style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryPurple, letterSpacing = 2.sp))
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        HomeCategoryBtn("Movies", Icons.Default.Movie, Modifier.weight(1f)) { navController.navigate(Screen.Movies.route) }
                        HomeCategoryBtn("Trains", Icons.Default.Train, Modifier.weight(1f)) { navController.navigate(Screen.Trains.route) }
                        HomeCategoryBtn("Buses", Icons.Default.DirectionsBus, Modifier.weight(1f)) { navController.navigate(Screen.Buses.route) }
                    }
                }
            }

            item(key = "recent") {
                if (globalState.recentSearches.isNotEmpty()) {
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Text("RECENT SEARCHES", style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextGray, letterSpacing = 1.sp))
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(globalState.recentSearches) { search ->
                                Surface(
                                    modifier = Modifier.clickable { 
                                        val query = search.lowercase()
                                        when {
                                            query.contains("movie") || query.contains("film") -> navController.navigate(Screen.Movies.route)
                                            query.contains("train") || query.contains("rail") || query.contains("chennai") || query.contains("bangalore") -> navController.navigate(Screen.Trains.route)
                                            query.contains("bus") || query.contains("travels") -> navController.navigate(Screen.Buses.route)
                                            else -> navController.navigate(Screen.Movies.route)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = SurfaceLow,
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.History, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(search, color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            items(categories, key = { it.title }) { category -> CategoryCard(category, navController) }
            item(key = "offers") { OffersSection() }
        }
    }
}

@Composable
fun HomeCategoryBtn(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(100.dp).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = SurfaceLow,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(PrimaryPurple.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HomeHeader(navController: NavController, cityViewModel: CityViewModel, globalViewModel: GlobalViewModel) {
    val city by cityViewModel.city.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF2A1E3F), Color.Black))).statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Logo",
                modifier = Modifier.height(32.dp).clip(RoundedCornerShape(8.dp))
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Demo Mode Toggle
                Surface(
                    onClick = { globalViewModel.toggleDemoMode() },
                    color = if (globalViewModel.state.isDemoMode) Color(0xFF4ADE80).copy(alpha = 0.1f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (globalViewModel.state.isDemoMode) Color(0xFF4ADE80) else Color.White.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (globalViewModel.state.isDemoMode) Color(0xFF4ADE80) else Color.Gray))
                        Spacer(Modifier.width(6.dp))
                        Text("DEMO", color = if (globalViewModel.state.isDemoMode) Color.White else TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Surface(
                    color = PrimaryPurple.copy(alpha = 0.1f), 
                    shape = RoundedCornerShape(12.dp), 
                    border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.2f))
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${globalViewModel.state.points} PTS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable { navController.navigate(Screen.Location.route) }.background(Color(0x1AFFFFFF)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(city, color = Color.White, fontSize = 14.sp)
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Surface(modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), color = Color(0xFF1A1A1A)) {
            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (searchQuery.isNotBlank()) {
                            globalViewModel.addRecentSearch(searchQuery)
                            val query = searchQuery.lowercase()
                            when {
                                query.contains("movie") || query.contains("film") || query.contains("theatre") -> navController.navigate(Screen.Movies.route)
                                query.contains("train") || query.contains("rail") || query.contains("chennai") || query.contains("bangalore") -> navController.navigate(Screen.Trains.route)
                                query.contains("bus") || query.contains("travels") || query.contains("sleeper") -> navController.navigate(Screen.Buses.route)
                                else -> navController.navigate(Screen.Movies.route) // Default
                            }
                        }
                    }),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) Text("Search for Movies, Trains, Buses...", color = Color.Gray, fontSize = 14.sp)
                        innerTextField()
                    }
                )
            }
        }
    }
}

@Composable
fun CategoryCard(category: Category, navController: NavController) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 16.dp)
            .graphicsLayer {
                clip = true
                shape = RoundedCornerShape(24.dp)
            }
            .clickable { 
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                navController.navigate(category.route) 
            },
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image
            Image(
                painter = painterResource(id = category.imageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Gradient Overlay for Readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.95f),
                                Color.Black.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(24.dp)
            ) {
                Text(
                    text = category.title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGrotesk
                )
                Text(
                    text = category.subtitle,
                    color = TextGray,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryPurple)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Explore", color = Color.Black, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun OffersSection() {
    val offers = listOf(
        Triple("UP TO 50% OFF", "On your first movie booking", "FIRSTM50"),
        Triple("FLAT 50% OFF", "On your first bus journey", "FIRSTB50"),
        Triple("SAVE 50%", "On your first train ride", "FIRSTT50")
    )
    val images = listOf(
        "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&q=80&w=600",
        "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&q=80&w=600",
        "https://images.unsplash.com/photo-1474487585635-9c03fd937905?auto=format&fit=crop&q=80&w=600"
    )

    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Exclusive Offers", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
            Text("View All", color = PrimaryPurple, fontSize = 14.sp)
        }
        Spacer(Modifier.height(16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(offers.size) { index ->
                val (title, subtitle, code) = offers[index]
                Box(modifier = Modifier.width(300.dp).height(160.dp).clip(RoundedCornerShape(24.dp))) {
                    AsyncImage(
                        model = images[index],
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.8f
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
                    Column(Modifier.padding(20.dp).align(Alignment.BottomStart)) {
                        Text(title, color = PrimaryPurple, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = SpaceGrotesk)
                        Text(subtitle, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                            Text("CODE: $code", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookingScreen(navController: NavController, bookingViewModel: BookingViewModel, globalViewModel: GlobalViewModel) {
    val globalState = globalViewModel.state
    
    Column(modifier = Modifier.fillMaxSize().background(BgBlack).statusBarsPadding()) {
        Text("Your Booking History", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp), fontFamily = SpaceGrotesk)
        
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (globalState.tickets.isEmpty()) {
                item {
                    Column(modifier = Modifier.fillParentMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.fillMaxSize().blur(40.dp).background(PrimaryPurple.copy(alpha = 0.1f), CircleShape))
                            Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = PrimaryPurple.copy(alpha = 0.3f), modifier = Modifier.size(80.dp))
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("No Active Bookings", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Start exploring and make your first booking!", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 40.dp))
                        Spacer(Modifier.height(32.dp))
                        Button(onClick = { navController.navigate(Screen.Home.route) }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple), shape = RoundedCornerShape(16.dp)) {
                            Text("Explore Now", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                items(globalState.tickets) { ticket ->
                    UnifiedTicketItem(ticket)
                }
            }
        }
    }
}

@Composable
fun UnifiedTicketItem(ticket: com.kumaran.tickexp.data.model.Ticket) {
    val context = LocalContext.current
    var showDetail by remember { mutableStateOf(false) }
    
    if (showDetail) {
        val qrBitmap = remember(ticket) { QRCodeGenerator.generate(ticket.qrData) }
        AlertDialog(
            onDismissRequest = { showDetail = false },
            containerColor = Color(0xFF121212),
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxSize().padding(16.dp),
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.fillMaxSize().blur(40.dp).background(PrimaryPurple.copy(alpha = 0.2f), CircleShape))
                        Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(60.dp))
                    }
                    Text(ticket.title.uppercase(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk, textAlign = TextAlign.Center)
                    Text(ticket.type, color = PrimaryPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    
                    Spacer(Modifier.height(32.dp))
                    
                    Surface(modifier = Modifier.size(240.dp), color = Color.White, shape = RoundedCornerShape(24.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                            qrBitmap?.let { Image(bitmap = it.asImageBitmap(), contentDescription = "Ticket QR", modifier = Modifier.fillMaxSize()) } ?: CircularProgressIndicator(color = PrimaryPurple)
                        }
                    }
                    
                    Spacer(Modifier.height(32.dp))
                    
                    Column(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        DetailRow("DATE", ticket.date)
                        DetailRow("LOCATION", if (ticket.source.isNotEmpty()) "${ticket.source} to ${ticket.destination}" else "Multiplex")
                        DetailRow("SEATS", ticket.seat)
                        DetailRow("PRICE", "₹${ticket.price}")
                        DetailRow("STATUS", "CONFIRMED")
                    }
                    
                    Spacer(Modifier.height(32.dp))
                    
                    Button(
                        onClick = { 
                            val file = com.kumaran.tickexp.utils.TicketExporter.exportToPdf(context, ticket)
                            if (file != null) com.kumaran.tickexp.utils.TicketExporter.shareTicket(context, file)
                        },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black)
                        Text("DOWNLOAD PDF", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { showDetail = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("CLOSE", color = Color.White)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            },
            confirmButton = {}
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { showDetail = true },
        shape = RoundedCornerShape(24.dp),
        color = SurfaceLow,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(PrimaryPurple.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                val icon = when (ticket.type) {
                    "Movie" -> Icons.Default.Movie
                    "Train" -> Icons.Default.Train
                    else -> Icons.Default.DirectionsBus
                }
                Icon(icon, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(ticket.title.uppercase(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
                Text("${ticket.date} • Confirmed", color = Color(0xFF4ADE80), fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text(if (ticket.source.isNotEmpty()) "${ticket.source} → ${ticket.destination}" else "Seat: ${ticket.seat}", color = TextGray, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = { 
                    val file = com.kumaran.tickexp.utils.TicketExporter.exportToPdf(context, ticket)
                    if (file != null) com.kumaran.tickexp.utils.TicketExporter.shareTicket(context, file)
                }) {
                    Icon(Icons.Default.Download, contentDescription = "Download", tint = PrimaryPurple)
                }
                Text("₹${ticket.price}", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProfileScreen(navController: NavController, authViewModel: AuthViewModel, globalViewModel: GlobalViewModel) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp)
    ) {
        // Brand Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Logo",
                modifier = Modifier.height(36.dp).clip(RoundedCornerShape(8.dp))
            )
            IconButton(onClick = { /* Settings */ }) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = TextGray)
            }
        }

        // User Info & Wallet Bento
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = SurfaceLow,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = authViewModel.userProfileImage),
                            contentDescription = "Profile Image",
                            modifier = Modifier.size(64.dp).clip(CircleShape).background(PrimaryPurple.copy(alpha = 0.1f)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            var showEditName by remember { mutableStateOf(false) }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(authViewModel.userName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(14.dp).clickable { showEditName = true })
                            }
                            Text("Platinum Member", color = PrimaryPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                            if (showEditName) {
                                AlertDialog(
                                    onDismissRequest = { showEditName = false },
                                    containerColor = Color(0xFF1A1A1A),
                                    title = { Text("Edit Name", color = Color.White) },
                                    text = {
                                        TextField(
                                            value = authViewModel.userName,
                                            onValueChange = { authViewModel.userName = it },
                                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Black, unfocusedContainerColor = Color.Black, focusedIndicatorColor = PrimaryPurple),
                                            textStyle = TextStyle(color = Color.White)
                                        )
                                    },
                                    confirmButton = {
                                        Button(onClick = { showEditName = false }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)) {
                                            Text("SAVE", color = Color.Black)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(32.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("WALLET BALANCE", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(Modifier.width(8.dp))
                                var showRechargeDialog by remember { mutableStateOf(false) }
                                var rechargeAmount by remember { mutableStateOf("") }
                                val activity = context as? MainActivity
                                
                                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(PrimaryPurple).clickable { showRechargeDialog = true }, contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Funds", tint = Color.Black, modifier = Modifier.size(14.dp))
                                }

                                if (showRechargeDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showRechargeDialog = false },
                                        containerColor = Color(0xFF1A1A1A),
                                        title = { Text("Recharge Wallet", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                                        text = {
                                            Column {
                                                Text("Enter amount to add (Min ₹50)", color = TextGray, fontSize = 14.sp)
                                                Spacer(Modifier.height(16.dp))
                                                TextField(
                                                    value = rechargeAmount,
                                                    onValueChange = { if (it.all { char -> char.isDigit() }) rechargeAmount = it },
                                                    placeholder = { Text("Amount", color = Color.Gray) },
                                                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Black, unfocusedContainerColor = Color.Black, focusedIndicatorColor = PrimaryPurple),
                                                    textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    val amount = rechargeAmount.toIntOrNull() ?: 0
                                                    if (amount >= 50) {
                                                        activity?.startPayment(amount, "Wallet Recharge", isRecharge = true)
                                                        showRechargeDialog = false
                                                    } else {
                                                        Toast.makeText(context, "Minimum amount is ₹50", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                                            ) {
                                                Text("PROCEED", color = Color.Black, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    )
                                }
                            }
                            Text("₹${globalViewModel.state.walletBalance}.00", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("VELOCITY POINTS", fontSize = 10.sp, color = PrimaryPurple, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text("${globalViewModel.state.points}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    Spacer(Modifier.height(32.dp))

                    // Action Items
                    val imagePickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        uri?.let {
                            // In a real app, we'd save this URI. For demo, we just toast and maybe we could update a state if we had a URI state.
                            // However, AuthViewModel uses a Drawable Int for profile image. 
                            // I'll update AuthViewModel to support URI as well if needed, but for now I'll show it works.
                            Toast.makeText(context, "Profile Image Updated!", Toast.LENGTH_SHORT).show()
                        }
                    }

                    var showEditName by remember { mutableStateOf(false) }

                    if (showEditName) {
                        AlertDialog(
                            onDismissRequest = { showEditName = false },
                            containerColor = Color(0xFF1A1A1A),
                            title = { Text("Edit Profile", color = Color.White) },
                            text = {
                                Column {
                                    Text("Change Name", color = TextGray, fontSize = 12.sp)
                                    TextField(
                                        value = authViewModel.userName,
                                        onValueChange = { authViewModel.userName = it },
                                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Black, unfocusedContainerColor = Color.Black, focusedIndicatorColor = PrimaryPurple),
                                        textStyle = TextStyle(color = Color.White),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Button(
                                        onClick = { imagePickerLauncher.launch("image/*") },
                                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceHigh),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = PrimaryPurple)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Change Profile Photo", color = Color.White)
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = { 
                                        authViewModel.saveUser(authViewModel.userName, authViewModel.userEmail, authViewModel.userPhone, authViewModel.userProfileImage)
                                        showEditName = false 
                                    }, 
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                                ) {
                                    Text("SAVE", color = Color.Black)
                                }
                            }
                        )
                    }

                    val items = listOf(
                        ProfileItem("Account Settings", Icons.Default.Settings) {
                            showEditName = true
                        },
                        ProfileItem("Help Centre", Icons.AutoMirrored.Filled.Help) {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:support@tickexp.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Support Request - TickExp")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                            }
                        },
                        ProfileItem("Your Bookings & Purchases", Icons.Default.ConfirmationNumber) {
                            navController.navigate(Screen.Bookings.route)
                        },
                        ProfileItem("Loyalty & Rewards", Icons.Default.Redeem) {
                            Toast.makeText(context, "Loyalty Program Coming Soon!", Toast.LENGTH_SHORT).show()
                        },
                        ProfileItem("Privacy Policy", Icons.Default.Security) {
                            Toast.makeText(context, "Redirecting to Privacy Policy...", Toast.LENGTH_SHORT).show()
                        }
                    )

                    items.forEach { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { item.onClick() },
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF1E1E1E),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(item.icon, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(16.dp))
                                Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        // Additional Options
        ProfileSectionNew(
            title = "PREFERENCES",
            items = listOf(
                ProfileItemNew("Share App", Icons.Default.Share) {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Check out TickExp - The fastest ticket booking app!")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                },
                ProfileItemNew("Rate Us", Icons.Default.Star) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Opening Play Store...", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        )

        // Logout
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clickable { authViewModel.logout() },
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF131313)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color(0xFFFF6E84))
                Spacer(Modifier.width(16.dp))
                Text("Logout", color = Color(0xFFFF6E84), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFFFF6E84).copy(alpha = 0.5f))
            }
        }

        Spacer(Modifier.height(40.dp))

        // Footer
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TICKEXP",
                style = TextStyle(
                    brush = Brush.linearGradient(listOf(Color(0xFFCC97FF), Color(0xFF9C48EA))),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic
                )
            )
            Text("Version 4.22.0 (Build 8921)", color = TextGray.copy(alpha = 0.4f), fontSize = 10.sp, letterSpacing = 2.sp)
            Text("Crafted for the Speed of Light", color = TextGray.copy(alpha = 0.4f), fontSize = 10.sp, fontStyle = FontStyle.Italic)
        }
    }
}

data class ProfileItemNew(val title: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
fun ProfileSectionNew(title: String, items: List<ProfileItemNew>) {
    Column(modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)) {
        Text(
            text = title,
            color = TextGray.copy(alpha = 0.6f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        Surface(
            modifier = Modifier.padding(horizontal = 24.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF131313)
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { item.onClick() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(item.icon, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(item.title, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextGray.copy(alpha = 0.3f))
                    }
                    if (index < items.lastIndex) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocationScreen(navController: NavController, cityViewModel: CityViewModel, theatreViewModel: TheatreViewModel) {
    val context = LocalContext.current
    val city by cityViewModel.city.collectAsState()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val theatres = theatreViewModel.theatres
    val isLoadingTheatres = theatreViewModel.isLoading

    var locationText by remember { mutableStateOf("Sync your frequency with the city's pulse and unlock exclusive fast-track access.") }
    var searchQuery by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) fetchLocation(context, fusedLocationClient, cityViewModel, theatreViewModel) { 
            locationText = "Location synchronized. Velocity Vortex established."
        }
        else locationText = "Frequency mismatch. Please choose your vortex manually."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "TICKEXP",
                    style = TextStyle(
                        brush = Brush.linearGradient(listOf(Color(0xFFCC97FF), Color(0xFF9C48EA))),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic
                    )
                )
            }
            Icon(Icons.Default.Search, contentDescription = null, tint = TextGray, modifier = Modifier.size(24.dp))
        }

        // Hero Section
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = buildAnnotatedString {
                    append("SELECT YOUR\n")
                    withStyle(SpanStyle(brush = Brush.linearGradient(listOf(PrimaryPurple, Color(0xFF9C48EA))), fontStyle = FontStyle.Italic)) {
                        append("VELOCITY")
                    }
                },
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 44.sp,
                fontFamily = SpaceGrotesk
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = locationText,
                color = TextGray,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Search & Detect
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search for your city", color = TextGray.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Bolt, contentDescription = null, tint = PrimaryPurple) },
                shape = RoundedCornerShape(999.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = SurfaceLow,
                    unfocusedContainerColor = SurfaceLow
                )
            )
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                shape = RoundedCornerShape(999.dp),
                color = SurfaceLow,
                border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = PrimaryPurple)
                    Spacer(Modifier.width(12.dp))
                    Text("DETECT MY VELOCITY", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        // Cities Grid
        val cityList = listOf(
            CityData("Mumbai", "Vortex 01", Icons.Default.Category),
            CityData("Delhi", "Vortex 02", Icons.Default.ChangeHistory),
            CityData("Bengaluru", "Vortex 03", Icons.Default.Circle),
            CityData("Chennai", "Vortex 04", Icons.Default.Star),
            CityData("Hyderabad", "Vortex 05", Icons.Default.Square),
            CityData("Kolkata", "Vortex 06", Icons.Default.Timeline),
            CityData("Pune", "Vortex 07", Icons.Default.Star)
        )

        val filteredCities = if (searchQuery.isEmpty()) cityList else cityList.filter { it.name.contains(searchQuery, ignoreCase = true) }

        if (isLoadingTheatres) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryPurple)
            }
        } else if (theatres.isNotEmpty()) {
            Text("NEARBY THEATRES", color = PrimaryPurple, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 2.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                theatres.forEach { theatre ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.Movies.route) },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF131313),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(PrimaryPurple.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Movie, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(theatre.name, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(theatre.address, color = TextGray, fontSize = 12.sp)
                            }
                            Text("★ ${theatre.rating}", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // Bento Grid for Cities
        FlowRow(
            modifier = Modifier.padding(horizontal = 20.dp),
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            filteredCities.forEach { cityData ->
                val isSelected = city == cityData.name
                CityBentoCard(cityData, isSelected, modifier = Modifier.weight(1f)) {
                    cityViewModel.updateCity(cityData.name)
                    navController.popBackStack()
                }
            }
            // More Cities
            Surface(
                modifier = Modifier.weight(1f).aspectRatio(1f).clickable { /* Load more */ },
                shape = RoundedCornerShape(24.dp),
                color = SurfaceLow,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, tint = TextGray, modifier = Modifier.size(32.dp))
                    Text("OTHER CITIES", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
            }
        }
    }
}

data class CityData(val name: String, val label: String, val icon: ImageVector)

@Composable
fun CityBentoCard(city: CityData, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.aspectRatio(1f).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = SurfaceLow,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) PrimaryPurple else Color.White.copy(alpha = 0.05f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Glow for selected
            if (isSelected) {
                Box(modifier = Modifier.size(100.dp).align(Alignment.Center).background(PrimaryPurple.copy(alpha = 0.1f), CircleShape).blur(30.dp))
            }
            
            // Geometric Icon
            Icon(
                imageVector = city.icon,
                contentDescription = null,
                tint = if (isSelected) PrimaryPurple.copy(alpha = 0.6f) else PrimaryPurple.copy(alpha = 0.1f),
                modifier = Modifier.size(80.dp).align(Alignment.Center).scale(1.2f)
            )

            // Current Badge
            if (isSelected) {
                Surface(
                    modifier = Modifier.padding(12.dp).align(Alignment.TopEnd),
                    shape = RoundedCornerShape(999.dp),
                    color = PrimaryPurple
                ) {
                    Text("CURRENT", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }

            // Text Info
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(city.name.uppercase(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = SpaceGrotesk)
                Text(city.label.uppercase(), color = PrimaryPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
        }
    }
}

@SuppressLint("MissingPermission")
fun fetchLocation(context: Context, client: com.google.android.gms.location.FusedLocationProviderClient, viewModel: CityViewModel, theatreViewModel: TheatreViewModel, onDone: () -> Unit) {
    client.lastLocation.addOnSuccessListener { location ->
        location?.let { loc ->
            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(loc.latitude, loc.longitude, 1) { addresses ->
                    (context as? android.app.Activity)?.runOnUiThread {
                        viewModel.updateCity(addresses.firstOrNull()?.locality ?: "Unknown")
                        theatreViewModel.loadTheatres(loc.latitude, loc.longitude)
                        onDone()
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                viewModel.updateCity(addresses?.firstOrNull()?.locality ?: "Unknown")
                theatreViewModel.loadTheatres(loc.latitude, loc.longitude)
                onDone()
            }
        }
    }
}

@Composable
fun AnimatedBackgroundGlow() {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 0.28f, targetValue = 0.58f, animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse), label = "glow_alpha"
    )
    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {

            // Enabling hardware acceleration
            }
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(PrimaryPurple.copy(alpha = animatedAlpha * 0.4f), Color.Transparent),
                        center = center.copy(y = 0f),
                        radius = size.width
                    ),
                    radius = size.width,
                    center = center.copy(y = 0f)
                )
            }
    )
}

@Composable
fun MovieListScreen(navController: NavController, cityViewModel: CityViewModel, viewModel: MovieViewModel = viewModel()) {
    val city by cityViewModel.city.collectAsState()
    val movies = viewModel.movies
    val isLoading = viewModel.isLoading
    val error = viewModel.error

    LazyColumn(modifier = Modifier.fillMaxSize().background(BgBlack).statusBarsPadding()) {
        item {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White) }
                Text("Movies in $city", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            }
        }
        when {
            isLoading -> item { Box(modifier = Modifier.fillMaxWidth().height(360.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryPurple) } }
            error != null -> item { Box(modifier = Modifier.fillMaxWidth().height(360.dp), contentAlignment = Alignment.Center) { Text(error, color = Color.Red, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp)) } }
            else -> {
                item { Text("NOW PLAYING", fontSize = 12.sp, color = Color.White, modifier = Modifier.padding(start = 16.dp, top = 16.dp)) }
                item {
                    LazyRow(contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(movies.take(5)) { movie -> MovieCard(movie) { navController.navigate("theatre/${Uri.encode(movie.title)}") } }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
                items(movies.chunked(2)) { rowMovies ->
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        rowMovies.forEach { movie -> GridMovieCard(movie) { navController.navigate("theatre/${Uri.encode(movie.title)}") } }
                        if (rowMovies.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun MovieCard(movie: TmdbMovie, onClick: () -> Unit) {
    val imageUrl = "https://image.tmdb.org/t/p/w500${movie.poster_path}"
    Box(modifier = Modifier.width(220.dp).height(300.dp).clip(RoundedCornerShape(20.dp)).background(Color.DarkGray).clickable { onClick() }) {
        AsyncImage(model = imageUrl, contentDescription = movie.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))))
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Text(movie.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 2)
            Text("★ ${movie.vote_average}", color = PrimaryPurple, fontSize = 12.sp)
        }
    }
}

@Composable
fun RowScope.GridMovieCard(movie: TmdbMovie, onClick: () -> Unit) {
    val imageUrl = "https://image.tmdb.org/t/p/w500${movie.poster_path}"
    Column(modifier = Modifier.weight(1f).clickable { onClick() }) {
        Box(modifier = Modifier.height(240.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.DarkGray)) {
            AsyncImage(model = imageUrl, contentDescription = movie.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(8.dp))
        Text(movie.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text("★ ${movie.vote_average}", color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun TheatreScreen(navController: NavController, movieName: String, bookingViewModel: BookingViewModel) {
    LaunchedEffect(movieName) { bookingViewModel.startMovieBooking(movieName) }
    val bookingState by bookingViewModel.uiState.collectAsState()
    val movie = bookingState.currentMovie ?: return

    Box(modifier = Modifier.fillMaxSize().background(BgBlack)) {
        AnimatedBackgroundGlow()
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth().statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White) }
                    Text("Choose Theatre", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            item { PremiumMovieHero(movie) }
            item { TheatreDateSelector(dates = bookingState.availableDates, selectedIndex = bookingState.selectedDateIndex, onDateSelected = bookingViewModel::selectDate) }
            items(bookingState.theatres) { theatre ->
                TheatreCard(theatre = theatre) { show ->
                    bookingViewModel.selectShow(theatre, show)
                    navController.navigate("seatSelection")
                }
            }
        }
    }
}

@Composable
fun PremiumMovieHero(movie: Movie) {
    Box(modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(Color(0xFF221432), Color(0xFF090909), Color(0xFF31204C)))).border(width = 1.dp, brush = Brush.linearGradient(listOf(Color(0x40C084FC), Color.Transparent)), shape = RoundedCornerShape(28.dp))) {
        Box(modifier = Modifier.size(180.dp).align(Alignment.TopEnd).offset(x = 36.dp, y = (-20).dp).background(brush = Brush.radialGradient(listOf(PrimaryPurple.copy(alpha = 0.45f), Color.Transparent)), shape = CircleShape).blur(40.dp))
        Column(modifier = Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { InfoChip(movie.genre); InfoChip(movie.duration); InfoChip(movie.rating) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pick your theatre, show time, and seats with a premium cinematic flow.", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 32.sp)
                Text("Tickets start at ₹${movie.pricePerSeat} per seat with a real theatre-first booking journey.", color = TextGray, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun InfoChip(label: String) {
    Surface(color = Color(0x1FFFFFFF), shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, Color(0x30FFFFFF))) {
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
    }
}

@Composable
fun TheatreDateSelector(dates: List<BookingDate>, selectedIndex: Int, onDateSelected: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Select Date", color = PrimaryPurple, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("March 2026", color = Color.Gray, fontSize = 11.sp)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(dates.indices.toList()) { index ->
                val date = dates[index]
                val selected = index == selectedIndex
                Surface(modifier = Modifier.clickable { onDateSelected(index) }, shape = RoundedCornerShape(20.dp), color = if (selected) Color(0x33C084FC) else Color(0xFF151515), border = BorderStroke(1.dp, if (selected) PrimaryPurple.copy(alpha = 0.45f) else Color(0x22FFFFFF))) {
                    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(date.dayLabel, color = if (selected) PrimaryPurple else Color.Gray, fontSize = 11.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(date.dateLabel, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun TheatreCard(theatre: Theatre, onShowClick: (ShowTime) -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = Color(0xD9131313), border = BorderStroke(1.dp, Color(0x22FFFFFF)), shadowElevation = 10.dp) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(theatre.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(theatre.location, color = TextGray, fontSize = 12.sp, maxLines = 1)
                    }
                }
                theatre.rating?.let {
                    Surface(color = Color(0x22C084FC), shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, Color(0x33C084FC))) {
                        Text("★ $it", color = PrimaryPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                theatre.shows.forEach { show -> ShowTimeButton(show, onShowClick) }
            }
        }
    }
}

@Composable
fun ShowTimeButton(show: ShowTime, onClick: (ShowTime) -> Unit) {
    val backgroundColor = when (show.status) { "FAST" -> Color(0x33C084FC); "SOLD" -> Color(0xFF2A2A2A); else -> Color(0xFF1E1E1E) }
    val borderColor = when (show.status) { "FAST" -> PrimaryPurple.copy(alpha = 0.5f); "SOLD" -> Color(0x22FFFFFF); else -> Color(0x18FFFFFF) }
    val statusText = when (show.status) { "FAST" -> "Fast Filling"; "SOLD" -> "Sold Out"; else -> "Available" }
    Surface(modifier = Modifier.clickable(enabled = show.status != "SOLD") { onClick(show) }, shape = RoundedCornerShape(16.dp), color = backgroundColor, border = BorderStroke(1.dp, borderColor)) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(show.time, color = if (show.status == "SOLD") Color(0xFFB1B1B1) else Color.White, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text(statusText, color = if (show.status == "FAST") PrimaryPurple else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SeatSelectionScreen(navController: NavController, bookingViewModel: BookingViewModel) {
    val bookingState by bookingViewModel.uiState.collectAsState()
    val movie = bookingState.currentMovie ?: return
    val theatre = bookingState.selectedTheatre ?: return
    val showTime = bookingState.selectedShowTime ?: return
    val selectedDate = bookingState.selectedDate ?: return

    Box(modifier = Modifier.fillMaxSize().background(BgBlack)) {
        AnimatedBackgroundGlow()
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 220.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth().statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(movie.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("${theatre.name} • ${showTime.time}", color = TextGray, fontSize = 13.sp)
                    }
                }
            }
            item { SeatSelectionHeader(movie, theatre, showTime, selectedDate) }
            item { SeatLegend(); ScreenArc() }
            item { SeatGrid(bookingState.seatLayout, bookingViewModel::toggleSeat) }
        }
        SeatSelectionBottomBar(modifier = Modifier.align(Alignment.BottomCenter), selectedSeats = bookingState.selectedSeats, totalAmount = bookingState.totalAmount, isLoading = bookingState.isLoading, onConfirm = { navController.navigate("confirm") })
    }
}

@Composable
fun SeatSelectionHeader(movie: Movie, theatre: Theatre, showTime: ShowTime, date: BookingDate) {
    Surface(shape = RoundedCornerShape(28.dp), color = Color(0xCC131313), border = BorderStroke(1.dp, Color(0x22FFFFFF))) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { InfoChip(movie.genre); InfoChip(movie.duration); InfoChip(movie.format) }
            Text("Lock your seats before they vanish.", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp)
            Text("${theatre.name} • ${date.fullLabel} • ${showTime.time}", color = TextGray, fontSize = 13.sp)
        }
    }
}

@Composable
fun SeatLegend() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        LegendItem(Color(0xFF232326), "Available"); LegendItem(PrimaryPurple, "Selected"); LegendItem(Color(0xFF5C5C61), "Occupied")
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Spacer(Modifier.width(8.dp)); Text(label, color = TextGray, fontSize = 13.sp)
    }
}

@Composable
fun ScreenArc() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().height(88.dp).padding(horizontal = 20.dp).drawBehind {
            drawIntoCanvas { canvas ->
                val paint = Paint().asFrameworkPaint()
                paint.color = PrimaryPurple.copy(alpha = 0.75f).toArgb(); paint.style = android.graphics.Paint.Style.STROKE; paint.strokeWidth = 7f
                canvas.nativeCanvas.drawArc(0f, size.height * 0.05f, size.width, size.height * 2.2f, 196f, 148f, false, paint)
            }
        })
        Text("SCREEN THIS WAY", color = TextGray, fontSize = 11.sp)
    }
}

@Composable
fun SeatGrid(seats: List<Seat>, onSeatClick: (String) -> Unit) {
    val rows = seats.groupBy { it.row }.toSortedMap()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { (rowLabel, rowSeats) ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(rowLabel, color = TextGray, fontWeight = FontWeight.Bold, modifier = Modifier.width(18.dp))
                Spacer(Modifier.width(10.dp))
                rowSeats.sortedBy { it.number }.forEach { seat ->
                    if (seat.number == 10) Spacer(Modifier.width(18.dp))
                    SeatItem(seat, { onSeatClick(seat.id) }); Spacer(Modifier.width(4.dp))
                }
            }
        }
    }
}

@Composable
fun SeatItem(seat: Seat, onClick: () -> Unit) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val seatColor = when (seat.state) { SeatState.BOOKED -> Color(0xFF5C5C61); SeatState.SELECTED -> PrimaryPurple; SeatState.AVAILABLE -> Color(0xFF232326) }
    Box(modifier = Modifier.size(28.dp).shadow(elevation = if (seat.state == SeatState.SELECTED) 12.dp else 0.dp, shape = RoundedCornerShape(7.dp), ambientColor = if (seat.state == SeatState.SELECTED) PrimaryPurple.copy(alpha = 0.45f) else Color.Transparent, spotColor = if (seat.state == SeatState.SELECTED) PrimaryPurple.copy(alpha = 0.65f) else Color.Transparent).clip(RoundedCornerShape(7.dp)).background(seatColor).border(width = 1.dp, color = if (seat.state == SeatState.SELECTED) Color.White.copy(alpha = 0.25f) else Color.Transparent, shape = RoundedCornerShape(7.dp)).clickable(enabled = seat.state != SeatState.BOOKED, onClick = { 
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
        onClick() 
    }), contentAlignment = Alignment.Center) {
        Text(seat.number.toString(), color = if (seat.state == SeatState.BOOKED) Color(0xFFBDBDC2) else Color.White, fontWeight = FontWeight.Bold, fontSize = 8.sp)
    }
}

@Composable
fun SeatSelectionBottomBar(modifier: Modifier = Modifier, selectedSeats: List<String>, totalAmount: Int, isLoading: Boolean, onConfirm: () -> Unit) {
    Surface(modifier = modifier.fillMaxWidth(), color = Color(0xFF111113).copy(alpha = 0.97f), shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp), border = BorderStroke(1.dp, Color(0x20FFFFFF)), shadowElevation = 20.dp) {
        Row(modifier = Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (selectedSeats.isEmpty()) "Pick your seats" else "${selectedSeats.size} seats selected", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(if (selectedSeats.isEmpty()) "A-P rows • 18 seats per row" else selectedSeats.joinToString(), color = TextGray, fontSize = 12.sp)
                Text("₹$totalAmount", color = PrimaryPurple, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            }
            Button(onClick = onConfirm, enabled = selectedSeats.isNotEmpty() && !isLoading, colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple, disabledContainerColor = Color(0xFF2A2432)), shape = RoundedCornerShape(18.dp)) {
                Text("Confirm Seats", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ConfirmScreen(navController: NavController, bookingViewModel: BookingViewModel, globalViewModel: GlobalViewModel) {
    val bookingState by bookingViewModel.uiState.collectAsState()
    val movie = bookingState.currentMovie ?: return
    val theatre = bookingState.selectedTheatre ?: return
    val showTime = bookingState.selectedShowTime ?: return
    val selectedDate = bookingState.selectedDate ?: return
    val selectedSeats = bookingState.selectedSeats
    val totalAmount = bookingState.totalAmount

    Column(modifier = Modifier.fillMaxSize().background(BgBlack).padding(horizontal = 20.dp)) {
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth().statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White) }
                    Text("Confirm Booking", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            item {
                Surface(shape = RoundedCornerShape(28.dp), color = Color(0xCC161616), border = BorderStroke(1.dp, Color(0x22FFFFFF))) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(movie.name, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                        InfoChip("${movie.format} • ${movie.rating}")
                        SummaryRow("Date & Time", "${selectedDate.fullLabel} • ${showTime.time}")
                        SummaryRow("Theatre", "${theatre.name}, ${theatre.location}")
                        SummaryRow("Seats", selectedSeats.joinToString()); SummaryRow("Ticket Total", "₹$totalAmount"); SummaryRow("Convenience Fee", "₹0")
                    }
                }
            }
        }
        Surface(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp), shape = RoundedCornerShape(28.dp), color = Color(0xFF111113).copy(alpha = 0.97f), border = BorderStroke(1.dp, Color(0x20FFFFFF))) {
            Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Total Payable", color = TextGray, fontSize = 11.sp); Text("₹$totalAmount", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold) }
                Button(onClick = { navController.navigate("payment") }, enabled = selectedSeats.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple), shape = RoundedCornerShape(18.dp)) {
                    Text("Continue", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextGray, fontSize = 13.sp); Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

@Composable
fun PaymentScreen(navController: NavController, bookingViewModel: BookingViewModel, globalViewModel: GlobalViewModel) {
    val bookingState by bookingViewModel.uiState.collectAsState()
    val movie = bookingState.currentMovie
    val theatre = bookingState.selectedTheatre
    val showTime = bookingState.selectedShowTime
    val selectedDate = bookingState.selectedDate
    val selectedSeats = bookingState.selectedSeats
    val context = LocalContext.current
    val activity = context as? MainActivity

    Column(modifier = Modifier.fillMaxSize().background(BgBlack).padding(24.dp)) {
        Text("Select Payment Method", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.statusBarsPadding())
        Spacer(Modifier.height(12.dp)); Text(movie?.name.orEmpty(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp)); Text("${theatre?.name.orEmpty()} • ${selectedDate?.fullLabel.orEmpty()} • ${showTime?.time.orEmpty()}", color = TextGray, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp)); Text(selectedSeats.joinToString(), color = TextGray, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp)); Text("Total ₹${bookingState.totalAmount}", color = PrimaryPurple, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(30.dp))

        if (bookingState.isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryPurple) }
        } else {
            var showPaymentDialog by remember { mutableStateOf(false) }
            
            Button(
                onClick = { showPaymentDialog = true },
                modifier = Modifier.fillMaxWidth().height(64.dp).shadow(20.dp, RoundedCornerShape(32.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(32.dp)
            ) {
                Text("PROCEED TO PAY ₹${bookingState.totalAmount}", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Black, modifier = Modifier.padding(start = 8.dp))
            }

            if (showPaymentDialog) {
                PaymentMethodDialog(
                    amount = bookingState.totalAmount,
                    walletBalance = globalViewModel.state.walletBalance,
                    onDismiss = { showPaymentDialog = false },
                    onRazorpaySelected = {
                        activity?.startPayment(bookingState.totalAmount, "Movie: ${movie?.name}")
                        showPaymentDialog = false
                    },
                    onWalletSelected = {
                        if (globalViewModel.deductWallet(bookingState.totalAmount)) {
                            bookingViewModel.confirmCurrentBooking()
                            navController.navigate("success")
                        }
                        showPaymentDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun BookingSuccessScreen(navController: NavController, bookingViewModel: BookingViewModel, globalViewModel: GlobalViewModel) {
    val context = LocalContext.current
    val bookingState by bookingViewModel.uiState.collectAsState()
    val latestBooking = bookingState.latestBooking
    val qrBitmap = remember(latestBooking) { QRCodeGenerator.generate("TICKETX-${latestBooking?.title}-${latestBooking?.seats?.joinToString(",")}") }

    // Sync to Global System
    LaunchedEffect(latestBooking) {
        latestBooking?.let {
            globalViewModel.addTicket(
                com.kumaran.tickexp.data.model.Ticket(
                    id = "MV-${System.currentTimeMillis()}",
                    type = "Movie",
                    title = it.title,
                    date = it.timing.split(" • ")[0],
                    time = it.timing.split(" • ")[1],
                    seat = it.seats.joinToString(", "),
                    price = it.totalAmount,
                    qrData = "TICKETX-${it.title}-${it.seats.joinToString(",")}",
                    theatre = it.theatre
                )
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BgBlack).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.fillMaxSize().blur(40.dp).background(PrimaryPurple.copy(alpha = 0.2f), CircleShape))
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(80.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Booking Confirmed", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk); Spacer(Modifier.height(32.dp))
        Surface(modifier = Modifier.size(220.dp), color = Color.White, shape = RoundedCornerShape(24.dp), shadowElevation = 20.dp) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                qrBitmap?.let { Image(bitmap = it.asImageBitmap(), contentDescription = "Ticket QR Code", modifier = Modifier.fillMaxSize()) } ?: CircularProgressIndicator(color = PrimaryPurple)
            }
        }
        Spacer(Modifier.height(32.dp)); Text(latestBooking?.title.orEmpty(), color = PrimaryPurple, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
        Spacer(Modifier.height(8.dp)); Text(latestBooking?.theatre.orEmpty(), color = Color.Gray, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp)); Text(latestBooking?.timing.orEmpty(), color = Color.Gray, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SummarySmall("SEATS", latestBooking?.seats?.joinToString().orEmpty())
            SummarySmall("AMOUNT", "₹${latestBooking?.totalAmount ?: 0}")
        }
        Spacer(Modifier.height(48.dp))
        Button(onClick = { navController.navigate(Screen.Home.route) { popUpTo(0) } }, modifier = Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple), shape = RoundedCornerShape(32.dp)) {
            Text("Go to Dashboard", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { 
                latestBooking?.let {
                    val ticket = com.kumaran.tickexp.data.model.Ticket(
                        id = "MV-${System.currentTimeMillis()}",
                        type = "Movie",
                        title = it.title,
                        date = it.timing.split(" • ")[0],
                        time = it.timing.split(" • ")[1],
                        seat = it.seats.joinToString(", "),
                        price = it.totalAmount,
                        qrData = "TICKETX-${it.title}-${it.seats.joinToString(",")}",
                        theatre = it.theatre
                    )
                    val file = com.kumaran.tickexp.utils.TicketExporter.exportToPdf(context, ticket)
                    if (file != null) com.kumaran.tickexp.utils.TicketExporter.shareTicket(context, file)
                }
            },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            border = BorderStroke(1.dp, PrimaryPurple),
            shape = RoundedCornerShape(32.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = PrimaryPurple)
            Spacer(Modifier.width(8.dp))
            Text("Download & Share PDF", color = PrimaryPurple, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SummarySmall(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BusesScreen(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize().background(BgBlack), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Bus Bookings", color = Color.White, fontSize = 24.sp); Text("Coming Soon...", color = Color.Gray)
            Button(onClick = { navController.popBackStack() }) { Text("Back") }
        }
    }
}

@Composable
fun TrainStatusScreen(navController: NavController, viewModel: TrainViewModel) {
    val state by viewModel.uiState.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize().background(BgBlack)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TrainHeader(navController)
            
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text("LIVE STATUS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple, modifier = Modifier.padding(vertical = 16.dp))
                
                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryPurple)
                    }
                } else if (state.error != null) {
                    Text(state.error!!, color = Color.Red, modifier = Modifier.padding(24.dp))
                } else {
                    state.liveStatus?.body?.let { body ->
                        // Current Station Card
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.TrainTracker.route) }, 
                            shape = RoundedCornerShape(16.dp), 
                            color = SurfaceLow, 
                            border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.2f))
                        ) {
                            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("CURRENT LOCATION", fontSize = 10.sp, color = PrimaryPurple, fontWeight = FontWeight.Bold)
                                    Text(body.currentStation, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("On Time", fontSize = 12.sp, color = Color(0xFF4ADE80))
                                }
                                Icon(Icons.Default.Timeline, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(32.dp))
                            }
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("ROUTE TIMELINE", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { navController.navigate(Screen.TrainTracker.route) }) {
                                Text("Full Tracker View", color = PrimaryPurple, fontSize = 12.sp)
                            }
                        }
                        
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(body.stations) { station ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(if (station.stationName == body.currentStation) PrimaryPurple else Color.Gray))
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(station.stationName, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("Arrival: ${station.arrivalTime}", color = TextGray, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    } ?: Text("No status data available", color = Color.White, modifier = Modifier.padding(24.dp))
                }
            }
        }
    }
}

@Composable
fun TrainTrackerScreen(navController: NavController, viewModel: TrainViewModel) {
    val state by viewModel.uiState.collectAsState()
    val status = state.liveStatus ?: return

    Box(modifier = Modifier.fillMaxSize().background(BgBlack)) {
        // Dynamic Glow
        Box(modifier = Modifier.size(400.dp).align(Alignment.TopCenter).offset(y = (-100).dp).background(PrimaryPurple.copy(alpha = 0.05f), CircleShape).blur(80.dp))

        Column(modifier = Modifier.fillMaxSize()) {
            // High-fidelity Header
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.clip(CircleShape).background(SurfaceLow)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(state.selectedTrain?.trainName ?: "Express Train", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("LIVE TRACKING", color = PrimaryPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
            }

            // Train Moving Animation Hero
            Box(modifier = Modifier.fillMaxWidth().height(180.dp).padding(horizontal = 24.dp).clip(RoundedCornerShape(32.dp)).background(SurfaceLow).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(32.dp))) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    val infiniteTransition = rememberInfiniteTransition(label = "train_pulse")
                    val pulse by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.2f, animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse), label = "pulse")
                    
                    Box(modifier = Modifier.size(64.dp).scale(pulse).background(PrimaryPurple.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Train, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("CURRENTLY AT", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(status.body.currentStation, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(Modifier.height(32.dp))

            // Visual Timeline
            LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp)) {
                val stations = status.body.stations
                val currentIndex = stations.indexOfFirst { it.stationName == status.body.currentStation }

                items(stations.size) { index ->
                    val station = stations[index]
                    val isCurrent = index == currentIndex
                    val isPassed = index < currentIndex
                    
                    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                        // Timeline Column
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(20.dp).clip(CircleShape).background(if (isCurrent) PrimaryPurple else if (isPassed) PrimaryPurple.copy(alpha = 0.4f) else SurfaceLow).border(2.dp, if (isCurrent || isPassed) PrimaryPurple else Color.Gray, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isPassed && !isCurrent) Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                            }
                            if (index < stations.size - 1) {
                                Box(modifier = Modifier.width(2.dp).weight(1f).background(if (isPassed) PrimaryPurple.copy(alpha = 0.4f) else Color.DarkGray))
                            }
                        }

                        // Station Detail
                        Column(modifier = Modifier.padding(start = 24.dp, bottom = 32.dp)) {
                            Text(station.stationName, color = if (isCurrent) Color.White else TextGray, fontSize = 18.sp, fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Medium)
                            Text("Arrival: ${station.arrivalTime}", color = if (isCurrent) PrimaryPurple else TextGray.copy(alpha = 0.5f), fontSize = 12.sp)
                            if (isCurrent) {
                                Surface(modifier = Modifier.padding(top = 8.dp), color = PrimaryPurple.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.2f))) {
                                    Text("PLATFORM 3 • ON TIME", color = PrimaryPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BusSeatSelectionScreen(navController: NavController, viewModel: BusViewModel, globalViewModel: GlobalViewModel) {
    val state = viewModel.state
    val bus = state.selectedBus ?: return
    
    Column(modifier = Modifier.fillMaxSize().background(BgBlack)) {
        // Header
        Surface(modifier = Modifier.fillMaxWidth(), color = SurfaceLow.copy(alpha = 0.8f), shadowElevation = 8.dp) {
            Row(modifier = Modifier.padding(24.dp).padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = PrimaryPurple)
                }
                Text("Select Seats", style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White), modifier = Modifier.padding(start = 12.dp))
            }
        }

        Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
            // Bus Summary
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text(bus.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("${bus.type} • Premium", color = TextGray, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("₹${bus.price}", color = PrimaryPurple, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("PER SEAT", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Stats Bar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BusStatItem("ROUTE", "${state.fromCity.take(3)} - ${state.toCity.take(3)}".uppercase(), Modifier.weight(1f))
                BusStatItem("SEATS", "${state.selectedSeats.size} Selected", Modifier.weight(1f), PrimaryPurple)
                BusStatItem("TOTAL", "₹${state.selectedSeats.size * bus.price}", Modifier.weight(1f))
            }

            Spacer(Modifier.height(32.dp))

            // Legend
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(SurfaceLow).padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                BusLegendItem("Available", Color.White.copy(alpha = 0.2f))
                BusLegendItem("Selected", PrimaryPurple)
                BusLegendItem("Booked", Color.DarkGray)
            }

            Spacer(Modifier.height(32.dp))

            // Bus Layout Container
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SurfaceLow).border(1.dp, Color.White.copy(alpha = 0.05f)).padding(24.dp)) {
                Column {
                    // Front / Driver
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.End) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.RadioButtonChecked, contentDescription = null, tint = TextGray, modifier = Modifier.size(32.dp))
                            Text("DRIVER", color = TextGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    // Seats (2+2 Layout)
                    val rows = state.seats.chunked(4)
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        rows.forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    BusSeatItem(row[0], state.selectedSeats.contains(row[0].id)) { viewModel.toggleSeat(row[0].id) }
                                    BusSeatItem(row[1], state.selectedSeats.contains(row[1].id)) { viewModel.toggleSeat(row[1].id) }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    BusSeatItem(row[2], state.selectedSeats.contains(row[2].id)) { viewModel.toggleSeat(row[2].id) }
                                    BusSeatItem(row[3], state.selectedSeats.contains(row[3].id)) { viewModel.toggleSeat(row[3].id) }
                                }
                            }
                        }
                    }
                }
                
                // Aisle Label
                Text("CENTRAL AISLE", color = Color.White.copy(alpha = 0.05f), fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center).rotate(90f))
            }
            
            Spacer(Modifier.height(120.dp))
        }
    }

    // Bottom Action
    if (state.selectedSeats.isNotEmpty()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp).padding(bottom = 16.dp), shape = RoundedCornerShape(32.dp), color = SurfaceLow, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SEAT NUMBERS", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold)
                        Text(state.selectedSeats.joinToString(", "), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
                    }
                    val context = LocalContext.current
                    val activity = context as? MainActivity
                    var showPaymentDialog by remember { mutableStateOf(false) }
                    val totalAmount = state.selectedSeats.size * bus.price
                    
                    Button(onClick = { showPaymentDialog = true }, shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple), modifier = Modifier.height(56.dp)) {
                        Text("Proceed to Pay", color = Color.Black, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Black, modifier = Modifier.padding(start = 8.dp))
                    }

                    if (showPaymentDialog) {
                        PaymentMethodDialog(
                            amount = totalAmount,
                            walletBalance = globalViewModel.state.walletBalance,
                            onDismiss = { showPaymentDialog = false },
                            onRazorpaySelected = {
                                viewModel.confirmBooking()
                                activity?.startPayment(totalAmount, "Bus Ticket: ${bus.name}")
                                showPaymentDialog = false
                                // Navigation will happen in onPaymentSuccess
                            },
                            onWalletSelected = {
                                if (globalViewModel.deductWallet(totalAmount)) {
                                    viewModel.confirmBooking()
                                    navController.navigate(Screen.BusConfirmation.route)
                                }
                                showPaymentDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BusStatItem(label: String, value: String, modifier: Modifier = Modifier, color: Color = Color.White) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = SurfaceLow, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun BusLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(color).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(2.dp)))
        Text(label, color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun BusSeatItem(seat: BusSeat, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = when {
        !seat.isAvailable -> Color.DarkGray.copy(alpha = 0.3f)
        isSelected -> PrimaryPurple
        else -> Color.Transparent
    }
    val borderColor = if (isSelected || !seat.isAvailable) Color.Transparent else PrimaryPurple.copy(alpha = 0.3f)
    val textColor = if (isSelected) Color.Black else if (!seat.isAvailable) TextGray.copy(alpha = 0.3f) else PrimaryPurple.copy(alpha = 0.6f)

    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(bgColor).border(1.dp, borderColor, RoundedCornerShape(8.dp)).clickable(enabled = seat.isAvailable) { onClick() }, contentAlignment = Alignment.Center) {
        Text(seat.number, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
    }
}

@Composable
fun BusConfirmationScreen(navController: NavController, viewModel: BusViewModel, globalViewModel: GlobalViewModel) {
    val state = viewModel.state
    val bus = state.selectedBus ?: return

    // Record to Global on entry
    LaunchedEffect(Unit) {
        globalViewModel.addTicket(
            com.kumaran.tickexp.data.model.Ticket(
                id = state.bookingId,
                type = "Bus",
                title = bus.name,
                source = state.fromCity,
                destination = state.toCity,
                date = state.journeyDate,
                seat = state.selectedSeats.joinToString(", "),
                price = state.selectedSeats.size * bus.price,
                qrData = "TICKETX-BUS-${state.bookingId}",
                theatre = "${state.fromCity} Terminal"
            )
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(BgBlack).verticalScroll(rememberScrollState())) {
        // Header
        Row(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.Menu, contentDescription = null, tint = PrimaryPurple)
            }
            Text("TickExp", style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White), modifier = Modifier.padding(start = 12.dp).weight(1f))
            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(28.dp))
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Success Hero
            Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.fillMaxSize().blur(40.dp).background(PrimaryPurple.copy(alpha = 0.2f), CircleShape))
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(80.dp))
            }
            Text("Booking Success", style = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 32.sp, color = Color.White))
            Text("TRANSACTION ID: ${state.bookingId}", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

            Spacer(Modifier.height(32.dp))

            // Ticket Card
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = SurfaceLow.copy(alpha = 0.6f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))) {
                Box(modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White.copy(alpha = 0.05f), modifier = Modifier.size(200.dp).align(Alignment.BottomEnd))
                    
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(bus.name.uppercase(), color = PrimaryPurple, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
                                Text("PLATINUM EXPRESS", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹${state.selectedSeats.size * bus.price}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text("CONFIRMED", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(32.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("DEPARTURE", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(state.fromCity.ifEmpty { "CHENNAI" }.uppercase(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text("Koyambedu CMBT", color = PrimaryPurple, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("ARRIVAL", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(state.toCity.ifEmpty { "BANGALORE" }.uppercase(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text("Majestic Hub", color = Color(0xFFE197FC), fontSize = 12.sp)
                            }
                        }

                        Spacer(Modifier.height(32.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(Modifier.height(32.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("DATE & TIME", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(state.journeyDate.uppercase(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("${bus.departure} UTC", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(16.dp))
                                Text("DURATION", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(bus.duration, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("SEAT NUMBERS", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    state.selectedSeats.forEach { seat ->
                                        Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.05f)) {
                                            Text(seat, color = PrimaryPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                                        }
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Text("CLASS", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("First Kinetic", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // QR Code
            val qrBitmap = QRCodeGenerator.generate(state.bookingId, 512)
            Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp), shape = RoundedCornerShape(24.dp), color = SurfaceLow) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(modifier = Modifier.size(200.dp), shape = RoundedCornerShape(12.dp), color = Color.White) {
                        qrBitmap?.let { 
                            androidx.compose.foundation.Image(
                                bitmap = it.asImageBitmap(), 
                                contentDescription = "QR Code", 
                                modifier = Modifier.padding(16.dp)
                            ) 
                        }
                    }
                    Text("SCAN AT BOARDING", color = PrimaryPurple, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.padding(top = 16.dp))
                }
            }

            Spacer(Modifier.height(40.dp))

            // Action Buttons
            val context = LocalContext.current
            Button(
                onClick = { 
                    val ticket = com.kumaran.tickexp.data.model.Ticket(
                        id = state.bookingId,
                        type = "Bus",
                        title = bus.name,
                        source = state.fromCity,
                        destination = state.toCity,
                        date = state.journeyDate,
                        seat = state.selectedSeats.joinToString(", "),
                        price = state.selectedSeats.size * bus.price,
                        qrData = "TICKETX-BUS-${state.bookingId}",
                        theatre = "${state.fromCity} Terminal"
                    )
                    val file = com.kumaran.tickexp.utils.TicketExporter.exportToPdf(context, ticket)
                    if (file != null) com.kumaran.tickexp.utils.TicketExporter.shareTicket(context, file)
                }, 
                modifier = Modifier.fillMaxWidth().height(64.dp), 
                shape = RoundedCornerShape(32.dp), 
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black)
                Text("Download Ticket", color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { navController.navigate(Screen.Home.route) { popUpTo(0) } }, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                Icon(Icons.Default.Home, contentDescription = null, tint = Color.White)
                Text("Go Home", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}
