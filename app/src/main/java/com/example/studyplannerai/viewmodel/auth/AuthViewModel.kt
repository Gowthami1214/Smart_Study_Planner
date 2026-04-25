package com.example.studyplannerai.viewmodel.auth

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyplannerai.core.util.Resource
import com.example.studyplannerai.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserProfileUiState(
    val userName: String = "",
    val userEmail: String = ""
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

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
        if (repository.isUserLoggedIn()) {
            updateUserState()
            isLoggedIn.value = true
        }
    }

    fun logIn(email: String, password: String) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            successMessage.value = null

            when (val result = repository.logIn(email, password)) {
                is Resource.Success -> {
                    updateUserState(fallbackEmail = email)
                    successMessage.value = "Login Successful!"
                    isLoggedIn.value = true
                }
                is Resource.Error -> {
                    errorMessage.value = result.message
                }
                is Resource.Loading -> {}
            }
            isLoading.value = false
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            successMessage.value = null

            when (val result = repository.signUp(email, password)) {
                is Resource.Success -> {
                    updateUserState(fallbackEmail = email)
                    successMessage.value = "Signup Successful!"
                    isLoggedIn.value = true
                }
                is Resource.Error -> {
                    errorMessage.value = result.message
                }
                is Resource.Loading -> {}
            }
            isLoading.value = false
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

    fun resetPassword(email: String) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            successMessage.value = null

            when (val result = repository.sendPasswordResetEmail(email)) {
                is Resource.Success -> {
                    successMessage.value = "Password reset email sent."
                }
                is Resource.Error -> {
                    errorMessage.value = result.message
                }
                is Resource.Loading -> {}
            }
            isLoading.value = false
        }
    }

    fun clearMessages() {
        errorMessage.value = null
        successMessage.value = null
    }

    /** Force-refresh the user profile. Call this when the Profile screen becomes visible. */
    fun refreshProfile() {
        if (repository.isUserLoggedIn()) updateUserState()
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
