package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.CartItem
import com.example.data.Order
import com.example.data.Product
import com.example.data.ChatMessage
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BeautyAppContent(viewModel: BeautyViewModel) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val cartCount by viewModel.cartCount.collectAsState()

    // Handle system back press
    BackHandler(enabled = currentScreen != Screen.CATALOG) {
        viewModel.navigateBack()
    }

    Scaffold(
        topBar = { BeautyHeader(viewModel = viewModel, cartCount = cartCount) },
        bottomBar = { BeautyBottomNavBar(viewModel = viewModel, currentScreen = currentScreen) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    slideInHorizontally { width -> width / 3 } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width / 3 } + fadeOut()
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    Screen.CATALOG -> CatalogScreen(viewModel = viewModel)
                    Screen.PRODUCT_DETAIL -> ProductDetailScreen(viewModel = viewModel)
                    Screen.CART -> CartScreen(viewModel = viewModel)
                    Screen.CHECKOUT -> CheckoutScreen(viewModel = viewModel)
                    Screen.TRACK_ORDER -> OrderTrackScreen(viewModel = viewModel)
                    else -> {}
                }
            }
        }
    }
}

// --- Visual App Header ---

@Composable
fun BeautyHeader(viewModel: BeautyViewModel, cartCount: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Name & Branding Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.navigateTo(Screen.CATALOG) }
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(LipstickPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Any's Beauty Corner",
                        color = LipstickPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = FontStyle.Italic
                    )
                    Text(
                        text = "সৌন্দর্য চর্চার বিশ্বস্ত সঙ্গী",
                        color = BeautyOnSurface.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Quick Access Shopping Cart Chip with Badge
            IconButton(
                onClick = { viewModel.navigateTo(Screen.CART) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("cart_button")
            ) {
                BadgedBox(
                    badge = {
                        if (cartCount > 0) {
                            Badge(
                                containerColor = LipstickAccent,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = cartCount.toString(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
                        contentDescription = "Shopping Cart",
                        tint = LipstickPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

// --- Persistent Styled Bottom Menu ---

@Composable
fun BeautyBottomNavBar(viewModel: BeautyViewModel, currentScreen: Screen) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        NavigationBarItem(
            selected = currentScreen == Screen.CATALOG || currentScreen == Screen.PRODUCT_DETAIL,
            onClick = { viewModel.navigateTo(Screen.CATALOG) },
            label = { Text("হোম", fontWeight = FontWeight.Bold) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == Screen.CATALOG) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Home"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = LipstickPrimary,
                selectedTextColor = LipstickPrimary,
                indicatorColor = LipstickLight
            )
        )

        NavigationBarItem(
            selected = currentScreen == Screen.TRACK_ORDER,
            onClick = { viewModel.navigateTo(Screen.TRACK_ORDER) },
            label = { Text("অর্ডার ট্রাক", fontWeight = FontWeight.Bold) },
            modifier = Modifier.testTag("track_order_button"),
            icon = {
                Icon(
                    imageVector = if (currentScreen == Screen.TRACK_ORDER) Icons.Filled.LocalShipping else Icons.Outlined.LocalShipping,
                    contentDescription = "Track Order"
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = LipstickPrimary,
                selectedTextColor = LipstickPrimary,
                indicatorColor = LipstickLight
            )
        )
    }
}

// ============================================
// # SCREEN 1: CATALOG SCREEN (HomePage Carousel Grid)
// ============================================

@Composable
fun CatalogScreen(viewModel: BeautyViewModel) {
    val productsList by viewModel.filteredProducts.collectAsState()
    val rawProductsList by viewModel.products.collectAsState()
    val activeCategory by viewModel.activeCategory.collectAsState()
    val searchInput by viewModel.searchQuery.collectAsState()

    // Slider active products (isInSlider == true)
    val sliderProducts = remember(rawProductsList) {
        rawProductsList.filter { it.isInSlider }.sortedBy { it.sliderOrder ?: 99 }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 30.dp)
    ) {
        // Search Banner
        item {
            ProductSearchBar(
                query = searchInput,
                onQueryChange = { viewModel.setSearchQuery(it) }
            )
        }

        // Active Event Carousel Banner
        if (sliderProducts.isNotEmpty() && searchInput.isBlank() && activeCategory == "all") {
            item {
                EventPromoSlider(sliderProducts) { product ->
                    viewModel.selectProduct(product)
                }
            }
        }

        // Beauty Categories Horizontal Filter Chips
        item {
            CategoryFilterBar(
                selectedCategory = activeCategory,
                onCategorySelect = { viewModel.setCategory(it) }
            )
        }

        // Dynamic Product Collections Title
        item {
            val titleText = when (activeCategory) {
                "all" -> "সকল কালেকশন"
                "skincare" -> "উজ্জ্বল স্কিনকেয়ার"
                "cosmetics" -> "অনন্য মেকআপ ও মেহেমান"
                "haircare" -> "কেশ রূপচর্চা"
                else -> "পছন্দের কালেকশন"
            }
            Text(
                text = titleText,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = LipstickDark,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        // Product Items Grid (Inline nested items)
        if (productsList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, bottom = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = "No products found",
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "কোনো প্রোডাক্ট পাওয়া যায়নি!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BeautyOnSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "আপনার সার্চের সাথে মিলে যায় এমন কোনো পণ্য বর্তমানে নেই।",
                            fontSize = 12.sp,
                            color = BeautyOnSurface.copy(alpha = 0.4f),
                            modifier = Modifier.padding(horizontal = 30.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Display products chunks as 2-column list for extreme scroll performance and zero lag
            val chunked = productsList.chunked(2)
            items(chunked) { rowProducts ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (product in rowProducts) {
                        ProductCard(
                            product = product,
                            onCardClick = { viewModel.selectProduct(product) },
                            onOrderClick = {
                                viewModel.addToCart(product, 1)
                                viewModel.navigateTo(Screen.CHECKOUT)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowProducts.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Brand Footer Section (Matching Website Theme)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(1.dp, LipstickAccent.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Any's Beauty Corner",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = LipstickPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "আপনার সৌন্দর্য চর্চার বিশ্বস্ত সঙ্গী। আমরা বিশ্বাস করি প্রতিটি মানুষের ত্বকের যত্ন নেওয়া প্রয়োজন সঠিক ও আসল প্রোডাক্ট দিয়ে।",
                    fontSize = 12.sp,
                    color = BeautyOnSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = LipstickAccent.copy(alpha = 0.15f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Contact Details Columns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Contact Info 1: Address
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Address",
                            tint = LipstickPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "মিরপুর ১০, ঢাকা",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BeautyOnSurface
                        )
                    }

                    // Contact Info 2: Phone
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Phone",
                            tint = LipstickPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "+880 1931-866636",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BeautyOnSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "© 2026 Any's Beauty Corner • Created with dedication",
                    fontSize = 10.sp,
                    color = BeautyOnSurface.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Inner Component: Search text bar
@Composable
fun ProductSearchBar(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("ত্বকের উজ্জ্বলতা, সাবান বা পছন্দের কফি খুঁজুন...", fontSize = 13.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", tint = LipstickPrimary) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Color.Gray)
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = LipstickPrimary,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(56.dp)
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .testTag("search_input")
    )
}

// Inner Component: Auto scrolling/Timer dynamic hero slider
@Composable
fun EventPromoSlider(products: List<Product>, onProductClick: (Product) -> Unit) {
    var activeIdx by remember { mutableStateOf(0) }

    LaunchedEffect(key1 = products) {
        while (true) {
            delay(4000)
            activeIdx = (activeIdx + 1) % products.size
        }
    }

    val activeProduct = products.getOrNull(activeIdx) ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Text(
            text = "ইভেন্ট ও অফার",
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = LipstickPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 16.dp)
                .clickable { onProductClick(activeProduct) },
            colors = CardDefaults.cardColors(containerColor = LipstickPrimary)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background cloud image loaders
                AsyncImage(
                    model = activeProduct.getImageList().firstOrNull(),
                    contentDescription = "Event banner image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // High contrast aesthetic gradient shading
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )

                // Offer floating texts inside slider
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(LipstickAccent, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "আজকের ধামাকা অফার!",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = activeProduct.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "৳ " + activeProduct.getFormattedPrice() + " টাকা মাত্র! (স্টকে আছে)",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Slider indicator dots
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    products.forEachIndexed { idx, _ ->
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (idx == activeIdx) LipstickAccent else Color.White.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }
    }
}

// Inner Component: Beauty Categories Filters Bar (skincare, cosmetics, skincare/health)
@Composable
fun CategoryFilterBar(selectedCategory: String, onCategorySelect: (String) -> Unit) {
    val categories = listOf(
        "all" to "সকল কালেকশন",
        "skincare" to "স্কিনকেয়ার",
        "cosmetics" to "মেকআপ ও মেহেদি",
        "haircare" to "হেয়ারকেয়ার"
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(categories) { (id, label) ->
            val isActive = selectedCategory == id
            Button(
                onClick = { onCategorySelect(id) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) LipstickPrimary else MaterialTheme.colorScheme.surface,
                    contentColor = if (isActive) Color.White else BeautyOnSurface
                ),
                shape = RoundedCornerShape(12.dp),
                border = if (!isActive) BorderStroke(1.dp, BeautyGrayBorder) else null,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Inner Component: Individual cosmetics product grid display standard M3 Card
@Composable
fun ProductCard(
    product: Product,
    onCardClick: () -> Unit,
    onOrderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .padding(8.dp)
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .clickable { onCardClick() }
            .testTag("product_item_card_${product.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                // Image
                AsyncImage(
                    model = product.getImageList().firstOrNull(),
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Stock details tag
                val inStock = product.stockStatus == "in_stock"
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                        .background(
                            if (inStock) LipstickPrimary.copy(alpha = 0.9f) else Color.Gray,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (inStock) "স্টকে আছে" else "স্টক শেষ",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                // Name
                Text(
                    text = product.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BeautyOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Tags
                Text(
                    text = product.tags.ifBlank { "রোজ গার্ডেন রূপচর্চা" },
                    fontSize = 10.sp,
                    color = BeautyGrayText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Value price listing
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "৳ ${product.getFormattedPrice()}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = LipstickPrimary
                        )
                    }

                    // Fast Action Add-To-Cart Circular Target (at least 48dp target)
                    IconButton(
                        onClick = onOrderClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(LipstickLight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = "Buy item directly",
                            tint = LipstickPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================
// # SCREEN 2: PRODUCT DETAIL SCREEN
// ============================================

@Composable
fun ProductDetailScreen(viewModel: BeautyViewModel) {
    val context = LocalContext.current
    val product by viewModel.selectedProduct.collectAsState()
    val scope = rememberCoroutineScope()
    var purchaseQuantity by remember { mutableStateOf(1) }

    val activeProduct = product ?: return

    // Dynamic slideshow index
    var mainImageIdx by remember { mutableStateOf(0) }
    val imagesList = activeProduct.getImageList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Back toolbar indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LipstickPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("হোমে ফিরে যান", color = LipstickPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Slideshow main visual content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(BeautyGrayBorder.copy(alpha = 0.3f))
        ) {
            AsyncImage(
                model = imagesList.getOrNull(mainImageIdx),
                contentDescription = activeProduct.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Multiple side thumbnails indicators
            if (imagesList.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    imagesList.forEachIndexed { index, imgUrl ->
                        Card(
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(
                                2.dp,
                                if (index == mainImageIdx) LipstickAccent else Color.Transparent
                            ),
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { mainImageIdx = index }
                        ) {
                            AsyncImage(
                                model = imgUrl,
                                contentDescription = "Img thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // Cosmetic Specifications
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = activeProduct.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BeautyOnSurface,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(LipstickLight, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = activeProduct.tags.ifBlank { "অরজিনাল কসমেটিকস" },
                        fontSize = 11.sp,
                        color = LipstickDark,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (activeProduct.stockStatus == "in_stock") "স্টকে ক্যাশ অন ডেলিভারি" else "স্টক সীমিত",
                    fontSize = 12.sp,
                    color = SuccessGreen,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pricing Indicator
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "৳ " + activeProduct.getFormattedPrice(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = LipstickPrimary
                )
                Text(
                    text = " টাকা",
                    fontSize = 14.sp,
                    color = LipstickPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quantity selector wrapper
            Text(
                text = "পরিমাণ নির্ধারণ করুন:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = BeautyOnSurface.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(BeautyGrayBorder.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                IconButton(
                    onClick = { if (purchaseQuantity > 1) purchaseQuantity-- },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrement purchase quantity")
                }
                Text(
                    text = purchaseQuantity.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                IconButton(
                    onClick = { purchaseQuantity++ },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increment purchase quantity")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Description Scroll area
            Text(
                text = "পণ্যের বিবরণ :",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = LipstickDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = activeProduct.description,
                fontSize = 13.sp,
                color = BeautyOnSurface.copy(alpha = 0.8f),
                lineHeight = 22.sp,
                textAlign = TextAlign.Justify,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            HorizontalDivider(color = BeautyGrayBorder)

            Spacer(modifier = Modifier.height(20.dp))

            // Core Checkout & Direct Order Actions Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cart Addition
                OutlinedButton(
                    onClick = {
                        viewModel.addToCart(activeProduct, purchaseQuantity)
                        Toast.makeText(context, "${activeProduct.name} কার্টে যোগ হয়েছে!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("add_to_cart_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, LipstickPrimary)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = "Cart plus icon")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "কার্টে যোগ করুন",
                            fontWeight = FontWeight.Bold,
                            color = LipstickPrimary,
                            fontSize = 13.sp
                        )
                    }
                }

                // Buy/Order Directly
                Button(
                    onClick = {
                        viewModel.addToCart(activeProduct, purchaseQuantity)
                        viewModel.navigateTo(Screen.CHECKOUT)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LipstickPrimary),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(54.dp)
                        .testTag("buy_now_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = "Purchase bag", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "সরাসরি অর্ডার করুন",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

// ============================================
// # SCREEN 3: SHOPPING CART SCREEN
// ============================================

@Composable
fun CartScreen(viewModel: BeautyViewModel) {
    val cartList by viewModel.cart.collectAsState()
    val subtotal by viewModel.subtotal.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "আপনার কার্ট কন্টেন্ট",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = LipstickPrimary,
            modifier = Modifier.padding(16.dp)
        )

        if (cartList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.RemoveShoppingCart,
                        contentDescription = "Empty cart icon",
                        tint = Color.LightGray,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "আপনার কার্টটি সম্পূর্ণ খালি!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BeautyOnSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "পছন্দের মেকআপ বা স্কিনকেয়ার প্রোডাক্ট বাছাই করুন",
                        fontSize = 12.sp,
                        color = BeautyOnSurface.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.navigateTo(Screen.CATALOG) },
                        colors = ButtonDefaults.buttonColors(containerColor = LipstickPrimary)
                    ) {
                        Text("শপিং করুন", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cartList) { item ->
                    CartProductRow(
                        cartItem = item,
                        onAdd = { viewModel.updateCartQuantity(item.product.id, 1) },
                        onMinus = { viewModel.updateCartQuantity(item.product.id, -1) },
                        onDelete = { viewModel.removeFromCart(item.product.id) }
                    )
                }
            }

            // Summary Bottom checkout banner
            Surface(
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "উপমোট (Subtotal):",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "৳ ${subtotal.toInt()} টাকা",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = LipstickPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.navigateTo(Screen.CHECKOUT) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = LipstickPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "অর্ডার প্লেস করুন",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Inner Component: Cart Row card visual
@Composable
fun CartProductRow(
    cartItem: CartItem,
    onAdd: () -> Unit,
    onMinus: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = cartItem.product.getImageList().firstOrNull(),
                contentDescription = cartItem.product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(65.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, BeautyGrayBorder, RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cartItem.product.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BeautyOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "৳ " + cartItem.product.getFormattedPrice() + " x ${cartItem.quantity}",
                    fontSize = 11.sp,
                    color = LipstickPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Quantity selector bounds
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onMinus,
                        modifier = Modifier
                            .size(24.dp)
                            .background(BeautyGrayBorder.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Less", modifier = Modifier.size(12.dp))
                    }
                    Text(
                        text = cartItem.quantity.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                    IconButton(
                        onClick = onAdd,
                        modifier = Modifier
                            .size(24.dp)
                            .background(BeautyGrayBorder.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(12.dp))
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove from list",
                    tint = Color.Red.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ============================================
// # SCREEN 4: ORDER PLACEMENT FLOW (Checkout Screen)
// ============================================

@Composable
fun CheckoutScreen(viewModel: BeautyViewModel) {
    val context = LocalContext.current
    val isOrdering by viewModel.isOrdering.collectAsState()
    val deliveryFee by viewModel.deliveryFee.collectAsState()
    val subtotal by viewModel.subtotal.collectAsState()
    val totalAmount by viewModel.totalAmount.collectAsState()
    val cartList by viewModel.cart.collectAsState()

    // Form states
    val nameS by viewModel.customerName.collectAsState()
    val phoneS by viewModel.phoneNumber.collectAsState()
    val addressS by viewModel.address.collectAsState()
    val locationS by viewModel.deliveryLocation.collectAsState()
    val outsideLocS by viewModel.outsideDhakaLocation.collectAsState()
    val methodS by viewModel.deliveryPaymentMethod.collectAsState()
    val payNoS by viewModel.paymentNumber.collectAsState()
    val trxS by viewModel.transactionId.collectAsState()
    val noteS by viewModel.deliveryNote.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "চেকআউট ও অর্ডার কনফার্ম করুন",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = LipstickPrimary,
            modifier = Modifier.padding(16.dp)
        )

        // Cart Preview horizontal scrolling
        Text(
            text = "পারচেজ আইটেমসমূহ:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = LipstickDark,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(cartList) { item ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BeautyGrayBorder),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = item.product.getImageList().firstOrNull(),
                            contentDescription = item.product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = item.product.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.widthIn(max = 100.dp)
                            )
                            Text(
                                text = "${item.quantity} x ৳${item.product.getFormattedPrice()}",
                                fontSize = 9.sp,
                                color = LipstickPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Billing Details Cards
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(1.dp, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "গ্রাহকের শিপিং ঠিকানা",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BeautyOnSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Name Input
                OutlinedTextField(
                    value = nameS,
                    onValueChange = { viewModel.customerName.value = it },
                    label = { Text("গ্রাহকের নাম (আবশ্যক)*", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LipstickPrimary,
                        focusedLabelColor = LipstickPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // Phone Input (validated on submit)
                OutlinedTextField(
                    value = phoneS,
                    onValueChange = { viewModel.phoneNumber.value = it },
                    label = { Text("মোবাইল নম্বর (আবশ্যক)*", fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LipstickPrimary,
                        focusedLabelColor = LipstickPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // Address Input
                OutlinedTextField(
                    value = addressS,
                    onValueChange = { viewModel.address.value = it },
                    label = { Text("পূর্ণ ঠিকানা (আবশ্যক)*", fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LipstickPrimary,
                        focusedLabelColor = LipstickPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // Delivery Note
                OutlinedTextField(
                    value = noteS,
                    onValueChange = { viewModel.deliveryNote.value = it },
                    label = { Text("অতিরিক্ত নোট (ঐচ্ছিক)", fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LipstickPrimary,
                        focusedLabelColor = LipstickPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Delivery Location chooser Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(1.dp, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "ডেলিভারি এলাকা নির্বাচন করুন",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BeautyOnSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Inside Dhaka
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            2.dp,
                            if (locationS == "insideDhaka") LipstickAccent else Color.Transparent
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (locationS == "insideDhaka") LipstickLight else BeautyGrayBorder.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .clickable { viewModel.deliveryLocation.value = "insideDhaka" }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("ঢাকা সিটি", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LipstickDark)
                            Text("ডেলিভারি চার্জ: ৭০৳", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    // Outside Dhaka
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            2.dp,
                            if (locationS == "outsideDhaka") LipstickAccent else Color.Transparent
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (locationS == "outsideDhaka") LipstickLight else BeautyGrayBorder.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .clickable { viewModel.deliveryLocation.value = "outsideDhaka" }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("ঢাকার বাইরে", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LipstickDark)
                            Text("ডেলিভারি চার্জ: ১৬০৳", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        // Special Advance Pay Warning Box for Outside Dhaka
        if (locationS == "outsideDhaka") {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, StatusOrange.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(containerColor = StatusOrange.copy(alpha = 0.05f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "ঢাকার বাইরে অর্ডারের ক্ষেত্রে নির্দেশনা:",
                        color = StatusOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "ডেলিভারি চার্জ ১৬০ টাকা আমাদের বিকাশ বা নগদ নম্বরে সেন্ড মানি (Personal) করে নিম্নের ফরমটি পূরণ করতে হবে। তবেই আপনার অর্ডারটি কনফার্ম হবে।\n\nবিকাশ/নগদ নম্বর: ০১৯৩१८६৬৬৩৬ (পার্সোনাল)",
                        color = BeautyOnSurface.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // District / Police Station
                    OutlinedTextField(
                        value = outsideLocS,
                        onValueChange = { viewModel.outsideDhakaLocation.value = it },
                        label = { Text("জেলা বা থানা নাম", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = StatusOrange, focusedLabelColor = StatusOrange),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    )

                    // Payment method chooser
                    Text("পেমেন্ট মেথড চয়ন করুন*", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StatusOrange)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("bkash" to "বিকাশ", "nagad" to "নগদ").forEach { (methodVal, methodLabel) ->
                            val isSelected = methodS == methodVal
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) StatusOrange.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, if (isSelected) StatusOrange else BeautyGrayBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clickable { viewModel.deliveryPaymentMethod.value = methodVal }
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(methodLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) StatusOrange else Color.Gray)
                                }
                            }
                        }
                    }

                    // Payment Sender Number
                    OutlinedTextField(
                        value = payNoS,
                        onValueChange = { viewModel.paymentNumber.value = it },
                        label = { Text("যেই নম্বর থেকে সেন্ড মানি করেছেন", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = StatusOrange, focusedLabelColor = StatusOrange),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    )

                    // Transaction TrxID
                    OutlinedTextField(
                        value = trxS,
                        onValueChange = { viewModel.transactionId.value = it },
                        label = { Text("ট্রানজেকশন আইডি (TrxID)", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = StatusOrange, focusedLabelColor = StatusOrange),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Pricing review sheet on checkout
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(1.dp, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("পণ্যের উপমোট (Subtotal):", fontSize = 13.sp, color = Color.Gray)
                    Text("৳ ${subtotal.toInt()} টাকা", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("ডেলিভারি ফি:", fontSize = 13.sp, color = Color.Gray)
                    Text("+ ৳ ${deliveryFee.toInt()} টাকা", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = BeautyGrayBorder, modifier = Modifier.padding(vertical = 4.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("সর্বমোট প্রদেয় মূল্য:", fontSize = 15.sp, color = BeautyOnSurface, fontWeight = FontWeight.Bold)
                    Text("৳ ${totalAmount.toInt()} টাকা", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = LipstickPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Confirm Checkout Trigger Button
        Button(
            onClick = {
                viewModel.submitCheckoutOrder(
                    onSuccess = { orderId ->
                        Toast.makeText(context, "আপনার অর্ডারটি সফল হয়েছে! আইডি: $orderId", Toast.LENGTH_LONG).show()
                        viewModel.trackSearchQuery.value = orderId
                        viewModel.navigateTo(Screen.TRACK_ORDER)
                    },
                    onFailure = { errorText ->
                        Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            enabled = !isOrdering && cartList.isNotEmpty(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LipstickPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp)
                .testTag("submit_button")
        ) {
            if (isOrdering) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("অর্ডার প্রসেস হচ্ছে...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = "Heart check", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("অর্ডারটি কনফার্ম করুন", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ============================================
// # SCREEN 5: ORDER TRACKING SCREEN
// ============================================

@Composable
fun OrderTrackScreen(viewModel: BeautyViewModel) {
    val trackedOrders by viewModel.trackedOrders.collectAsState()
    val isTrackingLoading by viewModel.isTrackingLoading.collectAsState()
    val searchInput by viewModel.trackSearchQuery.collectAsState()
    val activeTrackedOrder by viewModel.selectedTrackingOrder.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        viewModel.syncTrackingOrders()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "গ্রাহকদের অর্ডার ট্র্যাকিং",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = LipstickPrimary,
            modifier = Modifier.padding(16.dp)
        )

        // Query Order ID bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchInput,
                onValueChange = { viewModel.trackSearchQuery.value = it },
                placeholder = { Text("যেমন: 262005001", fontSize = 12.sp) },
                label = { Text("অর্ডার আইডি সেন্ড করুন", fontSize = 11.sp) },
                singleLine = true,
                trailingIcon = {
                    if (searchInput.isNotEmpty()) {
                        IconButton(onClick = { viewModel.trackSearchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Color.Gray)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LipstickPrimary,
                    focusedLabelColor = LipstickPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (searchInput.isBlank()) {
                        Toast.makeText(context, "দয়া করে একটি সঠিক আইডি দিন।", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.searchAndSelectOrder(
                            orderId = searchInput.trim(),
                            onFound = {
                                Toast.makeText(context, "অর্ডার ট্র্যাকিং বিবরণ সফল!", Toast.LENGTH_SHORT).show()
                            },
                            onNotFound = {
                                Toast.makeText(context, "এই আইডি সম্বলিত কোনো অর্ডার পাওয়া যায়নি!", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LipstickPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Text("ট্র্যাক", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tracking orders active lists
        if (isTrackingLoading) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LipstickPrimary)
            }
        } else {
            // Interactive status timelines display modal window overlay
            if (activeTrackedOrder != null) {
                OrderTrackerModalOverlay(order = activeTrackedOrder!!, onClose = { viewModel.closeOrderModal() })
            } else {
                if (trackedOrders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Track list empty",
                                tint = Color.LightGray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "পূর্ববর্তী কোনো অর্ডার নেই",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = BeautyOnSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "এই ফোনে পূর্বে করা অর্ডারের বিবরণ দেখতে উপরে আপনার ট্র্যাক আইডি খুঁজুন।",
                                fontSize = 11.sp,
                                color = BeautyOnSurface.copy(alpha = 0.4f),
                                modifier = Modifier.padding(horizontal = 40.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Text(
                        text = "আপনার পূর্বে কৃত অর্ডারসমূহ:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = LipstickDark,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(trackedOrders) { tracked ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectTrackingOrder(tracked) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "অর্ডার আইডি: #${tracked.orderId}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = LipstickPrimary
                                        )
                                        Text(
                                            text = "অর্ডার ডেট: " + tracked.orderDate.split("T").first(),
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = "৳ ${tracked.totalAmount.toInt()} টাকা",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BeautyOnSurface
                                        )
                                    }

                                    // Dynamic badge chip status
                                    val statusBengali = when (tracked.status) {
                                        "processing" -> "প্রক্রিয়াকরণ"
                                        "confirmed" -> "কনফার্মড"
                                        "packaging" -> "প্যাকেজিং"
                                        "shipped" -> "পাঠানো হয়েছে"
                                        "delivered" -> "ডেলিভারড"
                                        else -> "প্রসেসিং"
                                    }

                                    val badgeColor = when (tracked.status) {
                                        "delivered" -> SuccessGreen
                                        "shipped" -> InfoBlue
                                        "packaging" -> PendingYellow
                                        "confirmed" -> LipstickAccent
                                        else -> StatusOrange
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(badgeColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                            .border(1.dp, badgeColor, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = statusBengali,
                                            color = badgeColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Inner Component: Overlay dialog showing detailed tracked order status and timeline
@Composable
fun OrderTrackerModalOverlay(order: Order, onClose: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .border(1.dp, BeautyGrayBorder, RoundedCornerShape(16.dp))
            .shadow(4.dp, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Modal header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ট্রেসার অর্ডার বিবরণ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = LipstickPrimary
                )
                IconButton(onClick = onClose, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Default.Close, contentDescription = "Close detailed overlay tracer")
                }
            }

            HorizontalDivider(color = BeautyGrayBorder, modifier = Modifier.padding(vertical = 8.dp))

            // Step timeline visualizer progress bar
            Text(
                text = "ডেলিভারি ট্র্যাকার কারেন্ট স্ট্যাটাস:",
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            InteractiveTimelineTracker(currentStatus = order.status)

            Spacer(modifier = Modifier.height(20.dp))

            // Details list info card
            Card(
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BeautyGrayBorder),
                colors = CardDefaults.cardColors(containerColor = BeautyBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("অর্ডার আইডি: #${order.orderId}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LipstickDark)
                    Text("তারিখ: " + order.orderDate.split("T").first(), fontSize = 11.sp)
                    Text("গ্রাহকের নাম: ${order.customerName}", fontSize = 11.sp)
                    Text("মোবাইল: ${order.phoneNumber}", fontSize = 11.sp)
                    Text("ডেলিভারি এলাকা: ${order.deliveryLocation}", fontSize = 11.sp)
                    Text("শিপিং ঠিকানা: ${order.address}", fontSize = 11.sp)
                    if (order.deliveryLocation.contains("বাইরে")) {
                        Text("বিকাশ/নগদ TxID: ${order.transactionId}", fontSize = 11.sp, color = StatusOrange, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Detailed purchased item summary list
            Text(
                text = "ক্রয়কৃত কসমেটিকস সামগ্রী:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = LipstickDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            order.cartItems.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = item.product.getImageList().firstOrNull(),
                        contentDescription = item.product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(45.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.product.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text("${item.quantity} x ৳${item.product.getFormattedPrice()}", fontSize = 10.sp, color = Color.Gray)
                    }
                    Text("৳ ${(item.quantity * item.product.getPriceDouble()).toInt()} টাকা", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = BeautyGrayBorder)

            Spacer(modifier = Modifier.height(12.dp))

            // Totals list tracer
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("ডেলিভারি ফি:", fontSize = 11.sp, color = Color.Gray)
                    Text("৳ ${order.deliveryFee.toInt()} টাকা", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("সর্বমোট প্রদেয় মূল্য:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BeautyOnSurface)
                    Text("৳ ${order.totalAmount.toInt()} টাকা", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = LipstickPrimary)
                }
            }
        }
    }
}

// Inner Component: Segment progress tracer timelines
@Composable
fun InteractiveTimelineTracker(currentStatus: String) {
    val states = listOf("processing", "confirmed", "packaging", "shipped", "delivered")
    val stateBengali = listOf("প্রক্রিয়াকরণ", "কনফার্মড", "প্যাকেজিং", "শিপড্", "ডেলিভারড")

    val activeIdx = states.indexOf(currentStatus).let { if (it == -1) 0 else it }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        states.forEachIndexed { index, stateCode ->
            val isDone = index <= activeIdx
            val isCurrent = index == activeIdx

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Circle point visual indicators
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCurrent) LipstickPrimary 
                            else if (isDone) SuccessGreen 
                            else Color.LightGray
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone && !isCurrent) {
                        Icon(Icons.Default.Check, contentDescription = "Step finished indicator", tint = Color.White, modifier = Modifier.size(14.dp))
                    } else {
                        Text(
                            text = (index + 1).toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Track title label
                Text(
                    text = stateBengali[index],
                    fontSize = 12.sp,
                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (isCurrent) LipstickPrimary else if (isDone) SuccessGreen else Color.Gray
                )
            }
        }
    }
}
