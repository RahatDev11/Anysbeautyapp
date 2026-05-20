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
    ACCOUNT,
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

    // --- User Authentication and Account States ---
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _loggedInUser = MutableStateFlow<UserProfile?>(null)
    val loggedInUser: StateFlow<UserProfile?> = _loggedInUser.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _authError = MutableStateFlow("")
    val authError: StateFlow<String> = _authError.asStateFlow()

    private val _userOrders = MutableStateFlow<List<Order>>(emptyList())
    val userOrders: StateFlow<List<Order>> = _userOrders.asStateFlow()

    private val _isUserOrdersLoading = MutableStateFlow(false)
    val isUserOrdersLoading: StateFlow<Boolean> = _isUserOrdersLoading.asStateFlow()

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
        loadUserSession()
    }

    private fun loadUserSession() {
        val savedPhone = sharedPrefs.getString("user_phone", "") ?: ""
        val savedName = sharedPrefs.getString("user_name", "") ?: ""
        val savedEmail = sharedPrefs.getString("user_email", "") ?: ""
        val savedAddress = sharedPrefs.getString("user_address", "") ?: ""
        
        if (savedPhone.isNotBlank()) {
            _isLoggedIn.value = true
            _loggedInUser.value = UserProfile(
                phone = savedPhone,
                name = savedName,
                email = savedEmail,
                address = savedAddress
            )
            autofillCheckoutFromProfile()
        }
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
                val user = _loggedInUser.value
                val order = Order(
                    customerName = customerName.value,
                    phoneNumber = phoneNumber.value,
                    address = address.value,
                    deliveryLocation = if (deliveryLocation.value == "insideDhaka") "ঢাকা সিটির ভেতরে" else "ঢাকা সিটির বাইরে",
                    deliveryFee = deliveryFee.value,
                    subTotal = subtotal.value,
                    totalAmount = totalAmount.value,
                    cartItems = _cart.value,
                    userId = user?.phone?.let { "USER_$it" } ?: ("GUEST_" + System.currentTimeMillis()),
                    customerEmail = user?.email?.ifBlank { "guest@checkout.com" } ?: "guest@checkout.com",
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

                // Autofill again if they are logged in!
                autofillCheckoutFromProfile()

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
            val user = _loggedInUser.value
            val userEmail = user?.email ?: ""
            val userPhone = user?.phone ?: ""
            val list = repository.fetchOrdersForUser("", userEmail, userPhone, _guestOrderIds.value)
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

    // --- User Authentication / Account Methods ---

    fun registerUser(
        name: String,
        phone: String,
        email: String,
        address: String,
        pass: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = ""
            try {
                val cleanedName = name.trim()
                val cleanedPhone = phone.trim().replace(" ", "").replace("-", "")
                val cleanedEmail = email.trim()
                val cleanedAddress = address.trim()
                val cleanedPass = pass.trim()

                if (cleanedName.isBlank() || cleanedPhone.isBlank() || cleanedPass.isBlank()) {
                    _isAuthLoading.value = false
                    onFailure("অনুগ্রহ করে নাম, মোবাইল নম্বর এবং পাসওয়ার্ড দিন।")
                    return@launch
                }

                if (cleanedPhone.length < 11) {
                    _isAuthLoading.value = false
                    onFailure("সঠিক ১১ ডিজিটের মোবাইল নম্বরটি দিন।")
                    return@launch
                }

                // Check if user already exists
                val existing = repository.getUserProfile(cleanedPhone)
                if (existing != null) {
                    _isAuthLoading.value = false
                    onFailure("এই মোবাইল নম্বর দিয়ে ইতিমধ্যে একটি অ্যাকাউন্ট তৈরি করা হয়েছে! অনুগ্রহ করে লগইন করুন।")
                    return@launch
                }

                val profile = UserProfile(
                    phone = cleanedPhone,
                    name = cleanedName,
                    email = cleanedEmail,
                    address = cleanedAddress,
                    passwordHash = cleanedPass
                )

                val saved = repository.saveUserProfile(cleanedPhone, profile)
                if (saved) {
                    saveSessionLocally(profile)
                    _isAuthLoading.value = false
                    onSuccess()
                } else {
                    _isAuthLoading.value = false
                    onFailure("সার্ভার ত্রুটি, অনুগ্রহ করে আবার চেষ্টা করুন।")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isAuthLoading.value = false
                onFailure("সংযোগ ত্রুটি: ${e.localizedMessage}")
            }
        }
    }

    fun loginUser(phone: String, pass: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = ""
            try {
                val cleanedPhone = phone.trim().replace(" ", "").replace("-", "")
                val cleanedPass = pass.trim()

                if (cleanedPhone.isBlank() || cleanedPass.isBlank()) {
                    _isAuthLoading.value = false
                    onFailure("মোবাইল নম্বর এবং পাসওয়ার্ড প্রদান করুন।")
                    return@launch
                }

                val profile = repository.getUserProfile(cleanedPhone)
                if (profile == null) {
                    _isAuthLoading.value = false
                    onFailure("এই মোবাইল নম্বর সম্বলিত কোনো অ্যাকাউন্ট পাওয়া যায়নি।")
                    return@launch
                }

                if (profile.passwordHash != cleanedPass) {
                    _isAuthLoading.value = false
                    onFailure("ভুল পাসওয়ার্ড! অনুগ্রহ করে সঠিক পাসওয়ার্ড দিন।")
                    return@launch
                }

                saveSessionLocally(profile)
                _isAuthLoading.value = false
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                _isAuthLoading.value = false
                onFailure("সংযোগ ত্রুটি: ${e.localizedMessage}")
            }
        }
    }

    fun updateProfile(name: String, email: String, address: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            val currentProfile = _loggedInUser.value
            if (currentProfile == null) {
                _isAuthLoading.value = false
                onFailure("আপনি লগইন অবস্থায় নেই।")
                return@launch
            }

            try {
                val updated = currentProfile.copy(
                    name = name.trim(),
                    email = email.trim(),
                    address = address.trim()
                )

                val saved = repository.saveUserProfile(updated.phone, updated)
                if (saved) {
                    saveSessionLocally(updated)
                    _isAuthLoading.value = false
                    onSuccess()
                } else {
                    _isAuthLoading.value = false
                    onFailure("প্রোফাইল আপডেট করা যায়নি। সার্ভার ত্রুটি।")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isAuthLoading.value = false
                onFailure("সংযোগ ত্রুটি: ${e.localizedMessage}")
            }
        }
    }

    fun logout() {
        sharedPrefs.edit()
            .remove("user_phone")
            .remove("user_name")
            .remove("user_email")
            .remove("user_address")
            .apply()

        _isLoggedIn.value = false
        _loggedInUser.value = null
        _userOrders.value = emptyList()

        customerName.value = ""
        phoneNumber.value = ""
        address.value = ""
    }

    private fun saveSessionLocally(profile: UserProfile) {
        sharedPrefs.edit()
            .putString("user_phone", profile.phone)
            .putString("user_name", profile.name)
            .putString("user_email", profile.email)
            .putString("user_address", profile.address)
            .apply()

        _isLoggedIn.value = true
        _loggedInUser.value = profile
        autofillCheckoutFromProfile()
        loadUserOrders()
    }

    fun autofillCheckoutFromProfile() {
        val user = _loggedInUser.value
        if (user != null) {
            customerName.value = user.name
            phoneNumber.value = user.phone
            address.value = user.address
        }
    }

    fun loadUserOrders() {
        val user = _loggedInUser.value
        if (user != null) {
            viewModelScope.launch {
                _isUserOrdersLoading.value = true
                val list = repository.fetchOrdersForUser("", user.email, user.phone, _guestOrderIds.value)
                _userOrders.value = list
                _isUserOrdersLoading.value = false
            }
        }
    }
}
