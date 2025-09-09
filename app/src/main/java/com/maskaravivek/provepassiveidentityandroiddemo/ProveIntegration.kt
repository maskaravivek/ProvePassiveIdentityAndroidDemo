package com.maskaravivek.provepassiveidentityandroiddemo

import android.content.Context
import android.util.Log
import com.maskaravivek.provepassiveidentityandroiddemo.model.*
import com.maskaravivek.provepassiveidentityandroiddemo.network.ProveBackendService
import com.prove.sdk.proveauth.ProveAuth
import com.prove.sdk.proveauth.ProveAuthException
import com.prove.sdk.proveauth.AuthFinishStep
import com.prove.sdk.proveauth.OtpFinishInput
import com.prove.sdk.proveauth.OtpStartStep
import com.prove.sdk.proveauth.OtpFinishStep
import com.prove.sdk.proveauth.OtpStartInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ProveIntegration(
    private val context: Context,
    private val backendBaseUrl: String = "http://localhost:3000/"
) {

    private val tag = "ProveIntegration"
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    private val backendService: ProveBackendService by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val httpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        Retrofit.Builder()
            .baseUrl(backendBaseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ProveBackendService::class.java)
    }

    private var proveAuth: ProveAuth? = null
    private var correlationId: String? = null

    interface ProveIntegrationCallback {
        fun onVerificationSuccess(response: VerifyResponse)
        fun onVerificationFailure(error: String, result: VerificationResult)
        fun onFallbackRequired()
    }

    fun initializeProveAuth(
        phoneNumber: String,
        callback: ProveIntegrationCallback
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Step 1: Initialize with backend (matching React implementation)
                val initResponse = backendService.initialize(
                    InitializeRequest(
                        phoneNumber = phoneNumber,
                        possessionType = "mobile"
                    )
                )

                val responseBody = initResponse.body()
                if (!initResponse.isSuccessful || responseBody == null) {
                    withContext(Dispatchers.Main) {
                        callback.onVerificationFailure(
                            "Failed to initialize: ${initResponse.message()}",
                            VerificationResult.ERROR
                        )
                    }
                    return@launch
                }

                // Store correlation ID for verification
                correlationId = responseBody.correlationId
                val authToken = responseBody.authToken

                if (authToken == null) {
                    withContext(Dispatchers.Main) {
                        callback.onVerificationFailure(
                            "No auth token received from backend",
                            VerificationResult.ERROR
                        )
                    }
                    return@launch
                }

                // Step 2: Setup ProveAuth with interface implementations
                val authFinishStep = AuthFinishStep { 
                    handleAuthFinish(callback)
                }
                
                val otpStartStep = OtpStartStep { phoneNumberNeeded, otpException, otpStartStepCallback ->
                    Log.d(tag, "OTP fallback triggered - phoneNumberNeeded: $phoneNumberNeeded")
                    
                    if (otpException != null) {
                        Log.e(tag, "OTP Start exception: ${otpException.message}")
                        otpStartStepCallback.onError()
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            callback.onFallbackRequired()
                        }
                        otpStartStepCallback.onSuccess(OtpStartInput(phoneNumber))
                    }
                }
                
                val otpFinishStep = OtpFinishStep { otpException, otpFinishStepCallback ->
                    Log.d(tag, "OTP finish step called")
                    
                    if (otpException != null) {
                        Log.e(tag, "OTP Finish exception: ${otpException.message}")
                        otpFinishStepCallback.onError()
                    } else {
                        otpFinishStepCallback.onSuccess(OtpFinishInput(""))
                    }
                }
                
                proveAuth = ProveAuth.builder()
                    .withAuthFinishStep(authFinishStep)
                    .withOtpFallback(otpStartStep, otpFinishStep)
                    .withContext(context)
                    .build()

                // Step 3: Start authentication
                startAuthentication(authToken, callback)

            } catch (e: Exception) {
                Log.e(tag, "Error initializing ProveAuth", e)
                withContext(Dispatchers.Main) {
                    callback.onVerificationFailure(
                        "Initialization error: ${e.message}",
                        VerificationResult.ERROR
                    )
                }
            }
        }
    }

    private suspend fun startAuthentication(
        authToken: String,
        callback: ProveIntegrationCallback
    ) {
        try {
            executor.submit {
                try {
                    proveAuth?.authenticate(authToken)
                } catch (e: Exception) {
                    Log.e(tag, "Authentication failed", e)
                    CoroutineScope(Dispatchers.Main).launch {
                        callback.onVerificationFailure(
                            "Authentication failed: ${e.message}",
                            VerificationResult.ERROR
                        )
                    }
                }
            }
        } catch (e: ProveAuthException) {
            Log.e(tag, "ProveAuth exception", e)
            withContext(Dispatchers.Main) {
                callback.onVerificationFailure(
                    "Prove authentication error: ${e.message}",
                    VerificationResult.ERROR
                )
            }
        }
    }

    private fun handleAuthFinish(callback: ProveIntegrationCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentCorrelationId = correlationId
                if (currentCorrelationId == null) {
                    withContext(Dispatchers.Main) {
                        callback.onVerificationFailure(
                            "No correlation ID available for verification",
                            VerificationResult.ERROR
                        )
                    }
                    return@launch
                }

                Log.d(tag, "Authentication completed, verifying with backend using correlationId: $currentCorrelationId")

                val verifyResponse = backendService.verify(
                    VerifyRequest(correlationId = currentCorrelationId)
                )

                val response = verifyResponse.body()

                withContext(Dispatchers.Main) {
                    if (verifyResponse.isSuccessful && response != null) {
                        when {
                            response.success && response.verified -> {
                                callback.onVerificationSuccess(response)
                            }
                            response.fallbackRequired -> {
                                callback.onFallbackRequired()
                            }
                            else -> {
                                callback.onVerificationFailure(
                                    response.message ?: "Verification failed",
                                    VerificationResult.SOFT_FAILURE
                                )
                            }
                        }
                    } else {
                        callback.onVerificationFailure(
                            "Backend verification failed",
                            VerificationResult.ERROR
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e(tag, "Error during verification", e)
                withContext(Dispatchers.Main) {
                    callback.onVerificationFailure(
                        "Verification error: ${e.message}",
                        VerificationResult.ERROR
                    )
                }
            }
        }
    }

    fun cleanup() {
        executor.shutdown()
    }
}