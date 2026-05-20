package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Product(
    val id: String = "",
    val name: String = "",
    val price: Any = "", // Can be String ("350") or Double/Int (1350)
    val originalPrice: String? = null,
    val image: String = "",
    val category: String = "",
    val description: String = "",
    val stockStatus: String = "in_stock",
    val tags: String = "",
    val isInSlider: Boolean = false,
    val sliderOrder: Int? = null
) {
    // Helper to get price as double cleanly
    fun getPriceDouble(): Double {
        return when (val p = price) {
            is Number -> p.toDouble()
            is String -> p.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    // Helper to get price as formatted string
    fun getFormattedPrice(): String {
        val calculated = getPriceDouble()
        return if (calculated % 1.0 == 0.0) {
            calculated.toInt().toString()
        } else {
            String.format("%.2f", calculated)
        }
    }

    // Split Cloudinary image list by comma and clean up whitespace
    fun getImageList(): List<String> {
        if (image.isBlank()) return listOf("https://via.placeholder.com/400")
        return image.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}

@JsonClass(generateAdapter = true)
data class CartItem(
    val product: Product,
    val quantity: Int = 1
)

@JsonClass(generateAdapter = true)
data class Order(
    val orderId: String = "",
    val customerName: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val deliveryLocation: String = "", // "insideDhaka" or "outsideDhaka" (stored raw or formatted in Bengali)
    val deliveryFee: Double = 70.0,
    val subTotal: Double = 0.0,
    val totalAmount: Double = 0.0,
    val cartItems: List<CartItem> = emptyList(),
    val orderDate: String = "",
    val status: String = "processing", // "processing", "confirmed", "packaging", "shipped", "delivered"
    val userId: String = "",
    val customerEmail: String = "guest@checkout.com",
    val deliveryNote: String = "N/A",
    val outsideDhakaLocation: String = "N/A",
    val paymentNumber: String = "N/A",
    val transactionId: String = "N/A"
)

data class ChatMessage(
    val role: String, // "user" or "model" / "assistant"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
