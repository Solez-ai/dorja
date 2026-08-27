package com.example.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DorjaApp
import com.example.R
import com.example.ui.components.DorjaButton
import com.example.ui.components.DorjaCard
import com.example.ui.theme.DorjaColors

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit
) {
    val repository = DorjaApp.instance.repository

    var phone by remember { mutableStateOf("+880 1712-345678") }
    var password by remember { mutableStateOf("12345678") }
    var isSignUpMode by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("Rahim Ahmed") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.Paper50)
            .testTag("auth_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(36.dp))

                // DORJA Logo Header
                Image(
                    painter = painterResource(id = R.drawable.ic_dorja_logo),
                    contentDescription = "DORJA Logo",
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "DORJA",
                    style = MaterialTheme.typography.headlineLarge,
                    color = DorjaColors.Ink950,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Property Trust Platform",
                    style = MaterialTheme.typography.bodySmall,
                    color = DorjaColors.Gray700
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Auth Form Box
                DorjaCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = DorjaColors.White
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = if (isSignUpMode) "Create Account" else "Sign In",
                            style = MaterialTheme.typography.titleMedium,
                            color = DorjaColors.Ink950,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (isSignUpMode) {
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Full Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_name_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DorjaColors.Jol600,
                                    unfocusedBorderColor = DorjaColors.Sand300
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_phone_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DorjaColors.Jol600,
                                unfocusedBorderColor = DorjaColors.Sand300
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_password_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DorjaColors.Jol600,
                                unfocusedBorderColor = DorjaColors.Sand300
                            )
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        DorjaButton(
                            text = if (isSignUpMode) "Register Account" else "Sign In",
                            onClick = {
                                // Default to active session
                                onLoginSuccess()
                            },
                            testTag = "auth_submit_button"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { isSignUpMode = !isSignUpMode },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = if (isSignUpMode) "Already have an account? Sign In" else "New to DORJA? Create Account",
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Jol600
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Quick Switch Demo Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = DorjaColors.Sand300)
                        Text(
                            text = "  1-TAP DEMO SWITCH  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500,
                            fontFamily = FontFamily.Monospace
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = DorjaColors.Sand300)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Seller Quick Login
                    DorjaButton(
                        text = "Login as Seller: Shovro",
                        onClick = {
                            repository.switchUser("u1")
                            onLoginSuccess()
                        },
                        icon = Icons.Default.Storefront,
                        containerColor = DorjaColors.Jol600,
                        testTag = "demo_login_seller"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Buyer Quick Login
                    DorjaButton(
                        text = "Login as Buyer: Samin Yeasar",
                        onClick = {
                            repository.switchUser("u2")
                            onLoginSuccess()
                        },
                        icon = Icons.Default.Person,
                        containerColor = DorjaColors.Ink950,
                        testTag = "demo_login_buyer"
                    )
                }
            }

            // Bottom branding
            Row(
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = DorjaColors.Jol600,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "DORJA • Verified Bangladeshi Real Estate",
                    style = MaterialTheme.typography.labelSmall,
                    color = DorjaColors.Gray500
                )
            }
        }
    }
}
