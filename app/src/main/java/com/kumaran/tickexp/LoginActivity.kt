package com.kumaran.tickexp

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kumaran.tickexp.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val navController = rememberNavController()
            val authViewModel: AuthViewModel = viewModel()

            // Auto-navigation if already logged in
            LaunchedEffect(authViewModel.isLoggedIn) {
                if (authViewModel.isLoggedIn) {
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
            }
            
            NavHost(navController = navController, startDestination = "splash") {
                composable("splash") {
                    SplashScreen(navController, authViewModel)
                }
                composable("login") {
                    LoginScreen(authViewModel)
                }
            }
        }
    }
}

@Composable
fun SplashScreen(navController: NavController, authViewModel: AuthViewModel) {
    var startAnimation by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1.2f else 0.8f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1500),
        label = "alpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "progress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Glow effect
        Box(modifier = Modifier.size(300.dp).blur(60.dp).background(Color(0xFFC084FC).copy(alpha = 0.1f * alpha), CircleShape))
        
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(alpha).scale(scale)) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .shadow(30.dp, RoundedCornerShape(32.dp), spotColor = Color(0xFFC084FC))
            )
            Spacer(Modifier.height(32.dp))
            Text("TickExp", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.SansSerif, letterSpacing = 4.sp)
            Text("NEXT-GEN BOOKING ECOSYSTEM", color = Color(0xFFC084FC), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            
            Spacer(Modifier.height(60.dp))
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(Brush.horizontalGradient(listOf(Color(0xFF9333EA), Color(0xFFC084FC))))
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2500)
        if (!authViewModel.isLoggedIn) {
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }
}

@Composable
fun LoginScreen(viewModel: AuthViewModel) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var retypePassword by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    
    var isSignUp by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))

        // Logo
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "TickExp Logo",
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(28.dp))
                .shadow(40.dp, RoundedCornerShape(28.dp), spotColor = Color(0xFFC084FC))
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("TickExp", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        Text(if (isSignUp) "JOIN THE ECOSYSTEM" else "WELCOME TO THE FUTURE.", fontSize = 10.sp, color = Color(0xFFC084FC), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (isSignUp) {
            InputField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Full Name",
                icon = Icons.Default.Person
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        InputField(
            value = email,
            onValueChange = { email = it },
            placeholder = "Identity / Email",
            icon = Icons.Default.AlternateEmail
        )
        
        if (isSignUp) {
            Spacer(modifier = Modifier.height(16.dp))
            InputField(
                value = phone,
                onValueChange = { phone = it },
                placeholder = "Phone Number",
                icon = Icons.Default.Phone
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        InputField(
            value = password,
            onValueChange = { password = it },
            placeholder = if (isSignUp) "Create Access Key" else "Access Key",
            icon = Icons.Default.VpnKey,
            isPassword = true,
            passwordVisible = visible,
            onVisibilityToggle = { visible = !visible }
        )

        if (isSignUp) {
            Spacer(modifier = Modifier.height(16.dp))
            InputField(
                value = retypePassword,
                onValueChange = { retypePassword = it },
                placeholder = "Retype Access Key",
                icon = Icons.Default.Lock,
                isPassword = true,
                passwordVisible = visible,
                onVisibilityToggle = { visible = !visible }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            // Gender Selection
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("GENDER: ", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { gender = "Male" }) {
                    RadioButton(selected = gender == "Male", onClick = { gender = "Male" }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFC084FC)))
                    Text("Male", color = Color.White, fontSize = 14.sp)
                }
                Spacer(Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { gender = "Female" }) {
                    RadioButton(selected = gender == "Female", onClick = { gender = "Female" }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFC084FC)))
                    Text("Female", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        viewModel.errorMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = it, color = Color(0xFFFF6E84), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                if (isSignUp) {
                    if (password != retypePassword) {
                        Toast.makeText(context, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.signup(name, email, phone, password, gender)
                    }
                } else {
                    viewModel.login(email, password)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(20.dp, RoundedCornerShape(32.dp), ambientColor = Color(0xFFC084FC))
                .background(Brush.horizontalGradient(listOf(Color(0xFF9333EA), Color(0xFFC084FC))), RoundedCornerShape(32.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(32.dp),
            enabled = !viewModel.isLoading
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
            } else {
                Text(if (isSignUp) "INITIALIZE ACCOUNT" else "ACCESS PLATFORM", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("OR CONTINUE WITH", color = Color.Gray, fontSize = 12.sp)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
            SocialBtn("Google", Modifier.weight(1f))
            SocialBtn("Apple", Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = if (isSignUp) "Already have an account? Login" else "New here? Signup",
            color = Color(0xFFC084FC),
            modifier = Modifier.clickable { isSignUp = !isSignUp }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onVisibilityToggle: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color(0xFF1A1A1A), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFFCC97FF))
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, color = Color.Gray, fontSize = 16.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (isPassword) {
            IconButton(onClick = onVisibilityToggle) {
                Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = Color.Gray)
            }
        }
    }
}

@Composable
fun SocialBtn(text: String, modifier: Modifier) {
    val context = LocalContext.current
    Button(
        onClick = { Toast.makeText(context, "Social Login Currently Disabled", Toast.LENGTH_SHORT).show() },
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(text, color = Color.White)
    }
}
