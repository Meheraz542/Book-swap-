package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import com.example.data.auth.OtpResult
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.MarketplaceRepository
import com.example.data.auth.AuthResult
import com.example.model.CollegeDirectory
import com.example.model.Semester
import com.example.model.Technology
import com.example.ui.components.LiquidGlassBackdrop
import com.example.ui.components.liquidGlass
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Authentication Screen.
 *
 * Restored to the original clean Tabbed interface ("Sign In" / "Sign Up"):
 * - Email & Password authentication only.
 * - Clean glassmorphism styling without any unnecessary promotional clutter.
 * - Sign Up tab for complete student registration.
 */
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    var isRegisterTab by remember { mutableStateOf(false) }

    // Sign In State - blank by default without auto-fill
    var signInEmail by remember { mutableStateOf("") }
    var signInPassword by remember { mutableStateOf("") }
    var signInPasswordVisible by remember { mutableStateOf(false) }

    // Sign Up State
    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }
    var regPhone by remember { mutableStateOf("") }
    var regCollege by remember { mutableStateOf(CollegeDirectory.FENI_POLYTECHNIC) }
    var regCollegeDropdown by remember { mutableStateOf(false) }

    val regAvailableTechs = remember(regCollege) {
        CollegeDirectory.getTechnologiesForCollege(regCollege)
    }
    var regTech by remember { mutableStateOf(Technology.CIVIL) }
    var regTechDropdown by remember { mutableStateOf(false) }
    var regSem by remember { mutableStateOf(Semester.SEM_1) }
    var regSemDropdown by remember { mutableStateOf(false) }

    // Gmail OTP Authentication State
    var isVerifyingOtp by remember { mutableStateOf(false) }
    var enteredOtpCode by remember { mutableStateOf("") }
    var sentOtpCode by remember { mutableStateOf<String?>(null) }
    var maskedEmail by remember { mutableStateOf("") }
    var otpCountdown by remember { mutableIntStateOf(60) }
    var otpSuccessCelebration by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val authUiState by viewModel.uiState.collectAsState()
    LaunchedEffect(authUiState) {
        when (val state = authUiState) {
            is AuthUiState.Error -> {
                isLoading = false
                errorMessage = state.message
            }
            is AuthUiState.Success -> {
                isLoading = false
                onAuthSuccess()
            }
            is AuthUiState.Idle -> {
                isLoading = false
            }
            is AuthUiState.Loading -> {
                isLoading = true
            }
        }
    }

    // Countdown Timer for OTP
    LaunchedEffect(isVerifyingOtp, otpCountdown) {
        if (isVerifyingOtp && otpCountdown > 0) {
            delay(1000L)
            otpCountdown -= 1
        }
    }

    // Sync technology when college changes
    LaunchedEffect(regCollege) {
        if (!regAvailableTechs.contains(regTech)) {
            regTech = regAvailableTechs.firstOrNull() ?: Technology.CIVIL
        }
    }

    // Outer modern light-mode gradient canvas with luminous ambient blooms
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val w = size.width
                val h = size.height

                // Crisp Light Mode Canvas Base (Slate-to-Indigo gentle gradient)
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xFFF8FAFC), // Slate 50
                            Color(0xFFEEF2FF), // Soft Indigo 50
                            Color(0xFFF1F5F9), // Slate 100
                            Color(0xFFE0E7FF)  // Light Indigo accent
                        )
                    )
                )

                // Top-Left Soft Violet Caustic Bloom
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFC4B5FD).copy(alpha = 0.50f), Color.Transparent),
                        center = Offset(w * 0.16f, h * 0.15f),
                        radius = w * 0.78f
                    )
                )

                // Mid-Right Soft Cyan Caustic Bloom
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFBAE6FD).copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(w * 0.88f, h * 0.40f),
                        radius = w * 0.72f
                    )
                )

                // Bottom Ambient Indigo/Violet Bloom
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFA5B4FC).copy(alpha = 0.40f), Color.Transparent),
                        center = Offset(w * 0.50f, h * 0.90f),
                        radius = w * 0.80f
                    )
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Optional top back affordance if presented over the app
            if (onDismiss != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .shadow(3.dp, CircleShape)
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back to app",
                            tint = Color(0xFF1E293B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // App Logo Emblem - Official BookSwap Squircle Logo on clean white surface
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .shadow(8.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x206366F1), spotColor = Color(0x206366F1))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White,
                                Color(0xFFE2E8F0)
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_bookswap_logo),
                    contentDescription = "BookSwap Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // App Brand Name & Subtitle
            Text(
                text = "BookSwap",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A),
                letterSpacing = 0.2.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Student Book Marketplace",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // =========================================================
            // AUTH GLASS-CARD (Crisp Light Mode Frosted Glass Surface)
            // =========================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = Color(0x18000000),
                        spotColor = Color(0x18000000)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.95f),
                                Color.White.copy(alpha = 0.88f)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White,
                                Color(0xFFE2E8F0)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                // Top Specular Sheen Highlight
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .height(4.dp)
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.90f), Color.Transparent)
                            )
                        )
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isVerifyingOtp) {
                        GmailOtpVerificationCard(
                            email = regEmail.trim(),
                            maskedEmail = maskedEmail.ifBlank { regEmail.trim() },
                            otpCode = enteredOtpCode,
                            onOtpChange = { code ->
                                if (code.length <= 6 && code.all { it.isDigit() }) {
                                    enteredOtpCode = code
                                    errorMessage = null
                                }
                            },
                            sentCodeForTest = sentOtpCode,
                            countdown = otpCountdown,
                            isLoading = isLoading,
                            errorMessage = errorMessage,
                            isSuccess = otpSuccessCelebration,
                            onAutoFill = {
                                sentOtpCode?.let {
                                    enteredOtpCode = it
                                    errorMessage = null
                                }
                            },
                            onResendOtp = {
                                if (otpCountdown == 0) {
                                    isLoading = true
                                    errorMessage = null
                                    coroutineScope.launch {
                                        try {
                                            val res = MarketplaceRepository.resendSignUpOtp()
                                            isLoading = false
                                            when (res) {
                                                is OtpResult.CodeSent -> {
                                                    sentOtpCode = res.otpCode
                                                    maskedEmail = res.maskedEmail
                                                    otpCountdown = 60
                                                    enteredOtpCode = ""
                                                }
                                                is OtpResult.Error -> {
                                                    errorMessage = res.message
                                                }
                                            }
                                        } catch (e: Exception) {
                                            isLoading = false
                                            errorMessage = e.message ?: "Failed to resend code."
                                        }
                                    }
                                }
                            },
                            onVerify = {
                                if (enteredOtpCode.length != 6) {
                                    errorMessage = "Please enter the complete 6-digit OTP code."
                                    return@GmailOtpVerificationCard
                                }
                                isLoading = true
                                errorMessage = null
                                coroutineScope.launch {
                                    try {
                                        val result = MarketplaceRepository.verifySignUpOtp(enteredOtpCode)
                                        isLoading = false
                                        when (result) {
                                            is AuthResult.Success -> {
                                                otpSuccessCelebration = true
                                                delay(1000L)
                                                onAuthSuccess()
                                            }
                                            is AuthResult.Error -> {
                                                errorMessage = result.message
                                            }
                                        }
                                    } catch (e: Exception) {
                                        isLoading = false
                                        errorMessage = e.message ?: "Verification failed. Please try again."
                                    }
                                }
                            },
                            onBackToSignUp = {
                                MarketplaceRepository.cancelPendingOtp()
                                isVerifyingOtp = false
                                enteredOtpCode = ""
                                errorMessage = null
                            }
                        )
                    } else {
                        // Animated Pill Tab Switcher: "Sign In" vs "Sign Up"
                        SmoothAuthTabSwitcher(
                            isRegisterTab = isRegisterTab,
                            onTabSelected = { isRegister ->
                                isRegisterTab = isRegister
                                errorMessage = null
                            }
                        )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Error Message Banner
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = Color(0xFFB91C1C),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Form Content (direct conditional prevents Compose IME input detachment)
                    if (!isRegisterTab) {
                            // SIGN IN TAB
                            SignInForm(
                                email = signInEmail,
                                onEmailChange = { signInEmail = it; errorMessage = null },
                                password = signInPassword,
                                onPasswordChange = { signInPassword = it; errorMessage = null },
                                passwordVisible = signInPasswordVisible,
                                onTogglePasswordVisibility = { signInPasswordVisible = !signInPasswordVisible },
                                isLoading = isLoading,
                                onSignIn = {
                                    if (signInEmail.isBlank()) {
                                        errorMessage = "Please enter your email address."
                                        return@SignInForm
                                    }
                                    if (signInPassword.isBlank()) {
                                        errorMessage = "Please enter your password."
                                        return@SignInForm
                                    }
                                    if (signInPassword.length < 6) {
                                        errorMessage = "Password must be at least 6 characters long."
                                        return@SignInForm
                                    }

                                    isLoading = true
                                    errorMessage = null
                                    coroutineScope.launch {
                                        try {
                                            val result = MarketplaceRepository.performSignIn(
                                                email = signInEmail.trim(),
                                                password = signInPassword.trim()
                                            )
                                            isLoading = false
                                            when (result) {
                                                is AuthResult.Success -> {
                                                    onAuthSuccess()
                                                }
                                                is AuthResult.Error -> {
                                                    errorMessage = result.message
                                                }
                                            }
                                        } catch (e: Exception) {
                                            isLoading = false
                                            errorMessage = e.message ?: "Sign in failed. Please check credentials."
                                        }
                                    }
                                },
                                onSwitchToRegister = {
                                    isRegisterTab = true
                                    errorMessage = null
                                }
                            )
                        } else {
                            // SIGN UP TAB
                            SignUpForm(
                                name = regName,
                                onNameChange = { regName = it; errorMessage = null },
                                email = regEmail,
                                onEmailChange = { regEmail = it; errorMessage = null },
                                password = regPassword,
                                onPasswordChange = { regPassword = it; errorMessage = null },
                                passwordVisible = regPasswordVisible,
                                onTogglePasswordVisibility = { regPasswordVisible = !regPasswordVisible },
                                phone = regPhone,
                                onPhoneChange = { regPhone = it; errorMessage = null },
                                college = regCollege,
                                onCollegeChange = { regCollege = it },
                                collegeDropdown = regCollegeDropdown,
                                onToggleCollegeDropdown = { regCollegeDropdown = it },
                                availableTechs = regAvailableTechs,
                                tech = regTech,
                                onTechChange = { regTech = it },
                                techDropdown = regTechDropdown,
                                onToggleTechDropdown = { regTechDropdown = it },
                                sem = regSem,
                                onSemChange = { regSem = it },
                                semDropdown = regSemDropdown,
                                onToggleSemDropdown = { regSemDropdown = it },
                                isLoading = isLoading,
                                onSignUp = {
                                    if (regName.isBlank()) {
                                        errorMessage = "Please enter your full name."
                                        return@SignUpForm
                                    }
                                    if (regEmail.isBlank()) {
                                        errorMessage = "Please enter your email address."
                                        return@SignUpForm
                                    }
                                    if (regPassword.length < 6) {
                                        errorMessage = "Password must be at least 6 characters long."
                                        return@SignUpForm
                                    }
                                    if (regPhone.isBlank()) {
                                        errorMessage = "Please enter your mobile phone number."
                                        return@SignUpForm
                                    }

                                    isLoading = true
                                    errorMessage = null
                                    coroutineScope.launch {
                                        try {
                                            val result = MarketplaceRepository.initiateSignUpWithOtp(
                                                name = regName.trim(),
                                                email = regEmail.trim(),
                                                password = regPassword.trim(),
                                                phone = regPhone.trim(),
                                                college = regCollege,
                                                technology = regTech,
                                                semester = regSem
                                            )
                                            isLoading = false
                                            when (result) {
                                                is OtpResult.CodeSent -> {
                                                    sentOtpCode = result.otpCode
                                                    maskedEmail = result.maskedEmail
                                                    isVerifyingOtp = true
                                                    otpCountdown = 60
                                                    enteredOtpCode = ""
                                                }
                                                is OtpResult.Error -> {
                                                    errorMessage = result.message
                                                }
                                            }
                                        } catch (e: Exception) {
                                            isLoading = false
                                            errorMessage = e.message ?: "Failed to initiate verification. Please try again."
                                        }
                                    }
                                },
                                onSwitchToSignIn = {
                                    isRegisterTab = false
                                    errorMessage = null
                                }
                            )
                        }
                    }
            }
        }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Reusable light-mode frosted text field color palette with crisp slate borders and high-contrast text.
 */
