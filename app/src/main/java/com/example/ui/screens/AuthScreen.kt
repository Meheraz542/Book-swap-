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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
 * - Email & Password authentication.
 * - Prominent "Sign in with Google" button directly below Email/Password.
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

    // Pre-filled Sign In State for fast access
    var signInEmail by remember { mutableStateOf("mdmaheraz65@gmail.com") }
    var signInPassword by remember { mutableStateOf("polytechnic123") }
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

    LiquidGlassBackdrop(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 22.dp, vertical = 16.dp),
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
                            .shadow(2.dp, CircleShape)
                            .background(Color.White.copy(alpha = 0.90f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back to app",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // App Logo Emblem - Clean Circular Badge
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.95f),
                border = BorderStroke(1.5.dp, PrimaryBlue.copy(alpha = 0.35f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(76.dp)
                    .shadow(8.dp, CircleShape, ambientColor = PrimaryBlueGlow, spotColor = PrimaryBlueGlow)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.AutoStories,
                        contentDescription = "BookSet Logo",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // App Brand Name & Tagline
            Text(
                text = "BookSet",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                letterSpacing = 0.2.sp
            )

            Text(
                text = "Polytechnic Student Book Sharing",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryBlue,
                modifier = Modifier.padding(top = 2.dp)
            )

            Text(
                text = "Feni Polytechnic & Feni Computer Institute",
                fontSize = 11.5.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // =========================================================
            // AUTH CARD (Pill Switcher + Forms)
            // =========================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(24.dp),
                        backgroundColor = Color.White.copy(alpha = 0.92f),
                        borderBrush = GlassRimBrush,
                        borderWidth = 1.3.dp,
                        elevation = 8.dp,
                        shadowColor = SoftShadowColor,
                        innerShadowColor = Color(0x08000000),
                        innerHighlightColor = Color.White,
                        showTopSheen = true
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
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
                                    errorMessage = "অনুগ্রহ করে ৬-সংখ্যার ওটিপি কোডটি সম্পূর্ণ লিখুন।"
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

                    // Form Content Switcher
                    AnimatedContent(
                        targetState = isRegisterTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "authFormTransition"
                    ) { isRegister ->
                        if (!isRegister) {
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
                                onGoogleSignIn = {
                                    isLoading = true
                                    errorMessage = null
                                    viewModel.signInWithGoogle(
                                        context = context,
                                        college = regCollege,
                                        technology = regTech,
                                        semester = regSem,
                                        onAuthSuccess = {
                                            isLoading = false
                                            onAuthSuccess()
                                        }
                                    )
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
                                    if (regPassword.length < 4) {
                                        errorMessage = "Password must be at least 4 characters long."
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
        }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Animated Pill Tab Switcher between "Sign In" and "Sign Up".
 */
@Composable
private fun SmoothAuthTabSwitcher(
    isRegisterTab: Boolean,
    onTabSelected: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
        ) {
            val tabWidth = maxWidth / 2

            val indicatorOffset by animateDpAsState(
                targetValue = if (isRegisterTab) tabWidth else 0.dp,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                label = "tab_offset"
            )

            Box(
                modifier = Modifier
                    .width(tabWidth)
                    .fillMaxHeight()
                    .offset(x = indicatorOffset)
                    .shadow(3.dp, RoundedCornerShape(11.dp))
                    .background(Color.White, RoundedCornerShape(11.dp))
            )

            Row(modifier = Modifier.fillMaxSize()) {
                val signInColor by animateColorAsState(
                    targetValue = if (!isRegisterTab) PrimaryBlue else TextSecondary,
                    label = "signInTabColor"
                )
                val signUpColor by animateColorAsState(
                    targetValue = if (isRegisterTab) PrimaryBlue else TextSecondary,
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
 * Sign In Form with Email, Password, and "Sign in with Google" directly below.
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
    onGoogleSignIn: () -> Unit,
    onSwitchToRegister: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email Address") },
            placeholder = { Text("e.g. mdmaheraz65@gmail.com") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = null,
                    tint = PrimaryBlue
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                autoCorrectEnabled = false
            ),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFFAFAFA)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_signin_email")
        )

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = PrimaryBlue
                )
            },
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = TextSecondary
                    )
                }
            },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrectEnabled = false
            ),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFFAFAFA)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_signin_password")
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Sign In Action Button
        SmoothActionButton(
            text = "Sign In",
            icon = Icons.AutoMirrored.Outlined.Login,
            isLoading = isLoading,
            onClick = onSignIn,
            modifier = Modifier.fillMaxWidth(),
            testTag = "btn_submit_signin"
        )

        // Divider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
            Text(
                text = " OR ",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
        }

        // Direct Sign in with Google Button
        GoogleSignInButton(
            isLoading = isLoading,
            onClick = onGoogleSignIn,
            modifier = Modifier.fillMaxWidth()
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
                fontSize = 13.sp,
                color = TextSecondary
            )
            Text(
                text = "Sign Up",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue,
                modifier = Modifier
                    .clickable { onSwitchToRegister() }
                    .padding(4.dp)
                    .testTag("link_switch_signup")
            )
        }
    }
}

