package com.maskaravivek.provepassiveidentityandroiddemo.ui

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.maskaravivek.provepassiveidentityandroiddemo.model.VerificationResult
import com.maskaravivek.provepassiveidentityandroiddemo.model.VerifyResponse

object ResponseHandlers {
    
    fun handleVerificationSuccess(
        context: Context,
        response: VerifyResponse,
        onSuccessConfirmed: (VerifyResponse) -> Unit
    ) {
        val message = buildString {
            append("✅ Verification Successful!\n\n")
            response.firstName?.let { append("Name: $it") }
            response.lastName?.let { append(" $it\n") }
            response.address?.let { append("Address: $it\n") }
            response.city?.let { append("City: $it\n") }
            response.state?.let { append("State: $it\n") }
            response.zip?.let { append("ZIP: $it") }
        }
        
        AlertDialog.Builder(context)
            .setTitle("Identity Verified")
            .setMessage(message)
            .setPositiveButton("Continue") { _, _ ->
                onSuccessConfirmed(response)
            }
            .setCancelable(false)
            .show()
    }
    
    fun handleVerificationFailure(
        context: Context,
        error: String,
        result: VerificationResult,
        onRetry: () -> Unit,
        onFallback: () -> Unit
    ) {
        val (title, message, actions) = when (result) {
            VerificationResult.SOFT_FAILURE -> {
                Triple(
                    "Verification Inconclusive",
                    "We couldn't verify your identity automatically. This might be due to:\n\n" +
                    "• Network connectivity issues\n" +
                    "• Device not recognized\n" +
                    "• Carrier limitations\n\n" +
                    "Error: $error",
                    listOf("Try Again" to onRetry, "Use Alternative Method" to onFallback)
                )
            }
            
            VerificationResult.ERROR -> {
                Triple(
                    "Verification Error",
                    "An unexpected error occurred during verification:\n\n$error\n\n" +
                    "Please check your internet connection and try again.",
                    listOf("Retry" to onRetry, "Cancel" to {})
                )
            }
            
            else -> {
                Triple(
                    "Verification Failed",
                    error,
                    listOf("OK" to {})
                )
            }
        }
        
        val dialogBuilder = AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
        
        actions.forEachIndexed { index, (buttonText, action) ->
            when (index) {
                0 -> dialogBuilder.setPositiveButton(buttonText) { _, _ -> action() }
                1 -> dialogBuilder.setNegativeButton(buttonText) { _, _ -> action() }
            }
        }
        
        dialogBuilder.show()
    }
    
    fun handleFallbackRequired(
        context: Context,
        phoneNumber: String,
        onSmsRequested: () -> Unit,
        onCallRequested: () -> Unit,
        onCancel: () -> Unit
    ) {
        val message = "We need to verify your phone number using an alternative method.\n\n" +
                     "Phone: $phoneNumber\n\n" +
                     "Choose your preferred verification method:"
        
        AlertDialog.Builder(context)
            .setTitle("Additional Verification Required")
            .setMessage(message)
            .setPositiveButton("Send SMS Code") { _, _ ->
                showToast(context, "SMS verification requested")
                onSmsRequested()
            }
            .setNeutralButton("Call Me") { _, _ ->
                showToast(context, "Phone call verification requested")
                onCallRequested()
            }
            .setNegativeButton("Cancel") { _, _ ->
                onCancel()
            }
            .setCancelable(false)
            .show()
    }
    
    fun showLoadingState(context: Context, message: String = "Verifying your identity...") {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    
    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    
    fun showPermissionRationale(
        context: Context,
        onAccept: () -> Unit,
        onDecline: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle("Phone Permission Required")
            .setMessage(
                "Prove needs access to your phone state to verify your identity automatically.\n\n" +
                "This permission is used to:\n" +
                "• Verify your phone number without SMS\n" +
                "• Detect your mobile carrier\n" +
                "• Ensure secure identity verification\n\n" +
                "Your privacy is protected - no personal information is stored or shared."
            )
            .setPositiveButton("Grant Permission") { _, _ -> onAccept() }
            .setNegativeButton("Decline") { _, _ -> onDecline() }
            .setCancelable(false)
            .show()
    }
}