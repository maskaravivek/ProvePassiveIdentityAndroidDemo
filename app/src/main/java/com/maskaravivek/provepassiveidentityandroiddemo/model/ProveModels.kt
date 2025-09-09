package com.maskaravivek.provepassiveidentityandroiddemo.model

data class InitializeRequest(
    val phoneNumber: String,
    val possessionType: String = "mobile"
)

data class InitializeResponse(
    val authToken: String? = null,
    val correlationId: String? = null,
    val success: Boolean = false,
    val message: String? = null
)

data class VerifyRequest(
    val correlationId: String
)

data class VerifyResponse(
    val success: Boolean,
    val verified: Boolean = false,
    val firstName: String? = null,
    val lastName: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zip: String? = null,
    val message: String? = null,
    val fallbackRequired: Boolean = false
)

enum class VerificationResult {
    SUCCESS,
    SOFT_FAILURE,
    FALLBACK_REQUIRED,
    ERROR
}