@Composable
private fun frostedInputColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF6366F1),
    unfocusedBorderColor = Color(0xFFCBD5E1),
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color(0xFFF8FAFC),
    disabledContainerColor = Color(0xFFF1F5F9),
    disabledBorderColor = Color(0xFFE2E8F0),
    focusedTextColor = Color(0xFF0F172A),
    unfocusedTextColor = Color(0xFF1E293B),
    disabledTextColor = Color(0xFF64748B),
    focusedLabelColor = Color(0xFF6366F1),
    unfocusedLabelColor = Color(0xFF64748B),
    disabledLabelColor = Color(0xFF94A3B8),
    focusedLeadingIconColor = Color(0xFF6366F1),
    unfocusedLeadingIconColor = Color(0xFF64748B),
    disabledLeadingIconColor = Color(0xFF94A3B8),
    focusedTrailingIconColor = Color(0xFF475569),
    unfocusedTrailingIconColor = Color(0xFF64748B),
    disabledTrailingIconColor = Color(0xFF94A3B8),
    focusedPlaceholderColor = Color(0xFF94A3B8),
    unfocusedPlaceholderColor = Color(0xFF94A3B8),
    cursorColor = Color(0xFF6366F1)
)

/**
 * Animated Pill Tab Switcher between "Sign In" and "Sign Up", styled with light frosted capsule dock.
 */