/**
 * Sign Up Form with full academic profile creation.
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Full Name Field
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Full Name") },
            placeholder = { Text("e.g. Maheraz Rahim") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = PrimaryBlue
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFFAFAFA)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_reg_name")
        )

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email Address") },
            placeholder = { Text("student@gmail.com") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = null,
                    tint = PrimaryBlue
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFFAFAFA)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_reg_email")
        )

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password (at least 4 characters)") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = PrimaryBlue
                )
            },
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = "Toggle password",
                        tint = TextSecondary
                    )
                }
            },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFFAFAFA)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_reg_password")
        )

        // Phone Number Field
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Mobile Phone Number") },
            placeholder = { Text("01712-XXXXXX") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Phone,
                    contentDescription = null,
                    tint = PrimaryBlue
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFFAFAFA)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_reg_phone")
        )

        // College Dropdown Selector
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
                        tint = PrimaryBlue
                    )
                },
                trailingIcon = {
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown", tint = TextSecondary)
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = TextPrimary,
                    disabledBorderColor = Color(0xFFE2E8F0),
                    disabledLabelColor = TextSecondary,
                    disabledContainerColor = Color.White,
                    disabledLeadingIconColor = PrimaryBlue,
                    disabledTrailingIconColor = TextSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { onToggleCollegeDropdown(!collegeDropdown) }
                    .testTag("select_reg_college")
            )
            DropdownMenu(
                expanded = collegeDropdown,
                onDismissRequest = { onToggleCollegeDropdown(false) },
                properties = androidx.compose.ui.window.PopupProperties(focusable = true),
                modifier = Modifier.background(Color.White)
            ) {
                CollegeDirectory.ALL_COLLEGES.forEach { colName ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = colName,
                                fontWeight = if (colName == college) FontWeight.Bold else FontWeight.Normal,
                                color = if (colName == college) PrimaryBlue else TextPrimary
                            )
                        },
                        onClick = {
                            onToggleCollegeDropdown(false)
                            onCollegeChange(colName)
                        }
                    )
                }
            }
        }

        // Technology & Semester Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Technology
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = tech.shortName,
                    onValueChange = {},
                    enabled = false,
                    readOnly = true,
                    label = { Text("Technology") },
                    trailingIcon = {
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = TextPrimary,
                        disabledBorderColor = Color(0xFFE2E8F0),
                        disabledLabelColor = TextSecondary,
                        disabledContainerColor = Color.White,
                        disabledTrailingIconColor = TextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { onToggleTechDropdown(!techDropdown) }
                        .testTag("select_reg_tech")
                )
                DropdownMenu(
                    expanded = techDropdown,
                    onDismissRequest = { onToggleTechDropdown(false) },
                    properties = androidx.compose.ui.window.PopupProperties(focusable = true),
                    modifier = Modifier.background(Color.White)
                ) {
                    availableTechs.forEach { t ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = t.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = if (t == tech) FontWeight.Bold else FontWeight.Normal,
                                    color = if (t == tech) PrimaryBlue else TextPrimary
                                )
                            },
                            onClick = {
                                onToggleTechDropdown(false)
                                onTechChange(t)
                            }
                        )
                    }
                }
            }

            // Semester
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = sem.shortName,
                    onValueChange = {},
                    enabled = false,
                    readOnly = true,
                    label = { Text("Semester") },
                    trailingIcon = {
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = TextPrimary,
                        disabledBorderColor = Color(0xFFE2E8F0),
                        disabledLabelColor = TextSecondary,
                        disabledContainerColor = Color.White,
                        disabledTrailingIconColor = TextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { onToggleSemDropdown(!semDropdown) }
                        .testTag("select_reg_sem")
                )
                DropdownMenu(
                    expanded = semDropdown,
                    onDismissRequest = { onToggleSemDropdown(false) },
                    properties = androidx.compose.ui.window.PopupProperties(focusable = true),
                    modifier = Modifier.background(Color.White)
                ) {
                    Semester.entries.forEach { s ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = s.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = if (s == sem) FontWeight.Bold else FontWeight.Normal,
                                    color = if (s == sem) PrimaryBlue else TextPrimary
                                )
                            },
                            onClick = {
                                onToggleSemDropdown(false)
                                onSemChange(s)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Create Account Action Button
        SmoothActionButton(
            text = "Create Account",
            icon = Icons.Outlined.Check,
            isLoading = isLoading,
            onClick = onSignUp,
            modifier = Modifier.fillMaxWidth(),
            testTag = "btn_submit_signup"
        )

        // Switch to Sign In text link
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account? ",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Text(
                text = "Sign In",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue,
                modifier = Modifier
                    .clickable { onSwitchToSignIn() }
                    .padding(4.dp)
                    .testTag("link_switch_signin")
            )
        }
    }
}

/**
 * Reusable Action Button with spring tactile press animation.
 */
