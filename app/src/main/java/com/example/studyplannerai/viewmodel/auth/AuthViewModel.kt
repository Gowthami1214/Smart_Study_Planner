package com.example.studyplannerai.viewmodel.auth

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.studyplannerai.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UserProfileUiState(
    val userName: String = "",
    val userEmail: String = ""
)

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _profileState = MutableStateFlow(UserProfileUiState())
    val profileState: StateFlow<UserProfileUiState> = _profileState.asStateFlow()

    var isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val successMessage = mutableStateOf<String?>(null)

    var isLoggedIn = mutableStateOf(false)
    var currentUserEmail = mutableStateOf<String?>(null)
    var currentUserId = mutableStateOf<String?>(null)

    init {
        checkSession()
    }

    private fun checkSession() {
        val user = repository.getCurrentUser()
        if (user != null) {
            updateUserState()
            isLoggedIn.value = true
        }
    }

    fun logIn(email: String, password: String) {
        isLoading.value = true
        errorMessage.value = null
        successMessage.value = null

        repository.logIn(email = email, password = password) { success, error ->
            isLoading.value = false
            if (success) {
                updateUserState(fallbackEmail = email)
                successMessage.value = "Login Successful!"
                isLoggedIn.value = true
            } else {
                errorMessage.value = error
            }
        }
    }

    fun signUp(email: String, password: String) {
        isLoading.value = true
        errorMessage.value = null
        successMessage.value = null

        repository.signUp(email = email, password = password) { success, error ->
            isLoading.value = false
            if (success) {
                updateUserState(fallbackEmail = email)
                successMessage.value = "Signup Successful!"
                isLoggedIn.value = true
            } else {
                errorMessage.value = error
            }
        }
    }

    fun logOut() {
        repository.logOut()
        currentUserId.value = null
        currentUserEmail.value = null
        _profileState.value = UserProfileUiState()
        isLoggedIn.value = false
        successMessage.value = null
    }

    fun clearMessages() {
        errorMessage.value = null
        successMessage.value = null
    }

    private fun updateUserState(fallbackEmail: String? = null) {
        val user = repository.getCurrentUser()
        val email = user?.email ?: fallbackEmail.orEmpty()
        val name = user?.displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
            .replaceFirstChar { character ->
                if (character.isLowerCase()) character.titlecase() else character.toString()
            }

        currentUserId.value = user?.uid
        currentUserEmail.value = email.ifBlank { null }
        _profileState.update {
            it.copy(
                userName = name.ifBlank { "Study Planner User" },
                userEmail = email
            )
        }
    }
}
