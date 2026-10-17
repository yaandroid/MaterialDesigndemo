package com.brahm.crm.ai

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// ---------- Wire models ----------
data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.6,
    @SerializedName("max_tokens") val maxTokens: Int = 400
)

data class ChatChoice(val message: ChatMessage?)
data class ChatResponse(val choices: List<ChatChoice>?)

interface GroqApi {
    @POST("openai/v1/chat/completions")
    suspend fun chat(
        @Header("Authorization") auth: String,
        @Body body: ChatRequest
    ): ChatResponse
}

/** Result wrapper so the UI can show a clean message instead of a raw exception. */
sealed class AiResult {
    data class Ok(val text: String) : AiResult()
    data class Err(val message: String) : AiResult()
}

object GroqClient {
    private const val BASE = "https://api.groq.com/"

    private val api: GroqApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val http = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
        Retrofit.Builder()
            .baseUrl(BASE)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GroqApi::class.java)
    }

    /**
     * Generates a reply DRAFT. It never sends anything itself — the draft is
     * returned to the UI for the user to review, edit, copy or share.
     */
    suspend fun draftReply(
        apiKey: String,
        model: String,
        incomingText: String,
        personName: String,
        tone: String
    ): AiResult {
        if (apiKey.isBlank()) {
            return AiResult.Err("Groq API key missing. Add it in Settings.")
        }
        val sys = buildString {
            append("You are a helpful assistant for the BRAHM CRM app. ")
            append("Write a short, polite reply the user can send back to a customer. ")
            append("Tone: ").append(tone).append(". ")
            append("Keep it under 60 words. Reply in the same language as the incoming message ")
            append("(Hindi, Hinglish or English). Do not add quotes or labels, just the message text.")
        }
        val user = buildString {
            append("Customer name: ").append(personName.ifBlank { "Customer" }).append("\n")
            append("Their message: ").append(incomingText)
        }
        return try {
            val resp = api.chat(
                auth = "Bearer $apiKey",
                body = ChatRequest(
                    model = model,
                    messages = listOf(
                        ChatMessage("system", sys),
                        ChatMessage("user", user)
                    )
                )
            )
            val text = resp.choices?.firstOrNull()?.message?.content?.trim()
            if (text.isNullOrBlank()) AiResult.Err("Empty response from AI.")
            else AiResult.Ok(text)
        } catch (e: retrofit2.HttpException) {
            AiResult.Err("AI error ${e.code()} — check API key / model in Settings.")
        } catch (e: Exception) {
            AiResult.Err(e.message ?: "Network error. Check internet.")
        }
    }
}