@Composable
private fun SmoothActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "btn_scale"
    )

    Surface(
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(16.dp),
        color = PrimaryBlue,
        shadowElevation = 4.dp,
        interactionSource = interactionSource,
        modifier = modifier
            .scale(scale)
            .height(50.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp), ambientColor = PrimaryBlueGlow, spotColor = PrimaryBlueGlow)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Please wait...",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Clean Google Sign-In Button with tactile spring feedback.
 */
@Composable
private fun GoogleSignInButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "btn_google_signin"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "google_scale"
    )

    Surface(
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.2.dp, Color(0xFFCBD5E1)),
        shadowElevation = 2.dp,
        interactionSource = interactionSource,
        modifier = modifier
            .scale(scale)
            .height(48.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF4285F4),
                modifier = Modifier.size(22.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "G",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Sign in with Google",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
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
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "আপনার জিমেইলে পাঠানো ৬-সংখ্যার ওটিপি কোডটি দিন:",
            fontSize = 13.sp,
            color = TextSecondary,
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
                    tint = PrimaryBlue,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = email,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit email",
                    tint = TextMuted,
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
                            text = "Google Mail • ইনবক্স",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = "এখনই পাঠানো হয়েছে",
                        fontSize = 10.sp,
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "বিষয়: পলিটেকনিক বুক এক্সচেঞ্জ ভেরিফিকেশন কোড",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ওটিপি: ${sentCodeForTest ?: "******"}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    if (sentCodeForTest != null) {
                        Surface(
                            onClick = onAutoFill,
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryBlue.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoFixHigh,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "অটো-ফিল করুন",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
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
            // Invisible BasicTextField capturing numeric keystrokes
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
                            isSuccess -> Color(0xFFF0FDF4)
                            isCurrent -> Color.White
                            isFilled -> Color(0xFFF8FAFC)
                            else -> Color(0xFFF1F5F9)
                        },
                        border = BorderStroke(
                            width = if (isCurrent) 2.dp else 1.2.dp,
                            color = when {
                                isSuccess -> Color(0xFF22C55E)
                                isCurrent -> PrimaryBlue
                                isFilled -> Color(0xFF94A3B8)
                                else -> Color(0xFFCBD5E1)
                            }
                        ),
                        shadowElevation = if (isCurrent) 3.dp else 0.dp,
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
                                    color = TextPrimary
                                )
                            } else if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(18.dp)
                                        .background(PrimaryBlue, RoundedCornerShape(1.dp))
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
                    tint = TextMuted,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "কোড পুনরায় পাঠান: 00:${if (countdown < 10) "0$countdown" else "$countdown"}",
                    fontSize = 12.5.sp,
                    color = TextSecondary
                )
            } else {
                TextButton(
                    onClick = onResendOtp,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "পুনরায় কোড পাঠান (Resend OTP)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
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
                color = Color(0xFFF0FDF4),
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
                        text = "ওটিপি সফলভাবে যাচাই হয়েছে! লগইন করা হচ্ছে...",
                        color = Color(0xFF15803D),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Confirm & Log In Button
        Surface(
            onClick = onVerify,
            enabled = !isLoading && !isSuccess && otpCode.length == 6,
            shape = RoundedCornerShape(14.dp),
            color = if (otpCode.length == 6) PrimaryBlue else Color(0xFF94A3B8),
            shadowElevation = if (otpCode.length == 6) 3.dp else 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_verify_otp")
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "যাচাই করা হচ্ছে...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.VerifiedUser,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ওটিপি নিশ্চিত করুন এবং লগইন করুন",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Back to Sign Up Button
        TextButton(
            onClick = onBackToSignUp,
            enabled = !isLoading
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "সাইন আপ ফর্মে ফিরে যান",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
    }
}

