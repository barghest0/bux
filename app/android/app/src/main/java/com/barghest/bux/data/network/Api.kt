package com.barghest.bux.data.network

import com.barghest.bux.data.model.TransactionDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json


class Api() {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            })
        }

        install(DefaultRequest) {
            header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3NjI4Nzk3NDAsInN1YiI6MX0.crCxDSVv26bqnVN1sNd2D5HaYB7SiPkeK5JgV5PR2tg")
        }
    }

    private val baseUrl = "http://10.0.2.2:8082"

    suspend fun fetchTransactions(): List<TransactionDto> {
        return client.get("$baseUrl/transactions").body()
    }

    suspend fun postTransaction(dto: TransactionDto) {
        client.post("$baseUrl/transactions") {
            setBody(dto)
        }
    }
}