package com.maskaravivek.provepassiveidentityandroiddemo.network

import com.maskaravivek.provepassiveidentityandroiddemo.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ProveBackendService {
    
    @POST("initialize")
    suspend fun initialize(@Body request: InitializeRequest): Response<InitializeResponse>
    
    @POST("verify")
    suspend fun verify(@Body request: VerifyRequest): Response<VerifyResponse>
}