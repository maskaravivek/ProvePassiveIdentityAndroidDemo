package com.maskaravivek.provepassiveidentityandroiddemo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maskaravivek.provepassiveidentityandroiddemo.model.VerificationResult
import com.maskaravivek.provepassiveidentityandroiddemo.model.VerifyResponse
import com.maskaravivek.provepassiveidentityandroiddemo.ui.ResponseHandlers
import com.maskaravivek.provepassiveidentityandroiddemo.ui.theme.ProvePassiveIdentityAndroidDemoTheme

class MainActivity : ComponentActivity(), ProveIntegration.ProveIntegrationCallback {
    
    private lateinit var proveIntegration: ProveIntegration
    private val tag = "MainActivity"
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            ResponseHandlers.showToast(this, "Permission granted! You can now verify.")
        } else {
            ResponseHandlers.showToast(
                this, 
                "Permission denied. Fallback verification will be used."
            )
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Prove Integration
        proveIntegration = ProveIntegration(
            context = this,
            backendBaseUrl = "https://your-backend-url.com/"
        )
        
        setContent {
            ProvePassiveIdentityAndroidDemoTheme {
                ProveVerificationScreen()
            }
        }
    }
    
    @Composable
    fun ProveVerificationScreen() {
        var phoneNumber by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var statusText by remember { mutableStateOf("") }
        var showResults by remember { mutableStateOf(false) }
        var resultTitle by remember { mutableStateOf("") }
        var resultDetails by remember { mutableStateOf("") }
        var hasPermission by remember { mutableStateOf(PermissionUtils.hasPhoneStatePermission(this@MainActivity)) }
        
        val context = LocalContext.current
        
        LaunchedEffect(Unit) {
            hasPermission = PermissionUtils.hasPhoneStatePermission(context)
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Prove Passive Identity Demo",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            Text(
                text = "Enter your phone number to verify your identity instantly using Prove's passive verification.",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                enabled = !isLoading
            )
            
            Button(
                onClick = {
                    if (phoneNumber.trim().isEmpty()) {
                        statusText = "Please enter a phone number"
                        return@Button
                    }
                    
                    if (hasPermission) {
                        startVerification(phoneNumber.trim())
                        isLoading = true
                        statusText = "🔍 Verifying your identity..."
                        showResults = false
                    } else {
                        requestPhonePermission()
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Verify Identity", fontSize = 16.sp)
            }
            
            if (!hasPermission) {
                Text(
                    text = "⚠️ Phone permission required for passive verification",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            
            if (statusText.isNotEmpty()) {
                Text(
                    text = statusText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            
            if (showResults) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = resultTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = resultDetails,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
    
    private fun requestPhonePermission() {
        if (PermissionUtils.shouldShowRationale(this)) {
            ResponseHandlers.showPermissionRationale(
                context = this,
                onAccept = {
                    requestPermissionLauncher.launch(android.Manifest.permission.READ_PHONE_STATE)
                },
                onDecline = {
                    ResponseHandlers.showToast(
                        this, 
                        "Permission required for automatic verification"
                    )
                }
            )
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.READ_PHONE_STATE)
        }
    }
    private fun startVerification(phoneNumber: String) {
        Log.d(tag, "Starting verification for: $phoneNumber")
        proveIntegration.initializeProveAuth(phoneNumber, this)
    }
    
    // ProveIntegration.ProveIntegrationCallback implementations
    override fun onVerificationSuccess(response: VerifyResponse) {
        Log.d(tag, "Verification successful: $response")
        ResponseHandlers.handleVerificationSuccess(
            context = this,
            response = response,
            onSuccessConfirmed = { 
                ResponseHandlers.showToast(this, "Identity verified successfully!")
            }
        )
    }
    
    override fun onVerificationFailure(error: String, result: VerificationResult) {
        Log.d(tag, "Verification failed: $error, result: $result")
        ResponseHandlers.handleVerificationFailure(
            context = this,
            error = error,
            result = result,
            onRetry = { },
            onFallback = { }
        )
    }
    
    override fun onFallbackRequired() {
        Log.d(tag, "Fallback verification required")
        ResponseHandlers.handleFallbackRequired(
            context = this,
            phoneNumber = "",
            onSmsRequested = {
                ResponseHandlers.showToast(this, "SMS verification requested")
            },
            onCallRequested = {
                ResponseHandlers.showToast(this, "Call verification requested")
            },
            onCancel = { }
        )
    }
    
    override fun onDestroy() {
        super.onDestroy()
        proveIntegration.cleanup()
    }
}