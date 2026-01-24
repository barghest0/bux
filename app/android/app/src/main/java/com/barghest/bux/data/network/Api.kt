package com.barghest.bux.data.network

import android.util.Log
import com.barghest.bux.data.dto.AccountResponse
import com.barghest.bux.data.dto.BrokerResponse
import com.barghest.bux.data.dto.CategoryResponse
import com.barghest.bux.data.dto.CreateAccountRequest
import com.barghest.bux.data.dto.CreateBrokerRequest
import com.barghest.bux.data.dto.CreateCategoryRequest
import com.barghest.bux.data.dto.CreatePortfolioRequest
import com.barghest.bux.data.dto.CreateTradeRequest
import com.barghest.bux.data.dto.CreateTransactionRequest
import com.barghest.bux.data.dto.LoginRequest
import com.barghest.bux.data.dto.LoginResponse
import com.barghest.bux.data.dto.PortfolioResponse
import com.barghest.bux.data.dto.PortfolioSummaryResponse
import com.barghest.bux.data.dto.SecurityResponse
import com.barghest.bux.data.dto.TradeResponse
import com.barghest.bux.data.dto.TransactionResponse
import com.barghest.bux.data.dto.UpdateAccountRequest
import com.barghest.bux.data.dto.UpdateCategoryRequest
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
    private val investmentUrl = "http://10.0.2.2:8083"

    private fun HttpRequestBuilder.addAuthHeader() {
        tokenManager.getToken()?.let { token ->
            header("Authorization", "Bearer $token")
        }
    }

    // Auth
    suspend fun login(request: LoginRequest): Result<LoginResponse> = safeApiCall {
        client.post("$userUrl/auth/login") {
            setBody(request)
        }.body()
    }

    // Accounts
    suspend fun fetchAccounts(): Result<List<AccountResponse>> = safeApiCall {
        client.get("$transactionUrl/accounts") {
            addAuthHeader()
        }.body()
    }

    suspend fun fetchAccount(id: Int): Result<AccountResponse> = safeApiCall {
        client.get("$transactionUrl/accounts/$id") {
            addAuthHeader()
        }.body()
    }

    suspend fun createAccount(request: CreateAccountRequest): Result<AccountResponse> = safeApiCall {
        client.post("$transactionUrl/accounts") {
            addAuthHeader()
            setBody(request)
        }.body()
    }

    suspend fun updateAccount(id: Int, request: UpdateAccountRequest): Result<AccountResponse> = safeApiCall {
        client.put("$transactionUrl/accounts/$id") {
            addAuthHeader()
            setBody(request)
        }.body()
    }

    suspend fun deleteAccount(id: Int): Result<Unit> = safeApiCall {
        client.delete("$transactionUrl/accounts/$id") {
            addAuthHeader()
        }
    }

    // Transactions
    suspend fun fetchTransactions(): Result<List<TransactionResponse>> = safeApiCall {
        client.get("$transactionUrl/transactions") {
            addAuthHeader()
        }.body()
    }

    suspend fun fetchTransactionsByAccount(accountId: Int): Result<List<TransactionResponse>> = safeApiCall {
        client.get("$transactionUrl/transactions") {
            addAuthHeader()
            parameter("account_id", accountId)
        }.body()
    }

    suspend fun createTransaction(request: CreateTransactionRequest): Result<TransactionResponse> = safeApiCall {
        client.post("$transactionUrl/transactions") {
            addAuthHeader()
            setBody(request)
        }.body()
    }

    // Categories
    suspend fun fetchCategories(): Result<List<CategoryResponse>> = safeApiCall {
        client.get("$transactionUrl/categories") {
            addAuthHeader()
        }.body()
    }

    suspend fun fetchCategoriesByType(type: String): Result<List<CategoryResponse>> = safeApiCall {
        client.get("$transactionUrl/categories") {
            addAuthHeader()
            parameter("type", type)
        }.body()
    }

    suspend fun createCategory(request: CreateCategoryRequest): Result<CategoryResponse> = safeApiCall {
        client.post("$transactionUrl/categories") {
            addAuthHeader()
            setBody(request)
        }.body()
    }

    suspend fun updateCategory(id: Int, request: UpdateCategoryRequest): Result<CategoryResponse> = safeApiCall {
        client.put("$transactionUrl/categories/$id") {
            addAuthHeader()
            setBody(request)
        }.body()
    }

    suspend fun deleteCategory(id: Int): Result<Unit> = safeApiCall {
        client.delete("$transactionUrl/categories/$id") {
            addAuthHeader()
        }
    }

    // Investments - Brokers
    suspend fun fetchBrokers(): Result<List<BrokerResponse>> = safeApiCall {
        client.get("$investmentUrl/api/brokers") {
            addAuthHeader()
        }.body()
    }

    suspend fun createBroker(request: CreateBrokerRequest): Result<BrokerResponse> = safeApiCall {
        client.post("$investmentUrl/api/brokers") {
            addAuthHeader()
            setBody(request)
        }.body()
    }

    // Investments - Portfolios
    suspend fun fetchPortfolios(): Result<List<PortfolioResponse>> = safeApiCall {
        client.get("$investmentUrl/api/portfolios") {
            addAuthHeader()
        }.body()
    }

    suspend fun fetchPortfolio(id: Int): Result<PortfolioResponse> = safeApiCall {
        client.get("$investmentUrl/api/portfolios/$id") {
            addAuthHeader()
        }.body()
    }

    suspend fun fetchPortfolioSummary(id: Int): Result<PortfolioSummaryResponse> = safeApiCall {
        client.get("$investmentUrl/api/portfolios/$id/summary") {
            addAuthHeader()
        }.body()
    }

    suspend fun createPortfolio(request: CreatePortfolioRequest): Result<PortfolioResponse> = safeApiCall {
        client.post("$investmentUrl/api/portfolios") {
            addAuthHeader()
            setBody(request)
        }.body()
    }

    // Investments - Trades
    suspend fun fetchTrades(portfolioId: Int): Result<List<TradeResponse>> = safeApiCall {
        client.get("$investmentUrl/api/portfolios/$portfolioId/trades") {
            addAuthHeader()
        }.body()
    }

    suspend fun createTrade(request: CreateTradeRequest): Result<TradeResponse> = safeApiCall {
        client.post("$investmentUrl/api/trades") {
            addAuthHeader()
            setBody(request)
        }.body()
    }

    // Investments - Securities
    suspend fun searchSecurities(query: String, type: String? = null): Result<List<SecurityResponse>> = safeApiCall {
        client.get("$investmentUrl/api/securities") {
            addAuthHeader()
            parameter("query", query)
            type?.let { parameter("type", it) }
        }.body()
    }

    suspend fun fetchSecurity(id: Int): Result<SecurityResponse> = safeApiCall {
        client.get("$investmentUrl/api/securities/$id") {
            addAuthHeader()
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
