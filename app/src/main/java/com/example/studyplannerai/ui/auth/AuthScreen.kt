package com.example.studyplannerai.ui.auth

import android.util.Patterns
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.studyplannerai.R
import com.example.studyplannerai.ui.theme.*
import com.example.studyplannerai.viewmodel.auth.AuthViewModel

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = viewModel(),
    onNavigateHome: () -> Unit = {}
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    val isLoading = viewModel.isLoading.value
    val errorMessage = viewModel.errorMessage.value
    val successMessage = viewModel.successMessage.value
    val isLoggedIn = viewModel.isLoggedIn.value

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current.applicationContext

    LaunchedEffect(isLoggedIn) { if (isLoggedIn) onNavigateHome() }
    LaunchedEffect(successMessage) {
        successMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    fun validateInputs(): Boolean {
        var isValid = true
        if (email.isBlank()) { emailError = "Email cannot be empty"; isValid = false }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { emailError = "Invalid email format"; isValid = false }
        else emailError = null
        if (password.isBlank()) { passwordError = "Password cannot be empty"; isValid = false }
        else if (password.length < 6) { passwordError = "Password must be at least 6 characters"; isValid = false }
        else passwordError = null
        return isValid
    }

    // Ambient glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "authGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )
    val glowAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowAlpha2"
    )

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Surface700,
                    contentColor = OnSurface100,
                    actionColor = Violet300,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(BorderStroke(1.dp, Rose400.copy(0.4f)), RoundedCornerShape(12.dp))
                )
            }
        },
        containerColor = Surface900,
        modifier = Modifier.imePadding()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Surface900, Surface800, Surface900)))
                .padding(paddingValues)
        ) {
            // Background glow blobs
            Box(Modifier.size(260.dp).align(Alignment.TopEnd).offset(x = 80.dp, y = (-60).dp)
                .clip(CircleShape).background(Violet400.copy(alpha = glowAlpha * 0.15f)))
            Box(Modifier.size(200.dp).align(Alignment.BottomStart).offset(x = (-60).dp, y = 80.dp)
                .clip(CircleShape).background(Cyan400.copy(alpha = glowAlpha2 * 0.12f)))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo icon
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(bottom = 8.dp)) {
                    Box(Modifier.size((90 * (0.9f + glowAlpha * 0.1f)).dp).clip(CircleShape)
                        .background(Violet400.copy(alpha = glowAlpha * 0.18f)))
                    Box(Modifier.size(80.dp).clip(RoundedCornerShape(20.dp))
                        .background(Surface800),
                        contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Study Planner AI Logo",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "AI Smart\nStudy Planner",
                    style = MaterialTheme.typography.displaySmall,
                    color = OnSurface100,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text("Plan Smart. Study Better.", style = MaterialTheme.typography.bodyMedium, color = OnSurface300)
                Spacer(Modifier.height(36.dp))

                // Auth card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Surface800)
                        .border(BorderStroke(1.dp, Brush.linearGradient(listOf(Violet400.copy(0.3f), Cyan400.copy(0.15f)))), RoundedCornerShape(24.dp))
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Login/Signup toggle pills
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .background(Surface700).padding(4.dp)
                        ) {
                            Row {
                                listOf("Sign In", "Sign Up").forEachIndexed { i, label ->
                                    val active = (i == 0) == isLoginMode
                                    Box(
                                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                            .background(if (active) Brush.horizontalGradient(listOf(Violet500, Violet400)) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)))
                                            .clickable {
                                                isLoginMode = i == 0
                                                emailError = null; passwordError = null
                                                email = ""; password = ""
                                                viewModel.clearMessages()
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(label, style = MaterialTheme.typography.labelLarge,
                                            color = if (active) Color.White else OnSurface300,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        Text(
                            if (isLoginMode) "Welcome Back 👋" else "Create Account 🎓",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, color = OnSurface100
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (isLoginMode) "Sign in to continue your study journey" else "Start your AI-powered study adventure",
                            style = MaterialTheme.typography.bodySmall, color = OnSurface300, textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))

                        // Email field
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; emailError = null },
                            label = { Text("Email address", color = OnSurface300) },
                            leadingIcon = { Icon(Icons.Default.Email, null, tint = Violet300, modifier = Modifier.size(20.dp)) },
                            singleLine = true,
                            isError = emailError != null,
                            supportingText = { emailError?.let { Text(it, color = Rose400) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Violet400, unfocusedBorderColor = Surface600,
                                errorBorderColor = Rose400, focusedTextColor = OnSurface100,
                                unfocusedTextColor = OnSurface100, cursorColor = Violet300
                            )
                        )
                        Spacer(Modifier.height(12.dp))

                        // Password field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; passwordError = null },
                            label = { Text("Password", color = OnSurface300) },
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Violet300, modifier = Modifier.size(20.dp)) },
                            singleLine = true,
                            isError = passwordError != null,
                            supportingText = { passwordError?.let { Text(it, color = Rose400) } },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        null, tint = OnSurface300, modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Violet400, unfocusedBorderColor = Surface600,
                                errorBorderColor = Rose400, focusedTextColor = OnSurface100,
                                unfocusedTextColor = OnSurface100, cursorColor = Violet300
                            )
                        )

                        if (isLoginMode) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                TextButton(onClick = { showForgotPasswordDialog = true }, contentPadding = PaddingValues(0.dp)) {
                                    Text("Forgot Password?", color = Violet300, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        } else Spacer(Modifier.height(12.dp))

                        Spacer(Modifier.height(20.dp))

                        // Gradient action button
                        Box(
                            modifier = Modifier.fillMaxWidth().height(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (!isLoading) Brush.horizontalGradient(listOf(Violet500, Cyan400)) else Brush.horizontalGradient(listOf(Surface600, Surface600)))
                                .clickable(enabled = !isLoading) { if (validateInputs()) { if (isLoginMode) viewModel.logIn(email, password) else viewModel.signUp(email, password) } },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(if (isLoginMode) Icons.Default.Login else Icons.Default.PersonAdd, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Text(if (isLoginMode) "Sign In" else "Create Account", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isLoginMode) "Don't have an account? " else "Already have an account? ",
                        style = MaterialTheme.typography.bodyMedium, color = OnSurface300
                    )
                    TextButton(onClick = {
                        isLoginMode = !isLoginMode
                        emailError = null; passwordError = null; email = ""; password = ""
                        viewModel.clearMessages()
                    }, enabled = !isLoading) {
                        Text(if (isLoginMode) "Sign Up" else "Sign In", color = Violet300, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (showForgotPasswordDialog) {
            var resetEmail by remember { mutableStateOf(email) }
            var resetEmailError by remember { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = { showForgotPasswordDialog = false },
                containerColor = Surface800,
                title = { Text("Reset Password", color = OnSurface100) },
                text = {
                    Column {
                        Text("Enter your email address and we'll send you a link to reset your password.", color = OnSurface300, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it; resetEmailError = null },
                            label = { Text("Email address", color = OnSurface300) },
                            singleLine = true,
                            isError = resetEmailError != null,
                            supportingText = { resetEmailError?.let { Text(it, color = Rose400) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Violet400, unfocusedBorderColor = Surface600,
                                errorBorderColor = Rose400, focusedTextColor = OnSurface100,
                                unfocusedTextColor = OnSurface100, cursorColor = Violet300
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (resetEmail.isBlank()) {
                                resetEmailError = "Email cannot be empty"
                            } else if (!Patterns.EMAIL_ADDRESS.matcher(resetEmail).matches()) {
                                resetEmailError = "Invalid email format"
                            } else {
                                viewModel.resetPassword(resetEmail)
                                showForgotPasswordDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Violet500)
                    ) {
                        Text("Send Link", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showForgotPasswordDialog = false }) {
                        Text("Cancel", color = OnSurface300)
                    }
                }
            )
        }
    }
}