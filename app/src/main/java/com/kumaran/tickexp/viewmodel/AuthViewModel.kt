package com.kumaran.tickexp.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kumaran.tickexp.auth.AuthRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.*

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AuthRepository()
    private val prefs = application.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isLoggedIn by mutableStateOf(repo.isLoggedIn())
        private set

    var userName by mutableStateOf(prefs.getString("user_name", "GUEST USER") ?: "GUEST USER")
    var userEmail by mutableStateOf(prefs.getString("user_email", "") ?: "")
    var userPhone by mutableStateOf(prefs.getString("user_phone", "") ?: "")
    var userProfileImage by mutableIntStateOf(prefs.getInt("user_profile_image", com.kumaran.tickexp.R.drawable.profile_male))
    
    private val registeredPhones = mutableSetOf<String>()

    fun saveUser(name: String, email: String, phone: String, image: Int) {
        userName = name
        userEmail = email
        userPhone = phone
        userProfileImage = image
        prefs.edit().apply {
            putString("user_name", name)
            putString("user_email", email)
            putString("user_phone", phone)
            putInt("user_profile_image", image)
            apply()
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Email and password cannot be empty"
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val result = repo.login(email, password)

            result.onSuccess {
                isLoggedIn = true
            }.onFailure {
                errorMessage = it.message
            }

            isLoading = false
        }
    }

    fun signup(name: String, email: String, phone: String, password: String, gender: String) {
        if (registeredPhones.contains(phone)) {
            errorMessage = "Account already exists with this phone number"
            return
        }
        
        if (password.length < 6) {
            errorMessage = "Password must be at least 6 characters"
            return
        }

        val profileImg = if (gender.lowercase() == "female") {
            com.kumaran.tickexp.R.drawable.profile_female
        } else {
            com.kumaran.tickexp.R.drawable.profile_male
        }
        
        saveUser(name, email, phone, profileImg)
        registeredPhones.add(phone)

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val result = repo.signup(email, password)

            result.onSuccess {
                isLoggedIn = true
            }.onFailure {
                errorMessage = it.message
            }

            isLoading = false
        }
    }

    fun logout() {
        repo.logout()
        isLoggedIn = false
        prefs.edit().clear().apply()
        userName = "GUEST USER"
        userEmail = ""
        userPhone = ""
        userProfileImage = com.kumaran.tickexp.R.drawable.profile_male
    }
}
