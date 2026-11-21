package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.StoreRepository
import com.example.data.model.AdminMember
import com.example.data.model.CartItem
import com.example.data.model.FulfillmentType
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.ProductDraft
import com.example.data.model.StoreSettings
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class StoreViewModel(
    private val repository: StoreRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val isEmailVerified: Boolean,
    val currentDisplayName: String,
    val currentPhotoUrl: String?
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(StoreTab.STOREFRONT)
    val selectedTab: StateFlow<StoreTab> = _selectedTab.asStateFlow()

    private val _selectedAdminTab = MutableStateFlow(AdminSubTab.OVERVIEW)
    val selectedAdminTab: StateFlow<AdminSubTab> = _selectedAdminTab.asStateFlow()

    private val _catalogFilter = MutableStateFlow(CatalogFilterState())
    val catalogFilter: StateFlow<CatalogFilterState> = _catalogFilter.asStateFlow()

    private val _adminSearchQuery = MutableStateFlow("")
    val adminSearchQuery: StateFlow<String> = _adminSearchQuery.asStateFlow()

    private val _adminInventoryFilter = MutableStateFlow(AdminInventoryFilter.ALL)
    val adminInventoryFilter: StateFlow<AdminInventoryFilter> = _adminInventoryFilter.asStateFlow()

    private val _adminOrderFilter = MutableStateFlow<OrderStatus?>(null)
    val adminOrderFilter: StateFlow<OrderStatus?> = _adminOrderFilter.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private val _orderSuccessNumber = MutableStateFlow<String?>(null)
    val orderSuccessNumber: StateFlow<String?> = _orderSuccessNumber.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(
                displayName = currentDisplayName,
                email = currentUserEmail,
                photoUrl = currentPhotoUrl
            )
        }
    }

    val isAdmin: StateFlow<Boolean> = repository.observeIsAdmin(
        userId = currentUserId,
        email = currentUserEmail,
        emailVerified = isEmailVerified
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = repository.isBootstrappedAdmin(currentUserEmail, isEmailVerified)
    )

    val storeSettings: StateFlow<StoreSettings> = repository.observeStoreSettings(currentUserId)
        .catch { error ->
            unwrapAndRethrowIfCancelled("storeSettings", error)
            emit(StoreSettings())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = StoreSettings()
        )

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            unwrapAndRethrowIfCancelled("userProfile", error)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    val activeProductsState: StateFlow<UiState<List<Product>>> =
        repository.observeActiveProducts(currentUserId)
            .toUiState("products")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val filteredCatalogProducts: StateFlow<UiState<List<Product>>> = combine(
        activeProductsState,
        _catalogFilter
    ) { state, filter ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
            is UiState.Success -> {
                val query = filter.searchQuery.trim().lowercase()
                val filtered = state.data.filter { product ->
                    val matchesCategory = filter.selectedCategory == null ||
                        product.categoryEnum == filter.selectedCategory
                    val matchesStock = !filter.inStockOnly || !product.outOfStock
                    val matchesQuery = query.isEmpty() ||
                        product.name.lowercase().contains(query) ||
                        product.sku.lowercase().contains(query) ||
                        product.description.lowercase().contains(query) ||
                        product.category.lowercase().contains(query)
                    matchesCategory && matchesStock && matchesQuery
                }
                val sorted = when (filter.sortOption) {
                    ProductSortOption.FEATURED -> filtered.sortedWith(
                        compareByDescending<Product> { it.featured }
                            .thenByDescending { it.onSale }
                            .thenBy { it.outOfStock }
                            .thenBy { it.name.lowercase() }
                    )
                    ProductSortOption.PRICE_LOW_HIGH -> filtered.sortedBy { it.priceCents }
                    ProductSortOption.PRICE_HIGH_LOW -> filtered.sortedByDescending { it.priceCents }
                    ProductSortOption.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
                    ProductSortOption.STOCK_HIGH -> filtered.sortedByDescending { it.stockCount }
                }
                UiState.Success(sorted)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    val cartItemsState: StateFlow<UiState<List<CartItem>>> =
        repository.observeCart(currentUserId)
            .toUiState("cart")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val cartQuantityByProductId: StateFlow<Map<String, Long>> = cartItemsState.map { state ->
        if (state is UiState.Success) {
            state.data.associate { it.productId to it.quantity }
        } else {
            emptyMap()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyMap()
    )

    val totalCartItemCount: StateFlow<Int> = cartItemsState.map { state ->
        if (state is UiState.Success) state.data.sumOf { it.quantity.toInt() } else 0
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = 0
    )

    fun computeCartTotals(
        items: List<CartItem>,
        fulfillmentType: FulfillmentType,
        settings: StoreSettings
    ): CartTotals {
        val subtotal = items.sumOf { it.lineTotalCents }
        val count = items.sumOf { it.quantity }
        val deliveryFee = when {
            items.isEmpty() -> 0L
            fulfillmentType == FulfillmentType.PICKUP -> 0L
            subtotal >= settings.freeDeliveryThresholdCents -> 0L
            else -> settings.deliveryFeeCents
        }
        val tax = (subtotal * settings.taxRateBps) / 10000L
        val total = subtotal + deliveryFee + tax
        val remaining = (settings.freeDeliveryThresholdCents - subtotal).coerceAtLeast(0L)
        return CartTotals(
            items = items,
            itemCount = count,
            subtotalCents = subtotal,
            deliveryFeeCents = deliveryFee,
            taxCents = tax,
            totalCents = total,
            freeDeliveryRemainingCents = remaining
        )
    }

    val userOrdersState: StateFlow<UiState<List<Order>>> =
        repository.observeUserOrders(currentUserId)
            .toUiState("orders")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    // Admin-only streams: only attached when isAdmin == true
    val adminProductsState: StateFlow<UiState<List<Product>>> = isAdmin
        .flatMapLatest { admin ->
            if (admin) {
                repository.observeAllProductsAdmin(currentUserId).toUiState("admin_products")
            } else {
                flowOf(UiState.Success(emptyList()))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val filteredAdminProducts: StateFlow<UiState<List<Product>>> = combine(
        adminProductsState,
        _adminSearchQuery,
        _adminInventoryFilter
    ) { state, query, filter ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
            is UiState.Success -> {
                val q = query.trim().lowercase()
                val list = state.data.filter { p ->
                    val matchesFilter = when (filter) {
                        AdminInventoryFilter.ALL -> true
                        AdminInventoryFilter.ACTIVE -> p.active
                        AdminInventoryFilter.HIDDEN -> !p.active
                        AdminInventoryFilter.LOW_STOCK -> p.lowStock
                        AdminInventoryFilter.OUT_OF_STOCK -> p.outOfStock
                    }
                    val matchesQuery = q.isEmpty() ||
                        p.name.lowercase().contains(q) ||
                        p.sku.lowercase().contains(q) ||
                        p.category.lowercase().contains(q)
                    matchesFilter && matchesQuery
                }
                UiState.Success(list)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    val adminOrdersState: StateFlow<UiState<List<Order>>> = isAdmin
        .flatMapLatest { admin ->
            if (admin) {
                repository.observeAllOrdersAdmin(currentUserId).toUiState("admin_orders")
            } else {
                flowOf(UiState.Success(emptyList()))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val filteredAdminOrders: StateFlow<UiState<List<Order>>> = combine(
        adminOrdersState,
        _adminOrderFilter
    ) { state, filterStatus ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
            is UiState.Success -> {
                val filtered = if (filterStatus == null) {
                    state.data
                } else {
                    state.data.filter { it.statusEnum == filterStatus }
                }
                UiState.Success(filtered)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    val adminMembersState: StateFlow<UiState<List<AdminMember>>> = isAdmin
        .flatMapLatest { admin ->
            if (admin) {
                repository.observeAdminsList(currentUserId).toUiState("admins")
            } else {
                flowOf(UiState.Success(emptyList()))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val adminDashboardStats: StateFlow<AdminDashboardStats> = combine(
        adminProductsState,
        adminOrdersState
    ) { prodState, orderState ->
        val products = (prodState as? UiState.Success)?.data ?: emptyList()
        val orders = (orderState as? UiState.Success)?.data ?: emptyList()
        val activeOrders = orders.filter { it.statusEnum != OrderStatus.CANCELLED }
        AdminDashboardStats(
            totalRevenueCents = activeOrders.sumOf { it.totalCents },
            totalOrders = orders.size,
            pendingOrders = orders.count {
                it.statusEnum == OrderStatus.PENDING || it.statusEnum == OrderStatus.PACKING
            },
            activeProducts = products.count { it.active },
            totalProducts = products.size,
            lowStockProducts = products.filter { it.lowStock }.sortedBy { it.stockCount },
            outOfStockProducts = products.filter { it.outOfStock },
            recentOrders = orders.take(5)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = AdminDashboardStats()
    )

    // --- UI ACTIONS ---

    fun selectTab(tab: StoreTab) {
        _selectedTab.value = tab
    }

    fun selectAdminSubTab(tab: AdminSubTab) {
        _selectedAdminTab.value = tab
    }

    fun browseCategory(category: ProductCategory?) {
        _catalogFilter.update { it.copy(selectedCategory = category) }
        _selectedTab.value = StoreTab.CATALOG
    }

    fun updateSearchQuery(query: String) {
        _catalogFilter.update { it.copy(searchQuery = query) }
    }

    fun updateCategoryFilter(category: ProductCategory?) {
        _catalogFilter.update { it.copy(selectedCategory = category) }
    }

    fun toggleInStockOnly() {
        _catalogFilter.update { it.copy(inStockOnly = !it.inStockOnly) }
    }

    fun updateSortOption(sortOption: ProductSortOption) {
        _catalogFilter.update { it.copy(sortOption = sortOption) }
    }

    fun updateAdminSearchQuery(query: String) {
        _adminSearchQuery.value = query
    }

    fun updateAdminInventoryFilter(filter: AdminInventoryFilter) {
        _adminInventoryFilter.value = filter
    }

    fun updateAdminOrderFilter(status: OrderStatus?) {
        _adminOrderFilter.value = status
    }

    fun dismissOrderSuccess() {
        _orderSuccessNumber.value = null
    }

    fun addToCart(product: Product, delta: Long = 1L) {
        viewModelScope.launch {
            val result = repository.addOrIncrementCartItem(product, delta)
            result.onSuccess {
                if (delta > 0L) {
                    _snackbarEvents.tryEmit("Added ${product.name} to cart")
                }
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Unable to update cart")
            }
        }
    }

    fun setCartQuantity(productId: String, newQty: Long) {
        viewModelScope.launch {
            repository.setCartQuantity(productId, newQty)
                .onFailure { err ->
                    _snackbarEvents.tryEmit(err.localizedMessage ?: "Unable to update quantity")
                }
        }
    }

    fun removeCartItem(productId: String) {
        viewModelScope.launch {
            repository.removeCartItem(productId)
                .onFailure { err ->
                    _snackbarEvents.tryEmit(err.localizedMessage ?: "Unable to remove item")
                }
        }
    }

    fun placeOrder(
        deliveryAddress: String,
        notes: String?,
        fulfillmentType: FulfillmentType,
        cartItems: List<CartItem>
    ) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.placeOrder(
                customerName = currentDisplayName,
                customerEmail = currentUserEmail,
                deliveryAddress = deliveryAddress,
                notes = notes,
                fulfillmentType = fulfillmentType,
                cartItems = cartItems,
                settings = storeSettings.value
            )
            _isBusy.value = false
            result.onSuccess { orderNum ->
                _orderSuccessNumber.value = orderNum
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Checkout failed")
            }
        }
    }

    fun cancelUserOrder(orderId: String) {
        viewModelScope.launch {
            repository.cancelUserOrder(orderId)
                .onSuccess { _snackbarEvents.tryEmit("Order cancelled") }
                .onFailure { err -> _snackbarEvents.tryEmit(err.localizedMessage ?: "Cancel failed") }
        }
    }

    // --- ADMIN ACTIONS ---

    fun saveProduct(productId: String?, draft: ProductDraft, onComplete: () -> Unit) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.saveProduct(productId, draft)
            _isBusy.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit(
                    if (productId == null) "Created ${draft.name}" else "Updated ${draft.name}"
                )
                onComplete()
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to save product")
            }
        }
    }

    fun adjustStock(productId: String, newStockCount: Long) {
        viewModelScope.launch {
            repository.adjustProductStock(productId, newStockCount)
                .onFailure { err ->
                    _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to update stock")
                }
        }
    }

    fun toggleProductActive(product: Product) {
        viewModelScope.launch {
            repository.toggleProductActive(product.id, !product.active)
                .onSuccess {
                    _snackbarEvents.tryEmit(
                        if (!product.active) "${product.name} is now live" else "${product.name} hidden"
                    )
                }
                .onFailure { err ->
                    _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to update visibility")
                }
        }
    }

    fun toggleProductFeatured(product: Product) {
        viewModelScope.launch {
            repository.toggleProductFeatured(product.id, !product.featured)
                .onFailure { err ->
                    _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to update featured flag")
                }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product.id)
                .onSuccess { _snackbarEvents.tryEmit("Deleted ${product.name}") }
                .onFailure { err -> _snackbarEvents.tryEmit(err.localizedMessage ?: "Delete failed") }
        }
    }

    fun seedSampleCatalog() {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.seedSampleCatalog()
            _isBusy.value = false
            result.onSuccess { count ->
                val msg = if (count > 0) {
                    "Stocked $count general store products"
                } else {
                    "All sample SKUs are already in catalog"
                }
                _snackbarEvents.tryEmit(msg)
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to seed catalog")
            }
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus, notes: String?) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, newStatus, notes)
                .onSuccess { _snackbarEvents.tryEmit("Order marked ${newStatus.label}") }
                .onFailure { err ->
                    _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to update order")
                }
        }
    }

    fun saveStoreSettings(settings: StoreSettings) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.saveStoreSettings(settings)
            _isBusy.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit("Store settings saved")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to save settings")
            }
        }
    }

    fun addAdminMember(uid: String, email: String, label: String) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.addAdminMember(uid, email, label)
            _isBusy.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit("Admin access granted")
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.localizedMessage ?: "Failed to add admin")
            }
        }
    }

    fun removeAdminMember(uid: String) {
        viewModelScope.launch {
            repository.removeAdminMember(uid)
                .onSuccess { _snackbarEvents.tryEmit("Admin removed") }
                .onFailure { err -> _snackbarEvents.tryEmit(err.localizedMessage ?: "Remove failed") }
        }
    }

    private fun <T> Flow<T>.toUiState(label: String): Flow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val rootError = unwrapAndRethrowIfCancelled(label, error)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load $label"))
            }

    private fun unwrapAndRethrowIfCancelled(label: String, error: Throwable): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing $label", rootError)
        return rootError
    }

    private companion object {
        const val TAG = "StoreVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
