package com.moneymanager.app.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*
import com.moneymanager.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    val context  = LocalContext.current
    val auth     = FirebaseAuth.getInstance()
    var phone    by remember { mutableStateOf("") }
    var otp      by remember { mutableStateOf("") }
    var step     by remember { mutableIntStateOf(0) }  // 0=phone, 1=otp
    var error    by remember { mutableStateOf("") }
    var loading  by remember { mutableStateOf(false) }
    var verificationId by remember { mutableStateOf("") }

    val infiniteAnim = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteAnim.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOutQuad), RepeatMode.Reverse),
        label = "pulse"
    )

    // Google Sign-In setup
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("YOUR_WEB_CLIENT_ID") // Replace with your Web Client ID from Firebase Console
        .requestEmail()
        .build()
    val googleClient = GoogleSignIn.getClient(context, gso)
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                loading = true
                auth.signInWithCredential(credential).addOnCompleteListener {
                    loading = false
                    if (it.isSuccessful) onAuthSuccess() else error = it.exception?.message ?: "Sign in failed"
                }
            } catch (e: ApiException) { error = "Google sign-in failed: ${e.message}" }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(BgPrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Logo / Brand
            Box(
                modifier = Modifier.size(72.dp).background(
                    Brush.radialGradient(listOf(Accent1.copy(alpha = 0.3f), BgPrimary)),
                    shape = RoundedCornerShape(20.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Text("₹", fontSize = 36.sp, color = Accent1, modifier = Modifier.alpha(pulse))
            }

            Text(
                "Money Manager",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = TextPrimary
            )
            Text(
                "Track. Split. Settle.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            if (step == 0) {
                // Phone input
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("PHONE NUMBER", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it },
                        placeholder = { Text("+91 98765 43210", color = TextTertiary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent1, unfocusedBorderColor = BgCardAlt,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            cursorColor = Accent1, focusedContainerColor = BgCard,
                            unfocusedContainerColor = BgCard
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    if (error.isNotEmpty()) Text(error, color = ExpenseRed, style = MaterialTheme.typography.bodySmall)

                    Button(
                        onClick = {
                            error = ""; loading = true
                            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                override fun onVerificationCompleted(cred: PhoneAuthCredential) {
                                    auth.signInWithCredential(cred).addOnCompleteListener {
                                        loading = false
                                        if (it.isSuccessful) onAuthSuccess()
                                    }
                                }
                                override fun onVerificationFailed(e: FirebaseException) {
                                    loading = false; error = e.message ?: "Verification failed"
                                }
                                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                                    loading = false; verificationId = id; step = 1
                                }
                            }
                            PhoneAuthProvider.verifyPhoneNumber(
                                PhoneAuthOptions.newBuilder(auth)
                                    .setPhoneNumber(phone.trim())
                                    .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
                                    .setActivity(context as Activity)
                                    .setCallbacks(callbacks)
                                    .build()
                            )
                        },
                        enabled = phone.trim().length >= 10 && !loading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent1)
                    ) {
                        if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = TextPrimary, strokeWidth = 2.dp)
                        else Text("Send OTP", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HorizontalDivider(Modifier.weight(1f), color = BgCardAlt)
                        Text("OR", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        HorizontalDivider(Modifier.weight(1f), color = BgCardAlt)
                    }

                    OutlinedButton(
                        onClick = { googleLauncher.launch(googleClient.signInIntent) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedButtonDefaults.colors(contentColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BgCardAlt)
                    ) {
                        Text("Continue with Google", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                // OTP input
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter the 6-digit OTP sent to $phone", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)

                    OutlinedTextField(
                        value = otp, onValueChange = { if (it.length <= 6) otp = it },
                        placeholder = { Text("------", color = TextTertiary, letterSpacing = 8.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent1, unfocusedBorderColor = BgCardAlt,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            cursorColor = Accent1, focusedContainerColor = BgCard,
                            unfocusedContainerColor = BgCard
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    if (error.isNotEmpty()) Text(error, color = ExpenseRed, style = MaterialTheme.typography.bodySmall)

                    Button(
                        onClick = {
                            error = ""; loading = true
                            val cred = PhoneAuthProvider.getCredential(verificationId, otp.trim())
                            auth.signInWithCredential(cred).addOnCompleteListener {
                                loading = false
                                if (it.isSuccessful) onAuthSuccess()
                                else error = it.exception?.message ?: "Invalid OTP"
                            }
                        },
                        enabled = otp.length == 6 && !loading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent1)
                    ) {
                        if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = TextPrimary, strokeWidth = 2.dp)
                        else Text("Verify & Sign In", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    TextButton(onClick = { step = 0; otp = ""; error = "" }) {
                        Text("← Change number", color = AccentBlue)
                    }
                }
            }
        }
    }
}
