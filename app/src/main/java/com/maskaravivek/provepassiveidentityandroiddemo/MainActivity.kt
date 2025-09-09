package com.maskaravivek.provepassiveidentityandroiddemo

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.maskaravivek.provepassiveidentityandroiddemo.model.VerificationResult
import com.maskaravivek.provepassiveidentityandroiddemo.model.VerifyResponse
import com.maskaravivek.provepassiveidentityandroiddemo.ui.ResponseHandlers

class MainActivity : AppCompatActivity(), ProveIntegration.ProveIntegrationCallback {
    
    private lateinit var proveIntegration: ProveIntegration
    private val tag = "MainActivity"
    
    private lateinit var phoneEditText: TextInputEditText
    private lateinit var verifyButton: Button
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var resultsContainer: View
    private lateinit var resultTitle: TextView
    private lateinit var resultDetails: TextView
    
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
        setContentView(R.layout.activity_main)
        
        // Initialize Prove Integration
        proveIntegration = ProveIntegration(
            context = this,
            backendBaseUrl = "https://your-backend-url.com/"
        )
        
        // Initialize UI components
        initializeViews()
        setupClickListeners()
        updatePermissionUI()
    }
    
    private fun initializeViews() {
        phoneEditText = findViewById(R.id.phoneEditText)
        verifyButton = findViewById(R.id.verifyButton)
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        resultsContainer = findViewById(R.id.resultsContainer)
        resultTitle = findViewById(R.id.resultTitle)
        resultDetails = findViewById(R.id.resultDetails)
    }
    
    private fun setupClickListeners() {
        verifyButton.setOnClickListener {
            val phoneNumber = phoneEditText.text?.toString()?.trim() ?: ""
            
            if (phoneNumber.isEmpty()) {
                showStatus("Please enter a phone number")
                return@setOnClickListener
            }
            
            val hasPermission = PermissionUtils.hasPhoneStatePermission(this)
            if (hasPermission) {
                startVerification(phoneNumber)
                showLoading(true)
                showStatus("🔍 Verifying your identity...")
                hideResults()
            } else {
                requestPhonePermission()
            }
        }
    }
    
    private fun updatePermissionUI() {
        val hasPermission = PermissionUtils.hasPhoneStatePermission(this)
        // You can add a permission warning TextView to the layout if needed
    }
    
    private fun showLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        verifyButton.isEnabled = !loading
        phoneEditText.isEnabled = !loading
    }
    
    private fun showStatus(message: String) {
        statusText.text = message
        statusText.visibility = View.VISIBLE
    }
    
    private fun hideResults() {
        resultsContainer.visibility = View.GONE
    }
    
    private fun showResults(title: String, details: String) {
        resultTitle.text = title
        resultDetails.text = details
        resultsContainer.visibility = View.VISIBLE
        showLoading(false)
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
        showResults("✅ Verification Successful", response.toString())
        ResponseHandlers.showToast(this, "Identity verified successfully!")
    }
    
    override fun onVerificationFailure(error: String, result: VerificationResult) {
        Log.d(tag, "Verification failed: $error, result: $result")
        showLoading(false)
        showStatus("❌ Verification failed: $error")
        showResults("❌ Verification Failed", "Error: $error\nResult: $result")
    }
    
    override fun onFallbackRequired() {
        Log.d(tag, "Fallback verification required")
        showLoading(false)
        showStatus("⚠️ Fallback verification required")
        ResponseHandlers.showToast(this, "Fallback verification required")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        proveIntegration.cleanup()
    }
}