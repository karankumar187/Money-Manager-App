package com.moneymanager.app.ui.screens.auth

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.moneymanager.app.ui.theme.*

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    val context  = LocalContext.current
    val auth     = FirebaseAuth.getInstance()

    // +91 is fixed — user only types the 10-digit number
    var phoneDigits  by remember { mutableStateOf("") }
    var otp          by remember { mutableStateOf("") }
    var step         by remember { mutableIntStateOf(0) } // 0=phone, 1=otp
    var error        by remember { mutableStateOf("") }
    var loading      by remember { mutableStateOf(false) }
    var verificationId by remember { mutableStateOf("") }
    var resendToken    by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }

    val phoneFocus = remember { FocusRequester() }
    val otpFocus   = remember { FocusRequester() }

    val infiniteAnim = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteAnim.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    // Full phone number always includes +91
    val fullPhone = "+91${phoneDigits.trim()}"

    fun sendOtp(forceResend: Boolean = false) {
        error = ""; loading = true
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(cred: PhoneAuthCredential) {
                auth.signInWithCredential(cred).addOnCompleteListener {
                    loading = false
                    if (it.isSuccessful) onAuthSuccess()
                    else error = it.exception?.message ?: "Auto-verification failed"
                }
            }
            override fun onVerificationFailed(e: FirebaseException) {
                loading = false
                error = when {
                    e.message?.contains("invalid app identifier", true) == true ->
                        "App not verified. Add SHA-1 to Firebase Console."
                    e.message?.contains("too many requests", true) == true ->
                        "Too many attempts. Try after some time."
                    else -> e.message ?: "Verification failed"
                }
            }
            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                loading = false; verificationId = id; resendToken = token; step = 1
            }
        }
        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(fullPhone)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(context as Activity)
            .setCallbacks(callbacks)
        if (forceResend && resendToken != null) builder.setForceResendingToken(resendToken!!)
        PhoneAuthProvider.verifyPhoneNumber(builder.build())
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

            // ── Logo ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        Brush.radialGradient(listOf(Accent1.copy(alpha = 0.25f), BgPrimary)),
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("₹", fontSize = 40.sp, color = Accent1, modifier = Modifier.alpha(pulse))
            }

            Text(
                "Money Manager",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp
                ),
                color = TextPrimary
            )
            Text(
                "Track. Split. Settle.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            // ── Phone step ────────────────────────────────────
            if (step == 0) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "PHONE NUMBER",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )

                    // Phone input — +91 locked, only digits editable
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(BgCard)
                            .border(
                                1.dp,
                                if (phoneDigits.isNotEmpty()) Accent1.copy(0.5f) else BgCardAlt,
                                RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Country code badge (non-editable)
                        Box(
                            Modifier
                                .background(Accent1.copy(0.12f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "🇮🇳 +91",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Accent1
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        // Thin divider
                        Box(
                            Modifier
                                .width(1.dp)
                                .height(22.dp)
                                .background(BgCardAlt)
                        )

                        Spacer(Modifier.width(10.dp))

                        // Editable phone digits
                        BasicTextField(
                            value = phoneDigits,
                            onValueChange = { v ->
                                // Only allow digits, max 10
                                val digits = v.filter { it.isDigit() }
                                if (digits.length <= 10) phoneDigits = digits
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(phoneFocus),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            cursorBrush = SolidColor(Accent1),
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            ),
                            decorationBox = { inner ->
                                if (phoneDigits.isEmpty()) {
                                    Text(
                                        "10-digit number",
                                        color = TextTertiary,
                                        fontSize = 16.sp
                                    )
                                }
                                inner()
                            }
                        )

                        if (phoneDigits.isNotEmpty()) {
                            Text(
                                "${phoneDigits.length}/10",
                                fontSize = 11.sp,
                                color = if (phoneDigits.length == 10) IncomeGreen else TextTertiary
                            )
                        }
                    }

                    if (error.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(0.1f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                error,
                                color = ExpenseRed,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = { sendOtp() },
                        enabled = phoneDigits.length == 10 && !loading,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent1)
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                Modifier.size(22.dp), color = TextPrimary, strokeWidth = 2.dp
                            )
                        } else {
                            Text("Send OTP", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }
                }

            // ── OTP step ──────────────────────────────────────
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "OTP sent to +91 $phoneDigits",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 6-box OTP input
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (0..5).forEach { i ->
                            val char = otp.getOrNull(i)?.toString() ?: ""
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.85f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BgCard)
                                    .border(
                                        1.5.dp,
                                        if (otp.length == i) Accent1 else if (char.isNotEmpty()) Accent1.copy(0.4f) else BgCardAlt,
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    char,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    // Hidden text field driving OTP boxes
                    BasicTextField(
                        value = otp,
                        onValueChange = { v ->
                            val digits = v.filter { it.isDigit() }
                            if (digits.length <= 6) otp = digits
                        },
                        modifier = Modifier
                            .height(1.dp)
                            .focusRequester(otpFocus),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        cursorBrush = SolidColor(Accent1),
                        textStyle = TextStyle(color = BgPrimary, fontSize = 1.sp)
                    )

                    LaunchedEffect(Unit) {
                        try { otpFocus.requestFocus() } catch (_: Exception) {}
                    }

                    if (error.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(0.1f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                error,
                                color = ExpenseRed,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            error = ""; loading = true
                            val cred = PhoneAuthProvider.getCredential(verificationId, otp.trim())
                            auth.signInWithCredential(cred).addOnCompleteListener {
                                loading = false
                                if (it.isSuccessful) onAuthSuccess()
                                else { error = it.exception?.message ?: "Invalid OTP"; otp = "" }
                            }
                        },
                        enabled = otp.length == 6 && !loading,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent1)
                    ) {
                        if (loading) {
                            CircularProgressIndicator(Modifier.size(22.dp), color = TextPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("Verify & Sign In", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { step = 0; otp = ""; error = "" }) {
                            Text("← Change number", color = AccentBlue, fontSize = 13.sp)
                        }
                        TextButton(onClick = { otp = ""; error = ""; sendOtp(forceResend = true) }) {
                            Text("Resend OTP", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