@Composable
private fun SmoothAuthTabSwitcher(
    isRegisterTab: Boolean,
    onTabSelected: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .shadow(2.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x10000000))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            val tabWidth = maxWidth / 2

            val indicatorOffset by animateDpAsState(
                targetValue = if (isRegisterTab) tabWidth else 0.dp,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                label = "tab_offset"
            )

            // Sliding Active Capsule with radiant purple-indigo gradient and specular border
            Box(
                modifier = Modifier
                    .width(tabWidth)
                    .fillMaxHeight()
                    .offset(x = indicatorOffset)
                    .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x306366F1), spotColor = Color(0x306366F1))
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF7C3AED),
                                Color(0xFF6366F1)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.60f),
                                Color.White.copy(alpha = 0.20f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
            )

            Row(modifier = Modifier.fillMaxSize()) {
                val signInColor by animateColorAsState(
                    targetValue = if (!isRegisterTab) Color.White else Color(0xFF64748B),
                    label = "signInTabColor"
                )
                val signUpColor by animateColorAsState(
                    targetValue = if (isRegisterTab) Color.White else Color(0xFF64748B),
                    label = "signUpTabColor"
                )

                // Sign In Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTabSelected(false) }
                        .testTag("tab_switch_signin"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign In",
                        fontSize = 14.sp,
                        fontWeight = if (!isRegisterTab) FontWeight.Bold else FontWeight.Medium,
                        color = signInColor
                    )
                }

                // Sign Up Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTabSelected(true) }
                        .testTag("tab_switch_signup"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign Up",
                        fontSize = 14.sp,
                        fontWeight = if (isRegisterTab) FontWeight.Bold else FontWeight.Medium,
                        color = signUpColor
                    )
                }
            }
        }
    }
}

