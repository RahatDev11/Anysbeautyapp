package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// --- Gemini API Models ---
@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiConfig? = null,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiConfig(
    val temperature: Float = 0.7f
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null
)

// --- Retrofit Service Interfaces ---

interface BeautyApiService {
    @GET("products.json")
    suspend fun getProducts(): Map<String, Product>?

    @GET("orders.json")
    suspend fun getAllOrders(): Map<String, Order>?

    @GET("orders/{id}.json")
    suspend fun getOrderById(@Path("id") orderId: String): Order?

    @PUT("orders/{id}.json")
    suspend fun placeOrder(@Path("id") orderId: String, @Body order: Order): Order

    @GET("counters/{date}.json")
    suspend fun getDateCounter(@Path("date") dateString: String): Int?

    @PUT("counters/{date}.json")
    suspend fun setDateCounter(@Path("date") dateString: String, @Body counter: Int): Int

    @GET("users/{phone}.json")
    suspend fun getUserProfile(@Path("phone") phone: String): UserProfile?

    @PUT("users/{phone}.json")
    suspend fun createOrUpdateUser(@Path("phone") phone: String, @Body profile: UserProfile): UserProfile
}

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// --- Retrofit & Moshi Client setup ---

object ServiceLocator {
    private const val FIREBASE_BASE_URL = "https://nahid-6714-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val firebaseRetrofit = Retrofit.Builder()
        .baseUrl(FIREBASE_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val geminiRetrofit = Retrofit.Builder()
        .baseUrl(GEMINI_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val beautyService: BeautyApiService by lazy {
        firebaseRetrofit.create(BeautyApiService::class.java)
    }

    val geminiService: GeminiApiService by lazy {
        geminiRetrofit.create(GeminiApiService::class.java)
    }
}

// --- Beauty Repository ---

class BeautyRepository {
    private val api = ServiceLocator.beautyService
    private val gemini = ServiceLocator.geminiService

    // Fetch catalog products from Firebase Realtime Database
    suspend fun fetchProducts(): List<Product> {
        return try {
            val response = api.getProducts()
            response?.map { (key, product) ->
                product.copy(id = key)
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Fetch order by ID from DB
    suspend fun trackOrder(orderId: String): Order? {
        return try {
            api.getOrderById(orderId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Fetch orders matching specific custom user email, user ID, or phone number
    suspend fun fetchOrdersForUser(userId: String, email: String, phone: String, localOrders: List<String>): List<Order> {
        return try {
            val allOrders = api.getAllOrders() ?: return emptyList()
            allOrders.values.filter { order ->
                (userId.isNotBlank() && order.userId == userId) || 
                (email.isNotBlank() && order.customerEmail.lowercase() == email.lowercase()) || 
                (phone.isNotBlank() && order.phoneNumber.replace(" ", "") == phone.replace(" ", "")) ||
                localOrders.contains(order.orderId)
            }.sortedByDescending { it.orderDate }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Remote authentication profile database helpers
    suspend fun getUserProfile(phone: String): UserProfile? {
        return try {
            api.getUserProfile(phone)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveUserProfile(phone: String, profile: UserProfile): Boolean {
        return try {
            api.createOrUpdateUser(phone, profile)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Place order with sequential, generated Order ID like yyddMM001
    suspend fun createOrder(order: Order): String {
        val today = Date()
        val yearS = SimpleDateFormat("yy", Locale.US).format(today)
        val dayS = SimpleDateFormat("dd", Locale.US).format(today)
        val monthS = SimpleDateFormat("MM", Locale.US).format(today)
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(today)

        // Generate unique incremental order index for today
        var orderIndex = 1
        try {
            val currentCounter = api.getDateCounter(dateString)
            if (currentCounter != null) {
                orderIndex = currentCounter + 1
            }
            api.setDateCounter(dateString, orderIndex)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: Use some random padding if server counter fails
            orderIndex = Random().nextInt(800) + 100
        }

        val paddedIdx = orderIndex.toString().padStart(3, '0')
        val generatedOrderId = "$yearS$dayS$monthS$paddedIdx" // e.g. "262005001"

        val completeOrder = order.copy(
            orderId = generatedOrderId,
            orderDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(today),
            status = "processing"
        )

        // Write order into /orders/{orderId}.json
        api.placeOrder(generatedOrderId, completeOrder)
        return generatedOrderId
    }

    // Chat with Gemini AI Assistant
    suspend fun chatWithAssistant(prompt: String, conversationHistory: List<ChatMessage>): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return "দুঃখিত, এআই ফিচারটি সচল করার জন্য ব্রাউজারের গুগল এআই স্টুডিও সিক্রেট প্যানেলে GEMINI_API_KEY প্রদান করুন।"
        }

        // Map ChatMessage structure to Gemini REST format
        val mappedContents = conversationHistory.map { msg ->
            GeminiContent(
                role = if (msg.role == "user") "user" else "model",
                parts = listOf(GeminiPart(text = msg.text))
            )
        } + GeminiContent(
            role = "user",
            parts = listOf(GeminiPart(text = prompt))
        )

        val sysInstruction = GeminiContent(
            role = "user",
            parts = listOf(GeminiPart(text = """
                You are Any's Beauty Assistant. You help customers with beauty tips, skincare advice, cosmetics recommendations, and beauty order inquiries. 
                Keep your answers highly professional, polite, warm, and helpful. ALWAYS speak in Bengali language based in Bangladesh (Dhaka tone).
                Recommend products by name that are usually associated with Any's Beauty Corner (such as Kashmiri Mehandi, Prem Dulhan, Natural Health Supplement, Chocolate Shake, Saffron Goat Milk soap, Natural Food Almond Badam Shake, and Keto Green Coffee).
            """.trimIndent()))
        )

        val request = GeminiRequest(
            contents = mappedContents,
            generationConfig = GeminiConfig(temperature = 0.7f),
            systemInstruction = sysInstruction
        )

        return try {
            val response = gemini.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "আমি এই মুহূর্তে এটি বুঝতে পারছি না, অনুগ্রহ করে আবার চেষ্টা করুন।"
        } catch (e: Exception) {
            e.printStackTrace()
            "দুঃখিত, সংযোগে কিছু সমস্যা দেখা দিয়েছে। দয়া করে কিছুক্ষণ পর আবার চেষ্টা করুন। (${e.localizedMessage})"
        }
    }

    // Quick tips retrieval in Bengali via Gemini
    suspend fun generateBeautyTips(topic: String): String {
        return chatWithAssistant("Provide 3 important beauty tips specifically for $topic in a list format.", emptyList())
    }
}
