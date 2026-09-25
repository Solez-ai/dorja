package com.example.ui.auth

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
import androidx.compose.material.icons.filled.AdminPanelSettings
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DorjaApp
import com.example.ui.i18n.L
import com.example.ui.i18n.Lf
import com.example.ui.components.CountryPicker
import com.example.ui.components.DorjaButton
import com.example.ui.components.DorjaChip
import com.example.ui.components.DorjaLogo
import com.example.ui.components.DorjaCard
import com.example.ui.theme.DorjaColors
import com.example.ui.theme.LocalDarkTheme
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    startInSignUp: Boolean = false,
    onLoginSuccess: () -> Unit
) {
    val repository = DorjaApp.instance.repository
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var isSignUpMode by remember { mutableStateOf(startInSignUp) }
    var displayName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("BUYER") }
    var countryCode by remember {
        mutableStateOf(
            if (com.example.ui.i18n.LocaleSettings.isLanguagePinned()) com.example.ui.i18n.LocaleSettings.countryCode.value
            else "BD"
        )
    }
    var errorMessage by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    val credentialName = com.example.data.country.CountryRegistry.identityCredential(countryCode).shortName

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
                DorjaLogo(
                    modifier = Modifier.size(72.dp),
                    contentDescription = "DORJA Logo"
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
                    text = L("app_tagline"),
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
                            text = if (isSignUpMode) L("auth_create_account") else L("auth_sign_in"),
                            style = MaterialTheme.typography.titleMedium,
                            color = DorjaColors.Ink950,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (isSignUpMode) {
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text(L("auth_name")) },
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

                            Text(
                                text = L("auth_joining_as"),
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.Gray700
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DorjaChip(
                                    selected = role == "BUYER",
                                    label = L("account_role_buyer"),
                                    onClick = { role = "BUYER" },
                                    modifier = Modifier.testTag("auth_role_buyer")
                                )
                                DorjaChip(
                                    selected = role == "SELLER",
                                    label = L("account_role_host"),
                                    onClick = { role = "SELLER" },
                                    modifier = Modifier.testTag("auth_role_seller")
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text(L("auth_phone")) },
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
                            label = { Text(L("auth_password")) },
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
                        Spacer(modifier = Modifier.height(12.dp))

                        CountryPicker(
                            selected = countryCode,
                            onSelect = { code ->
                                countryCode = code
                                com.example.ui.i18n.LocaleSettings.applyCountry(context, code)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (isSignUpMode) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = Lf("auth_signup_verify_note_fmt", credentialName),
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray700
                            )
                        }

                        if (errorMessage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DorjaColors.ErrorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DorjaColors.Error,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        DorjaButton(
                            text = if (busy) L("common_loading") else if (isSignUpMode) L("auth_register_cta") else L("auth_sign_in"),
                            enabled = !busy,
                            onClick = {
                                busy = true
                                errorMessage = ""
                                scope.launch {
                                    val error = if (isSignUpMode) {
                                        repository.signUp(displayName, phone, password, role, countryCode)
                                    } else {
                                        repository.signIn(phone, password)
                                    }
                                    busy = false
                                    if (error == null) {
                                        onLoginSuccess()
                                    } else {
                                        errorMessage = error
                                    }
                                }
                            },
                            testTag = "auth_submit_button"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = {
                                errorMessage = ""
                                isSignUpMode = !isSignUpMode
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = if (isSignUpMode) L("auth_have_account") else L("auth_new_here"),
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Jol600
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // First-run bootstrap: the device's single admin account is
                // created here. The button disappears once an admin exists.
                Column(modifier = Modifier.fillMaxWidth()) {
                    var adminExists by remember { mutableStateOf<Boolean?>(null) }
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        adminExists = repository.hasAdmin()
                    }
                    if (adminExists == false) {
                        HorizontalDivider(color = DorjaColors.Sand300)
                        Spacer(modifier = Modifier.height(14.dp))
                        DorjaButton(
                            text = L("auth_create_admin_cta"),
                            onClick = {
                                busy = true
                                errorMessage = ""
                                scope.launch {
                                    val error = repository.createAdminAccount(phone, password, displayName, countryCode)
                                    busy = false
                                    if (error == null) {
                                        onLoginSuccess()
                                    } else {
                                        errorMessage = error
                                    }
                                }
                            },
                            icon = Icons.Default.AdminPanelSettings,
                            // Black with white text in dark mode; white with
                            // black text in light mode (Ink950 is semantic and
                            // inverts, which read as disabled here).
                            containerColor = if (LocalDarkTheme.current) Color(0xFF000000) else Color(0xFFFFFFFF),
                            contentColor = if (LocalDarkTheme.current) Color.White else DorjaColors.Ink950,
                            testTag = "auth_create_admin"
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = L("auth_admin_note"),
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
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
                    text = L("auth_footer"),
                    style = MaterialTheme.typography.labelSmall,
                    color = DorjaColors.Gray500
                )
            }
        }
    }
}