/**
 * Sign In Form with Email and Password using semi-transparent frosted styling.
 */
@Composable
private fun SignInForm(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isLoading: Boolean,
    onSignIn: () -> Unit,
    onSwitchToRegister: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Email Field with frosted styling
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email Address") },
            enabled = !isLoading,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = null,
                    tint = Color(0xFFA78BFA)
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                autoCorrectEnabled = false
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            shape = RoundedCornerShape(16.dp),
            colors = frostedInputColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_signin_email")
        )

        // Password Field with frosted styling
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            enabled = !isLoading,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = Color(0xFFA78BFA)
                )
            },
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = Color(0xFF64748B)
                    )
                }
            },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                autoCorrectEnabled = false
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onSignIn()
                }
            ),
            shape = RoundedCornerShape(16.dp),
            colors = frostedInputColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_signin_password")
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Sign In Action Button (frosted glass button with gradient & specular highlight)
        SmoothActionButton(
            text = "Sign In",
            icon = Icons.AutoMirrored.Outlined.Login,
            isLoading = isLoading,
            onClick = onSignIn,
            modifier = Modifier.fillMaxWidth(),
            testTag = "btn_submit_signin"
        )

        // Switch to Sign Up text link
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Don't have an account? ",
                fontSize = 13.5.sp,
                color = Color(0xFF64748B)
            )
            Text(
                text = "Sign Up",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6366F1),
                modifier = Modifier
                    .clickable { onSwitchToRegister() }
                    .padding(4.dp)
                    .testTag("link_switch_signup")
            )
        }
    }
}

/**
 * Sign Up Form with full academic profile creation and step-by-step transitional animations.
 */
