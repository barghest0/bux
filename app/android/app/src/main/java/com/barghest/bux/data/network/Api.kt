package com.barghest.bux.data.network

import android.util.Log
import com.barghest.bux.data.dto.LoginRequest
import com.barghest.bux.data.dto.LoginResponse
import com.barghest.bux.data.dto.TransactionRequest
import com.barghest.bux.data.dto.TransactionResponse
import com.barghest.bux.data.local.TokenManager
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json


class Api(private val tokenManager: TokenManager) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            })
        }

        install(DefaultRequest) {
            contentType(ContentType.Application.Json)
        }
    }

    private val transactionUrl = "http://10.0.2.2:8082"
    private val userUrl = "http://10.0.2.2:8081"

    private fun HttpRequestBuilder.addAuthHeader() {
        tokenManager.getToken()?.let { token ->
            header("Authorization", "Bearer $token")
        }
    }

    suspend fun fetchTransactions(): Result<List<TransactionResponse>> = safeApiCall {
        client.get("$transactionUrl/transactions") {
            addAuthHeader()
        }.body()
    }

    suspend fun postTransaction(request: TransactionRequest): Result<Unit> = safeApiCall {
        client.post("$transactionUrl/transactions") {
            addAuthHeader()
            setBody(request)
        }
    }

    suspend fun login(request: LoginRequest): Result<LoginResponse> = safeApiCall {
        client.post("$userUrl/auth/login") {
            setBody(request)
        }.body()
    }

    private suspend inline fun <T> safeApiCall(crossinline block: suspend () -> T): Result<T> {
        return withContext(Dispatchers.IO) {
            try {
                val result = block()
                Result.success(result)
            } catch (e: io.ktor.client.plugins.ResponseException) {
                val errorBody = e.response.bodyAsText()
                Log.e("Api", "HTTP Error ${e.response.status}: $errorBody")
                Result.failure(Exception("HTTP ${e.response.status}: $errorBody"))
            } catch (e: Exception) {
                Log.e("Api", "Network Error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
}
