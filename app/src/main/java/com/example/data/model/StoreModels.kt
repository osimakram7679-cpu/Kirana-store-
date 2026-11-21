package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

enum class ProductCategory(val wireName: String, val shortLabel: String) {
    PANTRY("Pantry", "Pantry"),
    PRODUCE("Produce", "Produce"),
    DAIRY_BAKERY("Dairy & Bakery", "Bakery"),
    BEVERAGES("Beverages", "Drinks"),
    HOME_CARE("Home & Care", "Home"),
    SNACKS("Snacks", "Snacks"),
    HARDWARE_UTILITY("Hardware & Utility", "Utility");

    companion object {
        fun fromWire(raw: String?): ProductCategory =
            entries.firstOrNull { it.wireName == raw || it.name == raw } ?: PANTRY
    }
}

enum class OrderStatus(val wireName: String, val label: String) {
    PENDING("PENDING", "Pending"),
    PACKING("PACKING", "Packing"),
    READY("READY", "Ready"),
    SHIPPED("SHIPPED", "Shipped"),
    DELIVERED("DELIVERED", "Delivered"),
    CANCELLED("CANCELLED", "Cancelled");

    fun nextAdminStep(): OrderStatus? = when (this) {
        PENDING -> PACKING
        PACKING -> READY
        READY -> SHIPPED
        SHIPPED -> DELIVERED
        DELIVERED, CANCELLED -> null
    }

    companion object {
        fun fromWire(raw: String?): OrderStatus =
            entries.firstOrNull { it.wireName == raw || it.name == raw } ?: PENDING
    }
}

enum class FulfillmentType(val wireName: String, val label: String) {
    DELIVERY("DELIVERY", "Local Delivery"),
    PICKUP("PICKUP", "Store Pickup");

    companion object {
        fun fromWire(raw: String?): FulfillmentType =
            entries.firstOrNull { it.wireName == raw || it.name == raw } ?: DELIVERY
    }
}

@Keep
@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val sku: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = ProductCategory.PANTRY.wireName,
    val unit: String = "Each",
    val priceCents: Long = 0L,
    val compareAtPriceCents: Long? = null,
    val stockCount: Long = 0L,
    val lowStockThreshold: Long = 5L,
    val featured: Boolean = false,
    val active: Boolean = true,
    val iconKey: String = "basket",
    val imageUrl: String? = null,
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val categoryEnum: ProductCategory
        get() = ProductCategory.fromWire(category)

    val outOfStock: Boolean
        get() = stockCount <= 0L

    val lowStock: Boolean
        get() = stockCount > 0L && stockCount <= lowStockThreshold

    val onSale: Boolean
        get() = compareAtPriceCents != null && compareAtPriceCents > priceCents
}

data class ProductDraft(
    val sku: String,
    val name: String,
    val description: String,
    val category: ProductCategory,
    val unit: String,
    val priceCents: Long,
    val compareAtPriceCents: Long?,
    val stockCount: Long,
    val lowStockThreshold: Long,
    val featured: Boolean,
    val active: Boolean,
    val iconKey: String,
    val imageUrl: String?
)

@Keep
@IgnoreExtraProperties
data class CartItem(
    @DocumentId val productId: String = "",
    val productName: String = "",
    val sku: String = "",
    val unit: String = "Each",
    val iconKey: String = "basket",
    val unitPriceCents: Long = 0L,
    val quantity: Long = 1L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val lineTotalCents: Long
        get() = unitPriceCents * quantity
}

@Keep
@IgnoreExtraProperties
data class OrderLineItem(
    val productId: String = "",
    val productName: String = "",
    val sku: String = "",
    val unit: String = "Each",
    val unitPriceCents: Long = 0L,
    val quantity: Long = 1L
) {
    fun toMap(): Map<String, Any> = mapOf(
        "productId" to productId,
        "productName" to productName,
        "sku" to sku,
        "unit" to unit,
        "unitPriceCents" to unitPriceCents,
        "quantity" to quantity
    )
}

@Keep
@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val orderNumber: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val deliveryAddress: String = "",
    val notes: String? = null,
    val fulfillmentType: String = FulfillmentType.DELIVERY.wireName,
    val status: String = OrderStatus.PENDING.wireName,
    val items: List<OrderLineItem> = emptyList(),
    val subtotalCents: Long = 0L,
    val deliveryFeeCents: Long = 0L,
    val taxCents: Long = 0L,
    val totalCents: Long = 0L,
    val itemCount: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val statusEnum: OrderStatus
        get() = OrderStatus.fromWire(status)

    val fulfillmentEnum: FulfillmentType
        get() = FulfillmentType.fromWire(fulfillmentType)
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val loyaltyPoints: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminMember(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val label: String = "",
    val addedBy: String = "",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StoreSettings(
    @DocumentId val id: String = "config",
    val storeName: String = "Daily Goods",
    val announcement: String = "Fresh neighborhood essentials delivered daily.",
    val storeHours: String = "8:00 AM - 9:00 PM Daily",
    val storeOpen: Boolean = true,
    val taxRateBps: Long = 825L,
    val deliveryFeeCents: Long = 499L,
    val freeDeliveryThresholdCents: Long = 3500L,
    val updatedAt: Timestamp? = null
)