@Composable
private fun SignUpForm(
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    college: String,
    onCollegeChange: (String) -> Unit,
    collegeDropdown: Boolean,
    onToggleCollegeDropdown: (Boolean) -> Unit,
    availableTechs: List<Technology>,
    tech: Technology,
    onTechChange: (Technology) -> Unit,
    techDropdown: Boolean,
    onToggleTechDropdown: (Boolean) -> Unit,
    sem: Semester,
    onSemChange: (Semester) -> Unit,
    semDropdown: Boolean,
    onToggleSemDropdown: (Boolean) -> Unit,
    isLoading: Boolean,
    onSignUp: () -> Unit,
    onSwitchToSignIn: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var currentStep by remember { mutableIntStateOf(1) } // 1: Personal Info, 2: Academic Info
    var step1Error by remember { mutableStateOf<String?>(null) }

    // Validation for Step 1 before transitioning to Step 2
    fun validateStep1(): Boolean {
        if (name.isBlank()) {
            step1Error = "Please enter your full student name."
            return false
        }
        if (email.isBlank() || (!email.contains("@") && !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches())) {
            step1Error = "Please enter a valid email address."
            return false
        }
        if (password.length < 6) {
            step1Error = "Password must be at least 6 characters long."
            return false
        }
        if (phone.isBlank()) {
            step1Error = "Please enter your mobile phone number."
            return false
        }
        step1Error = null
        return true
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Step Progress Indicator Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Step 1 Header Pill
                Surface(
                    onClick = {
                        focusManager.clearFocus()
                        currentStep = 1
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (currentStep == 1) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                    border = BorderStroke(
                        1.dp,
                        if (currentStep == 1) Color(0xFF6366F1) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (currentStep == 1) Color(0xFF6366F1) else Color(0xFFCBD5E1),
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "1",
                                    color = if (currentStep == 1) Color.White else Color(0xFF475569),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Account Info",
                            fontSize = 12.sp,
                            fontWeight = if (currentStep == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (currentStep == 1) Color(0xFF4338CA) else Color(0xFF64748B)
                        )
                    }
                }

                // Step 2 Header Pill
                Surface(
                    onClick = {
                        if (validateStep1()) {
                            focusManager.clearFocus()
                            currentStep = 2
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (currentStep == 2) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                    border = BorderStroke(
                        1.dp,
                        if (currentStep == 2) Color(0xFF6366F1) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (currentStep == 2) Color(0xFF6366F1) else Color(0xFFCBD5E1),
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "2",
                                    color = if (currentStep == 2) Color.White else Color(0xFF475569),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Campus & Tech",
                            fontSize = 12.sp,
                            fontWeight = if (currentStep == 2) FontWeight.Bold else FontWeight.Medium,
                            color = if (currentStep == 2) Color(0xFF4338CA) else Color(0xFF64748B)
                        )
                    }
                }
            }

            // Animated Linear Progress bar
            val progressAnim by animateFloatAsState(
                targetValue = if (currentStep == 1) 0.5f else 1.0f,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "step_progress"
            )
            LinearProgressIndicator(
                progress = { progressAnim },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .clip(CircleShape),
                color = Color(0xFF6366F1),
                trackColor = Color(0xFFE2E8F0)
            )
        }

        // Inline Step 1 Error Banner
        AnimatedVisibility(
            visible = step1Error != null && currentStep == 1,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFEF2F2),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = step1Error ?: "",
                        color = Color(0xFFB91C1C),
                        fontSize = 11.5.sp
                    )
                }
            }
        }

        // Step-by-Step Transitional Animated Content Container
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn())
                        .togetherWith(slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it } + fadeOut())
                } else {
                    (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it } + fadeIn())
                        .togetherWith(slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeOut())
                }
            },
            label = "signup_step_wizard"
        ) { step ->
            if (step == 1) {
                // STEP 1: Account Credentials & Contact
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Full Name Field with frosted styling
                    var nameFocused by remember { mutableStateOf(false) }
                    val nameScale by animateFloatAsState(
                        targetValue = if (nameFocused) 1.012f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "name_scale"
                    )
                    val nameElevation by animateDpAsState(
                        targetValue = if (nameFocused) 3.dp else 0.dp,
                        label = "name_elevation"
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            onNameChange(it)
                            step1Error = null
                        },
                        label = { Text("Full Name") },
                        enabled = !isLoading,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = if (nameFocused) Color(0xFF6366F1) else Color(0xFF64748B)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = frostedInputColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(nameScale)
                            .shadow(nameElevation, RoundedCornerShape(16.dp), ambientColor = Color(0x186366F1), spotColor = Color(0x186366F1))
                            .onFocusChanged { nameFocused = it.isFocused }
                            .testTag("input_reg_name")
                    )

                    // Email Field with frosted styling
                    var emailFocused by remember { mutableStateOf(false) }
                    val emailScale by animateFloatAsState(
                        targetValue = if (emailFocused) 1.012f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "email_scale"
                    )
                    val emailElevation by animateDpAsState(
                        targetValue = if (emailFocused) 3.dp else 0.dp,
                        label = "email_elevation"
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            onEmailChange(it)
                            step1Error = null
                        },
                        label = { Text("Email Address") },
                        enabled = !isLoading,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Email,
                                contentDescription = null,
                                tint = if (emailFocused) Color(0xFF6366F1) else Color(0xFF64748B)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                            autoCorrectEnabled = false
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = frostedInputColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(emailScale)
                            .shadow(emailElevation, RoundedCornerShape(16.dp), ambientColor = Color(0x186366F1), spotColor = Color(0x186366F1))
                            .onFocusChanged { emailFocused = it.isFocused }
                            .testTag("input_reg_email")
                    )

                    // Password Field with frosted styling
                    var passwordFocused by remember { mutableStateOf(false) }
                    val passwordScale by animateFloatAsState(
                        targetValue = if (passwordFocused) 1.012f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "password_scale"
                    )
                    val passwordElevation by animateDpAsState(
                        targetValue = if (passwordFocused) 3.dp else 0.dp,
                        label = "password_elevation"
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            onPasswordChange(it)
                            step1Error = null
                        },
                        label = { Text("Password (at least 6 characters)") },
                        supportingText = {
                            Text(
                                text = "Minimum 6 characters required",
                                fontSize = 11.sp,
                                color = if (password.isNotEmpty() && password.length < 6) Color(0xFFDC2626) else Color(0xFF64748B)
                            )
                        },
                        enabled = !isLoading,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = if (passwordFocused) Color(0xFF6366F1) else Color(0xFF64748B)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = onTogglePasswordVisibility) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle password",
                                    tint = Color(0xFF64748B)
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                            autoCorrectEnabled = false
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = frostedInputColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(passwordScale)
                            .shadow(passwordElevation, RoundedCornerShape(16.dp), ambientColor = Color(0x186366F1), spotColor = Color(0x186366F1))
                            .onFocusChanged { passwordFocused = it.isFocused }
                            .testTag("input_reg_password")
                    )

                    // Phone Number Field with frosted styling
                    var phoneFocused by remember { mutableStateOf(false) }
                    val phoneScale by animateFloatAsState(
                        targetValue = if (phoneFocused) 1.012f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "phone_scale"
                    )
                    val phoneElevation by animateDpAsState(
                        targetValue = if (phoneFocused) 3.dp else 0.dp,
                        label = "phone_elevation"
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            onPhoneChange(it)
                            step1Error = null
                        },
                        label = { Text("Mobile Phone Number") },
                        enabled = !isLoading,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Phone,
                                contentDescription = null,
                                tint = if (phoneFocused) Color(0xFF6366F1) else Color(0xFF64748B)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (validateStep1()) {
                                    focusManager.clearFocus()
                                    currentStep = 2
                                }
                            }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = frostedInputColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(phoneScale)
                            .shadow(phoneElevation, RoundedCornerShape(16.dp), ambientColor = Color(0x186366F1), spotColor = Color(0x186366F1))
                            .onFocusChanged { phoneFocused = it.isFocused }
                            .testTag("input_reg_phone")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Step 1 Next Button: Transition smoothly to Step 2
                    SmoothActionButton(
                        text = "Continue to Academic Profile →",
                        icon = Icons.Outlined.ArrowForward,
                        isLoading = false,
                        onClick = {
                            if (validateStep1()) {
                                focusManager.clearFocus()
                                currentStep = 2
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "btn_step1_next"
                    )
                }
            } else {
                // STEP 2: Campus, Technology & Semester Selection
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // College Dropdown Selector with crisp light-mode frosted styling
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = college,
                            onValueChange = {},
                            enabled = false,
                            readOnly = true,
                            label = { Text("Polytechnic / Institute") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.School,
                                    contentDescription = null,
                                    tint = Color(0xFF6366F1)
                                )
                            },
                            trailingIcon = {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown", tint = Color(0xFF64748B))
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = frostedInputColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    focusManager.clearFocus()
                                    onToggleCollegeDropdown(!collegeDropdown)
                                }
                                .testTag("select_reg_college")
                        )
                        DropdownMenu(
                            expanded = collegeDropdown,
                            onDismissRequest = { onToggleCollegeDropdown(false) },
                            properties = androidx.compose.ui.window.PopupProperties(
                                focusable = false,
                                dismissOnBackPress = true,
                                dismissOnClickOutside = true
                            ),
                            modifier = Modifier
                                .widthIn(min = 280.dp, max = 340.dp)
                                .heightIn(max = 280.dp)
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x15000000))
                        ) {
                            Text(
                                text = "Select Polytechnic / Institute",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                            CollegeDirectory.ALL_COLLEGES.forEach { colName ->
                                val isSelected = colName == college
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = colName,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF1E293B),
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color(0xFF4F46E5),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        onToggleCollegeDropdown(false)
                                        onCollegeChange(colName)
                                    },
                                    modifier = Modifier.background(
                                        if (isSelected) Color(0xFFEEF2FF) else Color.Transparent
                                    )
                                )
                            }
                        }
                    }

                    // Technology & Semester Row with frosted styling
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Technology Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = tech.shortName,
                                onValueChange = {},
                                enabled = false,
                                readOnly = true,
                                label = { Text("Technology") },
                                trailingIcon = {
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color(0xFF64748B))
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = frostedInputColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        focusManager.clearFocus()
                                        onToggleTechDropdown(!techDropdown)
                                    }
                                    .testTag("select_reg_tech")
                            )
                            DropdownMenu(
                                expanded = techDropdown,
                                onDismissRequest = { onToggleTechDropdown(false) },
                                properties = androidx.compose.ui.window.PopupProperties(
                                    focusable = false,
                                    dismissOnBackPress = true,
                                    dismissOnClickOutside = true
                                ),
                                modifier = Modifier
                                    .widthIn(min = 220.dp, max = 300.dp)
                                    .heightIn(max = 280.dp)
                                    .background(Color.White, RoundedCornerShape(16.dp))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                    .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x15000000))
                            ) {
                                Text(
                                    text = "Select Technology",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                                availableTechs.forEach { t ->
                                    val isSelected = t == tech
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = t.displayName,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF1E293B)
                                                    )
                                                    Text(
                                                        text = t.shortName,
                                                        fontSize = 11.sp,
                                                        color = if (isSelected) Color(0xFF6366F1) else Color(0xFF64748B)
                                                    )
                                                }
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Check,
                                                        contentDescription = "Selected",
                                                        tint = Color(0xFF4F46E5),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            onToggleTechDropdown(false)
                                            onTechChange(t)
                                        },
                                        modifier = Modifier.background(
                                            if (isSelected) Color(0xFFEEF2FF) else Color.Transparent
                                        )
                                    )
                                }
                            }
                        }

                        // Semester Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = sem.shortName,
                                onValueChange = {},
                                enabled = false,
                                readOnly = true,
                                label = { Text("Semester") },
                                trailingIcon = {
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color(0xFF64748B))
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = frostedInputColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        focusManager.clearFocus()
                                        onToggleSemDropdown(!semDropdown)
                                    }
                                    .testTag("select_reg_sem")
                            )
                            DropdownMenu(
                                expanded = semDropdown,
                                onDismissRequest = { onToggleSemDropdown(false) },
                                properties = androidx.compose.ui.window.PopupProperties(
                                    focusable = false,
                                    dismissOnBackPress = true,
                                    dismissOnClickOutside = true
                                ),
                                modifier = Modifier
                                    .widthIn(min = 190.dp, max = 260.dp)
                                    .heightIn(max = 280.dp)
                                    .background(Color.White, RoundedCornerShape(16.dp))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                    .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x15000000))
                            ) {
                                Text(
                                    text = "Select Semester",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                                Semester.entries.forEach { s ->
                                    val isSelected = s == sem
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = s.displayName,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF1E293B)
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Check,
                                                        contentDescription = "Selected",
                                                        tint = Color(0xFF4F46E5),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            onToggleSemDropdown(false)
                                            onSemChange(s)
                                        },
                                        modifier = Modifier.background(
                                            if (isSelected) Color(0xFFEEF2FF) else Color.Transparent
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Final Create Account Action Button (frosted glass button with gradient)
                    SmoothActionButton(
                        text = "Create Account",
                        icon = Icons.Outlined.Check,
                        isLoading = isLoading,
                        onClick = onSignUp,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "btn_submit_signup"
                    )

                    // Back to Step 1 Button with frosted glass surface
                    Surface(
                        onClick = {
                            focusManager.clearFocus()
                            currentStep = 1
                        },
                        shape = RoundedCornerShape(26.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_step2_back")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null,
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Back to Personal Details",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }
            }
        }

        // Switch to Sign In text link
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account? ",
                fontSize = 13.5.sp,
                color = Color(0xFF64748B)
            )
            Text(
                text = "Sign In",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6366F1),
                modifier = Modifier
                    .clickable { onSwitchToSignIn() }
                    .padding(4.dp)
                    .testTag("link_switch_signin")
            )
        }
    }
}

