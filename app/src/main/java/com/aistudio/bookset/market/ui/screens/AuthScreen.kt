package com.aistudio.bookset.market.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.bookset.market.ui.components.NeumorphicButton
import com.aistudio.bookset.market.ui.components.NeumorphicCard
import com.aistudio.bookset.market.ui.components.NeumorphicRecessedBox
import com.aistudio.bookset.market.ui.theme.AccentCoral
import com.aistudio.bookset.market.ui.theme.AccentGreen
import com.aistudio.bookset.market.ui.theme.InputBackground
import com.aistudio.bookset.market.ui.theme.InputBorder
import com.aistudio.bookset.market.ui.theme.NeumorphicBackground
import com.aistudio.bookset.market.ui.theme.NeumorphicDarkShadow
import com.aistudio.bookset.market.ui.theme.NeumorphicLightShadow
import com.aistudio.bookset.market.ui.theme.NeumorphicSurface
import com.aistudio.bookset.market.ui.theme.PrimaryBlue
import com.aistudio.bookset.market.ui.theme.PrimaryBlueVariant
import com.aistudio.bookset.market.ui.theme.TextMuted
import com.aistudio.bookset.market.ui.theme.TextPrimary
import com.aistudio.bookset.market.ui.theme.TextSecondary

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var isRegisterMode by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onAuthSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeumorphicBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // App Brand Header with Neumorphic Floating Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        ambientColor = NeumorphicDarkShadow.copy(alpha = 0.5f),
                        spotColor = NeumorphicDarkShadow.copy(alpha = 0.6f)
                    )
                    .background(NeumorphicSurface, CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "BookSet Market Logo",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "BookSet Market",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Buy, sell, and discover curated book sets",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Neumorphic Pill Tab Switcher
            NeumorphicTabSwitcher(
                isRegisterMode = isRegisterMode,
                onTabSelected = { register ->
                    isRegisterMode = register
                    viewModel.resetState()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Neumorphic Card Containing Recessed Fields
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRegisterMode) "Create an Account" else "Welcome Back",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 18.dp)
                    )

                    // Error Message Banner
                    AnimatedVisibility(visible = uiState is AuthUiState.Error) {
                        val errorMsg = (uiState as? AuthUiState.Error)?.message ?: ""
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentCoral.copy(alpha = 0.12f))
                                .border(1.dp, AccentCoral.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = errorMsg,
                                color = AccentCoral,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Registration Specific Fields
                    if (isRegisterMode) {
                        // Full Name Recessed Box
                        NeumorphicRecessedBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                        ) {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Full Name") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = "Name Icon", tint = PrimaryBlue)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                colors = neumorphicTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("full_name_input")
                            )
                        }

                        // Phone Number Recessed Box
                        NeumorphicRecessedBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                        ) {
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text("Phone Number") },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = "Phone Icon", tint = PrimaryBlue)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                colors = neumorphicTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("phone_input")
                            )
                        }
                    }

                    // Email Address Recessed Box
                    NeumorphicRecessedBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = "Email Icon", tint = PrimaryBlue)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            colors = neumorphicTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_input")
                        )
                    }

                    // Password Recessed Box
                    NeumorphicRecessedBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    ) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = "Lock Icon", tint = PrimaryBlue)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Hide Password" else "Show Password",
                                        tint = TextMuted
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                if (isRegisterMode) {
                                    viewModel.signUpWithEmail(fullName, email, phoneNumber, password)
                                } else {
                                    viewModel.signInWithEmail(email, password)
                                }
                            }),
                            colors = neumorphicTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input")
                        )
                    }

                    // Submit Button with Soft Neumorphic Dual Shadow
                    NeumorphicButton(
                        onClick = {
                            focusManager.clearFocus()
                            if (isRegisterMode) {
                                viewModel.signUpWithEmail(fullName, email, phoneNumber, password)
                            } else {
                                viewModel.signInWithEmail(email, password)
                            }
                        },
                        enabled = uiState !is AuthUiState.Loading,
                        backgroundColor = PrimaryBlue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("auth_submit_button")
                    ) {
                        if (uiState is AuthUiState.Loading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = if (isRegisterMode) "Create Account" else "Sign In",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Direct Admin Entrance (NO PIN / PASSWORD REQUIRED)
            // As specifically requested: "Cancel the : Private Admin PIN / Password Verification So can the admin directly login the admin panel"
            NeumorphicButton(
                onClick = {
                    viewModel.directAdminLogin()
                },
                backgroundColor = NeumorphicSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("direct_admin_login_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin Icon",
                        tint = AccentGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Direct Admin Access (No PIN Required)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun NeumorphicTabSwitcher(
    isRegisterMode: Boolean,
    onTabSelected: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = NeumorphicDarkShadow.copy(alpha = 0.35f),
                spotColor = NeumorphicDarkShadow.copy(alpha = 0.5f)
            )
            .background(InputBackground, RoundedCornerShape(24.dp))
            .border(1.dp, InputBorder.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Sign In Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .then(
                        if (!isRegisterMode) {
                            Modifier
                                .shadow(2.dp, RoundedCornerShape(20.dp))
                                .background(NeumorphicSurface)
                                .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                        } else {
                            Modifier
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple()
                    ) { onTabSelected(false) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign In",
                    fontWeight = if (!isRegisterMode) FontWeight.Bold else FontWeight.Medium,
                    color = if (!isRegisterMode) PrimaryBlue else TextSecondary,
                    fontSize = 14.sp
                )
            }

            // Register Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .then(
                        if (isRegisterMode) {
                            Modifier
                                .shadow(2.dp, RoundedCornerShape(20.dp))
                                .background(NeumorphicSurface)
                                .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                        } else {
                            Modifier
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple()
                    ) { onTabSelected(true) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Register",
                    fontWeight = if (isRegisterMode) FontWeight.Bold else FontWeight.Medium,
                    color = if (isRegisterMode) PrimaryBlue else TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun neumorphicTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    disabledBorderColor = Color.Transparent,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedLabelColor = PrimaryBlue,
    unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
