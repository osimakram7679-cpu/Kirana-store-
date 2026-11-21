package com.example.ui

import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.ProductCategory

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class StoreTab(val label: String) {
    STOREFRONT("Store"),
    CATALOG("All Products"),
    CART("Cart"),
    ORDERS("Orders"),
    ADMIN("Control")
}

enum class AdminSubTab(val label: String) {
    OVERVIEW("Overview"),
    INVENTORY("Inventory"),
    ORDERS("Orders"),
    SETTINGS("Settings")
}

enum class ProductSortOption(val label: String) {
    FEATURED("Featured"),
    PRICE_LOW_HIGH("Price: Low–High"),
    PRICE_HIGH_LOW("Price: High–Low"),
    NAME_ASC("Name A–Z"),
    STOCK_HIGH("In Stock First")
}

enum class AdminInventoryFilter(val label: String) {
    ALL("All SKUs"),
    ACTIVE("Active"),
    HIDDEN("Hidden"),
    LOW_STOCK("Low Stock"),
    OUT_OF_STOCK("Out of Stock")
}

data class CartTotals(
    val items: List<CartItem> = emptyList(),
    val itemCount: Long = 0L,
    val subtotalCents: Long = 0L,
    val deliveryFeeCents: Long = 0L,
    val taxCents: Long = 0L,
    val totalCents: Long = 0L,
    val freeDeliveryRemainingCents: Long = 0L
)

data class AdminDashboardStats(
    val totalRevenueCents: Long = 0L,
    val totalOrders: Int = 0,
    val pendingOrders: Int = 0,
    val activeProducts: Int = 0,
    val totalProducts: Int = 0,
    val lowStockProducts: List<Product> = emptyList(),
    val outOfStockProducts: List<Product> = emptyList(),
    val recentOrders: List<Order> = emptyList()
)

data class CatalogFilterState(
    val searchQuery: String = "",
    val selectedCategory: ProductCategory? = null,
    val inStockOnly: Boolean = false,
    val sortOption: ProductSortOption = ProductSortOption.FEATURED
)