/**
 * Reusable Action Button with rich frosted styling,
 * top specular reflection sheen, and spring tactile press physics.
 */
@Composable
private fun SmoothActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(
        Color(0xFF7C3AED),
        Color(0xFF6366F1)
    ),
    textColor: Color = Color.White,
    testTag: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "btn_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .height(52.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = Color(0x306366F1),
                spotColor = Color(0x306366F1)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.horizontalGradient(gradientColors)
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.85f),
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.55f)
                    )
                ),
                shape = RoundedCornerShape(26.dp)
            )
            .clickable(
                enabled = !isLoading,
                interactionSource = interactionSource,
                indication = ripple(color = Color.White.copy(alpha = 0.35f))
            ) { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        // Specular top highlight sheen
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(4.dp)
                .align(Alignment.TopCenter)
                .padding(top = 2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.40f), Color.Transparent)
                    )
                )
        )

        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = textColor,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Please wait...",
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = text,
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}



/**
 * Beautiful Gmail OTP Verification Component with real-time 6-digit PIN input,
 * animated focus, live countdown, simulated email reception card, and error handling.
 */
@Composable
private fun GmailOtpVerificationCard(
    email: String,
    maskedEmail: String,
    otpCode: String,
    onOtpChange: (String) -> Unit,
    sentCodeForTest: String?,
    countdown: Int,
    isLoading: Boolean,
    errorMessage: String?,
    isSuccess: Boolean,
    onAutoFill: () -> Unit,
    onResendOtp: () -> Unit,
    onVerify: () -> Unit,
    onBackToSignUp: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(250)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Icon Badge with Gmail theme
        Surface(
            shape = CircleShape,
            color = Color(0xFFFEE2E2),
            border = BorderStroke(1.5.dp, Color(0xFFFCA5A5)),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.MarkEmailRead,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Gmail OTP Verification",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Enter the 6-digit verification code sent to your Gmail:",
            fontSize = 13.sp,
            color = Color(0xFF475569),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Email pill with edit icon
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFF1F5F9),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = null,
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = email,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit email",
                    tint = Color(0xFF64748B),
                    modifier = Modifier
                        .size(14.dp)
                        .clickable { onBackToSignUp() }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Simulated Incoming Gmail Notification Box
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEA4335),
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "M",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Google Mail • Inbox",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )
                    }
                    Text(
                        text = "Received Just Now",
                        fontSize = 10.sp,
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Subject: Polytechnic Book Exchange Verification Code",
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OTP: ${sentCodeForTest ?: "******"}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4F46E5)
                    )
                    if (sentCodeForTest != null) {
                        Surface(
                            onClick = onAutoFill,
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEEF2FF),
                            border = BorderStroke(1.dp, Color(0xFFC7D2FE))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoFixHigh,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Auto Fill",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4F46E5)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 6-Digit OTP Box Display with underlying input
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // BasicTextField capturing numeric keystrokes with full touch bounds
            BasicTextField(
                value = otpCode,
                onValueChange = onOtpChange,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .matchParentSize()
                    .alpha(0f)
            )

            // Visual 6 digit boxes
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 6) {
                    val digit = otpCode.getOrNull(i)?.toString() ?: ""
                    val isCurrent = otpCode.length == i
                    val isFilled = digit.isNotEmpty()

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            isSuccess -> Color(0xFFDCFCE7)
                            isCurrent -> Color.White
                            isFilled -> Color.White
                            else -> Color(0xFFF8FAFC)
                        },
                        border = BorderStroke(
                            width = if (isCurrent) 2.dp else 1.2.dp,
                            color = when {
                                isSuccess -> Color(0xFF22C55E)
                                isCurrent -> Color(0xFF6366F1)
                                isFilled -> Color(0xFF94A3B8)
                                else -> Color(0xFFCBD5E1)
                            }
                        ),
                        modifier = Modifier
                            .size(44.dp, 52.dp)
                            .clickable {
                                try {
                                    focusRequester.requestFocus()
                                } catch (_: Exception) {}
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (digit.isNotEmpty()) {
                                Text(
                                    text = digit,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            } else if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(18.dp)
                                        .background(Color(0xFF6366F1), RoundedCornerShape(1.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFFCBD5E1), CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Resend Timer & Action
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (countdown > 0) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Resend code in: 00:${if (countdown < 10) "0$countdown" else "$countdown"}",
                    fontSize = 12.5.sp,
                    color = Color(0xFF475569)
                )
            } else {
                TextButton(
                    onClick = onResendOtp,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Resend OTP",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6366F1)
                    )
                }
            }
        }

        // Error message banner
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFEF2F2),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFB91C1C),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Success Celebration Banner
        AnimatedVisibility(
            visible = isSuccess,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFDCFCE7),
                border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OTP verified successfully! Logging you in...",
                        color = Color(0xFF15803D),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Confirm & Log In Button with Frosted Gradient Action styling
        SmoothActionButton(
            text = "Verify OTP & Sign In",
            icon = Icons.Outlined.VerifiedUser,
            isLoading = isLoading,
            onClick = {
                if (!isLoading && !isSuccess && otpCode.length == 6) {
                    onVerify()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            gradientColors = if (otpCode.length == 6 && !isSuccess) {
                listOf(Color(0xFF7C3AED), Color(0xFF6366F1), Color(0xFF4F46E5))
            } else {
                listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1))
            },
            textColor = if (otpCode.length == 6 && !isSuccess) Color.White else Color(0xFF64748B),
            testTag = "btn_verify_otp"
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Back to Sign Up Button
        TextButton(
            onClick = onBackToSignUp,
            enabled = !isLoading
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Back to Sign Up",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

