package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class Screen {
    CATALOG,
    PRODUCT_DETAIL,
    CART,
    CHECKOUT,
    TRACK_ORDER,
    CHAT_AI
}

class BeautyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BeautyRepository()
    private val sharedPrefs = application.getSharedPreferences("anybeauty_prefs", Context.MODE_PRIVATE)

    // --- Navigation Flow ---
    private val screenStack = mutableListOf(Screen.CATALOG)
    private val _currentScreen = MutableStateFlow(Screen.CATALOG)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // --- Catalog and Shopping States ---
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _activeCategory = MutableStateFlow("all")
    val activeCategory: StateFlow<String> = _activeCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    // --- Cart States ---
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    // --- Shipping / Checkout Form States ---
    val customerName = MutableStateFlow("")
    val phoneNumber = MutableStateFlow("")
    val address = MutableStateFlow("")
    val deliveryLocation = MutableStateFlow("insideDhaka") // "insideDhaka" | "outsideDhaka"
    val outsideDhakaLocation = MutableStateFlow("")
    val deliveryPaymentMethod = MutableStateFlow("") // "bkash" | "nagad"
    val paymentNumber = MutableStateFlow("")
    val transactionId = MutableStateFlow("")
    val deliveryNote = MutableStateFlow("")

    private val _isOrdering = MutableStateFlow(false)
    val isOrdering: StateFlow<Boolean> = _isOrdering.asStateFlow()

    // --- Order Tracking States ---
    private val _guestOrderIds = MutableStateFlow<List<String>>(emptyList())
    private val _trackedOrders = MutableStateFlow<List<Order>>(emptyList())
    val trackedOrders: StateFlow<List<Order>> = _trackedOrders.asStateFlow()

    private val _selectedTrackingOrder = MutableStateFlow<Order?>(null)
    val selectedTrackingOrder: StateFlow<Order?> = _selectedTrackingOrder.asStateFlow()

    private val _isTrackingLoading = MutableStateFlow(false)
    val isTrackingLoading: StateFlow<Boolean> = _isTrackingLoading.asStateFlow()

    val trackSearchQuery = MutableStateFlow("")

    // --- Chat AI (Beauty Advisor) States ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("model", "আসসালামু আলাইকুম! আমি Any's Beauty Assistant। আপনাকে ব্রাইটেনিং, হেয়ারকেয়ার বা রূপচর্চা নিয়ে কীভাবে সাহায্য করতে পারি? যেকোনো টিপস বা প্রোডাক্ট সম্পর্কে জানতে আমাকে জিজ্ঞেস করুন।")
    ))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // --- Computed State Flows ---
    val filteredProducts: StateFlow<List<Product>> = combine(
        _products, _activeCategory, _searchQuery
    ) { prodList, cat, query ->
        var result = prodList

        // Filter by category
        if (cat != "all") {
            result = result.filter { product ->
                // Check category tag or name
                product.category.lowercase() == cat.lowercase() ||
                (cat == "skincare" && (product.category.lowercase() == "skincare" || product.category.lowercase() == "health")) ||
                (cat == "cosmetics" && (product.category.lowercase() == "cosmetics" || product.category.lowercase() == "mehandi")) ||
                (cat == "haircare" && product.category.lowercase() == "haircare")
            }
        }

        // Filter by search query
        if (query.isNotBlank()) {
            result = result.filter { product ->
                product.name.lowercase().contains(query.lowercase()) ||
                product.description.lowercase().contains(query.lowercase()) ||
                product.tags.lowercase().contains(query.lowercase())
            }
        }

        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartCount: StateFlow<Int> = _cart.map { list ->
        list.sumOf { it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val subtotal: StateFlow<Double> = _cart.map { list ->
        list.sumOf { it.product.getPriceDouble() * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val deliveryFee: StateFlow<Double> = deliveryLocation.map { loc ->
        if (loc == "outsideDhaka") 160.0 else 70.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 70.0)

    val totalAmount: StateFlow<Double> = combine(subtotal, deliveryFee) { sub, fee ->
        sub + fee
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 70.0)

    init {
        loadCatalog()
        loadLocalOrderTrackers()
    }

    // --- Action Methods ---

    fun navigateTo(screen: Screen) {
        if (screenStack.lastOrNull() != screen) {
            screenStack.add(screen)
        }
        _currentScreen.value = screen
    }

    fun navigateBack(): Boolean {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.size - 1)
            _currentScreen.value = screenStack.last()
            return true
        }
        return false // Exits app if stack is empty
    }

    fun loadCatalog() {
        viewModelScope.launch {
            val fetched = repository.fetchProducts()
            _products.value = fetched
        }
    }

    fun selectProduct(product: Product) {
        _selectedProduct.value = product
        navigateTo(Screen.PRODUCT_DETAIL)
    }

    fun setCategory(category: String) {
        _activeCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Cart Actions ---

    fun addToCart(product: Product, quantity: Int = 1) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }
        if (index != -1) {
            val existing = currentList[index]
            currentList[index] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            currentList.add(CartItem(product, quantity))
        }
        _cart.value = currentList
    }

    fun updateCartQuantity(productId: String, amount: Int) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index != -1) {
            val existing = currentList[index]
            val newQuantity = existing.quantity + amount
            if (newQuantity > 0) {
                currentList[index] = existing.copy(quantity = newQuantity)
            } else {
                currentList.removeAt(index)
            }
        }
        _cart.value = currentList
    }

    fun removeFromCart(productId: String) {
        val currentList = _cart.value.toMutableList()
        currentList.removeAll { it.product.id == productId }
        _cart.value = currentList
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    // --- Order & Placement Actions ---

    private fun loadLocalOrderTrackers() {
        val idsString = sharedPrefs.getString("guest_orders", "") ?: ""
        if (idsString.isNotBlank()) {
            val list = idsString.split(",").filter { it.isNotBlank() }
            _guestOrderIds.value = list
        }
    }

    private fun saveLocalOrderTracker(orderId: String) {
        val updatedList = _guestOrderIds.value.toMutableList()
        if (!updatedList.contains(orderId)) {
            updatedList.add(orderId)
            _guestOrderIds.value = updatedList
            sharedPrefs.edit().putString("guest_orders", updatedList.joinToString(",")).apply()
        }
    }

    fun submitCheckoutOrder(onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            if (_cart.value.isEmpty()) {
                onFailure("কার্টটি খালি! অনুগ্রহ করে কিছু পণ্য যোগ করুন।")
                return@launch
            }
            if (customerName.value.isBlank() || phoneNumber.value.isBlank() || address.value.isBlank()) {
                onFailure("দয়া করে নাম, মোবাইল নম্বর এবং পূর্ণ ঠিকানা প্রদান করুন।")
                return@launch
            }
            if (deliveryLocation.value == "outsideDhaka") {
                if (outsideDhakaLocation.value.isBlank() || deliveryPaymentMethod.value.isBlank() ||
                    paymentNumber.value.isBlank() || transactionId.value.isBlank()) {
                    onFailure("ঢাকার বাইরের অর্ডারের জন্য অগ্রিম চার্জ পরিশোধের বিবরণসমূহ আবশ্যক।")
                    return@launch
                }
            }

            _isOrdering.value = true
            try {
                val order = Order(
                    customerName = customerName.value,
                    phoneNumber = phoneNumber.value,
                    address = address.value,
                    deliveryLocation = if (deliveryLocation.value == "insideDhaka") "ঢাকা সিটির ভেতরে" else "ঢাকা সিটির বাইরে",
                    deliveryFee = deliveryFee.value,
                    subTotal = subtotal.value,
                    totalAmount = totalAmount.value,
                    cartItems = _cart.value,
                    userId = "GUEST_" + System.currentTimeMillis(),
                    customerEmail = "guest@checkout.com",
                    deliveryNote = deliveryNote.value.ifBlank { "N/A" },
                    outsideDhakaLocation = if (deliveryLocation.value == "outsideDhaka") outsideDhakaLocation.value else "N/A",
                    paymentNumber = if (deliveryLocation.value == "outsideDhaka") paymentNumber.value else "N/A",
                    transactionId = if (deliveryLocation.value == "outsideDhaka") transactionId.value else "N/A"
                )

                val confirmedId = repository.createOrder(order)
                saveLocalOrderTracker(confirmedId)
                clearCart()

                // Reset checkout info fields
                customerName.value = ""
                phoneNumber.value = ""
                address.value = ""
                outsideDhakaLocation.value = ""
                paymentNumber.value = ""
                transactionId.value = ""
                deliveryNote.value = ""

                // Fetch tracking orders updated list
                syncTrackingOrders()

                _isOrdering.value = false
                onSuccess(confirmedId)
            } catch (e: Exception) {
                e.printStackTrace()
                _isOrdering.value = false
                onFailure("অর্ডারটি সম্পন্ন করা সম্ভব হয়নি। দয়া করে ইন্টারনেট সংযোগ চেক করুন। (${e.localizedMessage})")
            }
        }
    }

    // --- Order Tracking Actions ---

    fun syncTrackingOrders() {
        viewModelScope.launch {
            _isTrackingLoading.value = true
            val list = repository.fetchOrdersForUser("", "", _guestOrderIds.value)
            _trackedOrders.value = list
            _isTrackingLoading.value = false
        }
    }

    fun searchAndSelectOrder(orderId: String, onFound: () -> Unit, onNotFound: () -> Unit) {
        viewModelScope.launch {
            _isTrackingLoading.value = true
            val fetched = repository.trackOrder(orderId)
            _isTrackingLoading.value = false
            if (fetched != null) {
                _selectedTrackingOrder.value = fetched
                onFound()
            } else {
                onNotFound()
            }
        }
    }

    fun selectTrackingOrder(order: Order) {
        _selectedTrackingOrder.value = order
    }

    fun closeOrderModal() {
        _selectedTrackingOrder.value = null
    }

    // --- Assistant AI Conversational Actions ---

    fun sendMessageToAI(query: String) {
        if (query.isBlank()) return
        val userMsg = ChatMessage("user", query)
        val currentMsgs = _chatMessages.value + userMsg
        _chatMessages.value = currentMsgs

        viewModelScope.launch {
            _isAiLoading.value = true
            val responseText = repository.chatWithAssistant(query, currentMsgs.dropLast(1))
            val modelMsg = ChatMessage("model", responseText)
            _chatMessages.value = _chatMessages.value + modelMsg
            _isAiLoading.value = false
        }
    }

    fun getBeautyTips(topic: String) {
        val currentMsgs = _chatMessages.value + ChatMessage("user", "$topic নিয়ে টিপস দিন।")
        _chatMessages.value = currentMsgs

        viewModelScope.launch {
            _isAiLoading.value = true
            val tips = repository.generateBeautyTips(topic)
            _chatMessages.value = _chatMessages.value + ChatMessage("model", tips)
            _isAiLoading.value = false
        }
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            ChatMessage("model", "এআই চ্যাট হিস্ট্রি মুছে ফেলা হয়েছে। নতুন কী জানতে চান বলুন!")
        )
    }
}
