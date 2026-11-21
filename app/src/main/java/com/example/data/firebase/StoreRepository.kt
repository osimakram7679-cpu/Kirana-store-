package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.AdminMember
import com.example.data.model.CartItem
import com.example.data.model.FulfillmentType
import com.example.data.model.Order
import com.example.data.model.OrderLineItem
import com.example.data.model.OrderStatus
import com.example.data.model.Product
import com.example.data.model.ProductCatalogSeed
import com.example.data.model.ProductDraft
import com.example.data.model.StoreSettings
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val BOOTSTRAPPED_ADMIN_EMAIL = "osimakram90644@gmail.com"
        private val ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun isBootstrappedAdmin(email: String?, emailVerified: Boolean): Boolean {
        return emailVerified && email?.trim()?.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true) == true
    }

    fun observeIsAdmin(userId: String, email: String?, emailVerified: Boolean): Flow<Boolean> {
        if (isBootstrappedAdmin(email, emailVerified)) {
            return flowOf(true)
        }
        val path = "admins/$userId"
        return db.collection("admins").document(userId)
            .snapshots()
            .map { snapshot -> snapshot.exists() }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
                }
                emit(false)
            }
    }

    fun observeActiveProducts(userId: String): Flow<List<Product>> = flow {
        val path = "products"
        emitAll(
            db.collection(path)
                .whereEqualTo("active", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Product::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    logStreamError(userId, OperationType.LIST, path, error)
                    throw error
                }
        )
    }

    fun observeAllProductsAdmin(userId: String): Flow<List<Product>> = flow {
        val path = "products"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Product::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    logStreamError(userId, OperationType.LIST, path, error)
                    throw error
                }
        )
    }

    fun observeCart(userId: String): Flow<List<CartItem>> = flow {
        val path = "users/$userId/cart"
        emitAll(
            db.collection("users").document(userId).collection("cart")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CartItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.productName.lowercase() }
                }
                .catch { error ->
                    logStreamError(userId, OperationType.LIST, path, error)
                    throw error
                }
        )
    }

    fun observeUserOrders(userId: String): Flow<List<Order>> = flow {
        val path = "orders"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Order::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    logStreamError(userId, OperationType.LIST, path, error)
                    throw error
                }
        )
    }

    fun observeAllOrdersAdmin(userId: String): Flow<List<Order>> = flow {
        val path = "orders"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Order::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    logStreamError(userId, OperationType.LIST, path, error)
                    throw error
                }
        )
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    logStreamError(userId, OperationType.GET, path, error)
                    throw error
                }
        )
    }

    fun observeAdminsList(userId: String): Flow<List<AdminMember>> = flow {
        val path = "admins"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AdminMember::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.label.lowercase() }
                }
                .catch { error ->
                    logStreamError(userId, OperationType.LIST, path, error)
                    throw error
                }
        )
    }

    fun observeStoreSettings(userId: String): Flow<StoreSettings> = flow {
        val path = "store_settings/config"
        emitAll(
            db.collection("store_settings").document("config")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            StoreSettings::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: StoreSettings()
                    } else {
                        StoreSettings()
                    }
                }
                .catch { error ->
                    logStreamError(userId, OperationType.GET, path, error)
                    throw error
                }
        )
    }

    private fun logStreamError(userId: String, op: OperationType, path: String, error: Throwable) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    suspend fun ensureUserProfile(
        displayName: String,
        email: String,
        photoUrl: String?
    ): Result<Unit> {
        var docPath = "users/unknown"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val existing = docRef.get().await()
            val safeName = displayName.trim().ifBlank { "Shopper" }.take(80)
            val safeEmail = email.trim().take(120)
            val safePhoto = photoUrl?.trim()?.takeIf { it.isNotEmpty() }?.take(500)
            if (!existing.exists()) {
                val payload = buildMap<String, Any> {
                    put("userId", uid)
                    put("displayName", safeName)
                    put("email", safeEmail)
                    if (safePhoto != null) put("photoUrl", safePhoto)
                    put("loyaltyPoints", 0L)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (safePhoto != null) {
                    updates["photoUrl"] = safePhoto
                } else if (existing.contains("photoUrl")) {
                    updates["photoUrl"] = FieldValue.delete()
                }
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addOrIncrementCartItem(product: Product, delta: Long = 1L): Result<Unit> {
        var docPath = "users/unknown/cart/${product.id}"
        return try {
            val uid = requireUserId()
            require(product.id.isNotBlank() && ID_REGEX.matches(product.id)) {
                "Invalid product identifier"
            }
            require(product.active && !product.outOfStock) {
                "Product is currently unavailable"
            }
            val docRef = db.collection("users").document(uid)
                .collection("cart").document(product.id)
            docPath = docRef.path
            val snap = docRef.get().await()
            if (snap.exists()) {
                val currentQty = snap.getLong("quantity") ?: 0L
                val nextQty = (currentQty + delta).coerceAtMost(product.stockCount).coerceAtMost(99L)
                if (nextQty <= 0L) {
                    docRef.delete().await()
                } else {
                    docRef.update(
                        mapOf(
                            "productName" to product.name.take(80),
                            "sku" to product.sku.take(32),
                            "unit" to product.unit.take(24),
                            "iconKey" to product.iconKey.take(40),
                            "unitPriceCents" to product.priceCents,
                            "quantity" to nextQty,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } else if (delta > 0L) {
                val initialQty = delta.coerceAtMost(product.stockCount).coerceIn(1L, 99L)
                val payload = mapOf(
                    "productName" to product.name.take(80),
                    "sku" to product.sku.take(32),
                    "unit" to product.unit.take(24),
                    "iconKey" to product.iconKey.take(40),
                    "unitPriceCents" to product.priceCents,
                    "quantity" to initialQty,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun setCartQuantity(productId: String, newQuantity: Long): Result<Unit> {
        var docPath = "users/unknown/cart/$productId"
        return try {
            val uid = requireUserId()
            require(productId.isNotBlank() && ID_REGEX.matches(productId)) { "Invalid product ID" }
            val docRef = db.collection("users").document(uid)
                .collection("cart").document(productId)
            docPath = docRef.path
            if (newQuantity <= 0L) {
                docRef.delete().await()
            } else {
                docRef.update(
                    mapOf(
                        "quantity" to newQuantity.coerceIn(1L, 99L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun removeCartItem(productId: String): Result<Unit> {
        var docPath = "users/unknown/cart/$productId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
                .collection("cart").document(productId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun placeOrder(
        customerName: String,
        customerEmail: String,
        deliveryAddress: String,
        notes: String?,
        fulfillmentType: FulfillmentType,
        cartItems: List<CartItem>,
        settings: StoreSettings
    ): Result<String> {
        var opPath = "orders"
        return try {
            val uid = requireUserId()
            require(settings.storeOpen) { "Store is currently closed for new orders." }
            require(cartItems.isNotEmpty() && cartItems.size <= 30) {
                "Cart must contain between 1 and 30 distinct items."
            }
            val cleanAddress = if (fulfillmentType == FulfillmentType.PICKUP) {
                deliveryAddress.trim().ifBlank { "Store Counter Pickup" }.take(200)
            } else {
                deliveryAddress.trim().take(200).also {
                    require(it.length >= 4) { "Please enter a valid delivery address." }
                }
            }
            val cleanName = customerName.trim().ifBlank { "Shopper" }.take(80)
            val cleanEmail = customerEmail.trim().take(120)
            val cleanNotes = notes?.trim()?.takeIf { it.isNotEmpty() }?.take(300)

            val orderRef = db.collection("orders").document()
            opPath = orderRef.path
            val orderNumber = "DG-${(1000..9999).random()}"

            db.runTransaction { transaction ->
                val productRefs = cartItems.map { db.collection("products").document(it.productId) }
                val productSnaps = productRefs.map { transaction.get(it) }
                val userRef = db.collection("users").document(uid)
                val userSnap = transaction.get(userRef)

                val verifiedLineItems = mutableListOf<OrderLineItem>()
                var subtotalCents = 0L
                var itemCount = 0L

                for (i in cartItems.indices) {
                    val cartItem = cartItems[i]
                    val prodSnap = productSnaps[i]
                    if (!prodSnap.exists()) {
                        throw IllegalStateException("${cartItem.productName} is no longer in the catalog.")
                    }
                    val active = prodSnap.getBoolean("active") ?: false
                    if (!active) {
                        throw IllegalStateException("${cartItem.productName} is currently unavailable.")
                    }
                    val currentStock = prodSnap.getLong("stockCount") ?: 0L
                    if (currentStock < cartItem.quantity) {
                        throw IllegalStateException("Only $currentStock left for ${cartItem.productName}.")
                    }
                    val livePrice = prodSnap.getLong("priceCents") ?: cartItem.unitPriceCents
                    val liveName = prodSnap.getString("name") ?: cartItem.productName
                    val liveSku = prodSnap.getString("sku") ?: cartItem.sku
                    val liveUnit = prodSnap.getString("unit") ?: cartItem.unit

                    subtotalCents += livePrice * cartItem.quantity
                    itemCount += cartItem.quantity
                    verifiedLineItems.add(
                        OrderLineItem(
                            productId = cartItem.productId,
                            productName = liveName,
                            sku = liveSku,
                            unit = liveUnit,
                            unitPriceCents = livePrice,
                            quantity = cartItem.quantity
                        )
                    )
                }

                val deliveryFeeCents = when {
                    fulfillmentType == FulfillmentType.PICKUP -> 0L
                    subtotalCents >= settings.freeDeliveryThresholdCents -> 0L
                    else -> settings.deliveryFeeCents
                }
                val taxCents = (subtotalCents * settings.taxRateBps) / 10000L
                val totalCents = (subtotalCents + deliveryFeeCents + taxCents).coerceAtLeast(1L)

                for (i in cartItems.indices) {
                    val cartItem = cartItems[i]
                    val prodSnap = productSnaps[i]
                    val currentStock = prodSnap.getLong("stockCount") ?: 0L
                    transaction.update(
                        productRefs[i],
                        mapOf(
                            "stockCount" to (currentStock - cartItem.quantity).coerceAtLeast(0L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    val cartDocRef = db.collection("users").document(uid)
                        .collection("cart").document(cartItem.productId)
                    transaction.delete(cartDocRef)
                }

                val earnedPoints = (subtotalCents / 100L).coerceAtLeast(1L)
                if (userSnap.exists()) {
                    val existingPoints = userSnap.getLong("loyaltyPoints") ?: 0L
                    transaction.update(
                        userRef,
                        mapOf(
                            "loyaltyPoints" to (existingPoints + earnedPoints),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                val orderPayload = buildMap<String, Any> {
                    put("orderNumber", orderNumber)
                    put("userId", uid)
                    put("customerName", cleanName)
                    put("customerEmail", cleanEmail)
                    put("deliveryAddress", cleanAddress)
                    if (cleanNotes != null) put("notes", cleanNotes)
                    put("fulfillmentType", fulfillmentType.wireName)
                    put("status", OrderStatus.PENDING.wireName)
                    put("items", verifiedLineItems.map { it.toMap() })
                    put("subtotalCents", subtotalCents)
                    put("deliveryFeeCents", deliveryFeeCents)
                    put("taxCents", taxCents)
                    put("totalCents", totalCents)
                    put("itemCount", itemCount)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                transaction.set(orderRef, orderPayload)
            }.await()

            Result.success(orderNumber)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, opPath)
            Result.failure(e)
        }
    }

    suspend fun cancelUserOrder(orderId: String): Result<Unit> {
        val docPath = "orders/$orderId"
        return try {
            requireUserId()
            require(orderId.isNotBlank() && ID_REGEX.matches(orderId)) { "Invalid order ID" }
            db.collection("orders").document(orderId).update(
                mapOf(
                    "status" to OrderStatus.CANCELLED.wireName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- ADMIN OPERATIONS ---

    suspend fun saveProduct(productId: String?, draft: ProductDraft): Result<Unit> {
        var docPath = "products"
        return try {
            val uid = requireUserId()
            val sku = draft.sku.trim().uppercase().take(32)
            val name = draft.name.trim().take(80)
            val description = draft.description.trim().take(500)
            val unit = draft.unit.trim().ifBlank { "Each" }.take(24)
            val iconKey = draft.iconKey.trim().ifBlank { "basket" }.take(40)
            val imageUrl = draft.imageUrl?.trim()?.takeIf { it.isNotEmpty() }?.take(500)

            require(sku.length >= 2) { "SKU must be at least 2 characters." }
            require(name.length >= 2) { "Product name must be at least 2 characters." }
            require(draft.priceCents in 1L..10_000_000L) { "Price must be greater than \$0.00." }
            require(draft.stockCount in 0L..100_000L) { "Stock count must be between 0 and 100,000." }
            require(draft.lowStockThreshold in 0L..1000L) { "Low stock threshold must be 0..1000." }

            if (productId == null) {
                val docRef = db.collection("products").document()
                docPath = docRef.path
                val payload = buildMap<String, Any> {
                    put("sku", sku)
                    put("name", name)
                    put("description", description)
                    put("category", draft.category.wireName)
                    put("unit", unit)
                    put("priceCents", draft.priceCents)
                    if (draft.compareAtPriceCents != null && draft.compareAtPriceCents > 0L) {
                        put("compareAtPriceCents", draft.compareAtPriceCents)
                    }
                    put("stockCount", draft.stockCount)
                    put("lowStockThreshold", draft.lowStockThreshold)
                    put("featured", draft.featured)
                    put("active", draft.active)
                    put("iconKey", iconKey)
                    if (imageUrl != null) put("imageUrl", imageUrl)
                    put("createdBy", uid)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(payload).await()
            } else {
                require(ID_REGEX.matches(productId)) { "Invalid product ID" }
                val docRef = db.collection("products").document(productId)
                docPath = docRef.path
                val updates = mutableMapOf<String, Any>(
                    "sku" to sku,
                    "name" to name,
                    "description" to description,
                    "category" to draft.category.wireName,
                    "unit" to unit,
                    "priceCents" to draft.priceCents,
                    "stockCount" to draft.stockCount,
                    "lowStockThreshold" to draft.lowStockThreshold,
                    "featured" to draft.featured,
                    "active" to draft.active,
                    "iconKey" to iconKey,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                updates["compareAtPriceCents"] =
                    if (draft.compareAtPriceCents != null && draft.compareAtPriceCents > 0L) {
                        draft.compareAtPriceCents
                    } else {
                        FieldValue.delete()
                    }
                updates["imageUrl"] = imageUrl ?: FieldValue.delete()
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun adjustProductStock(productId: String, newStockCount: Long): Result<Unit> {
        val docPath = "products/$productId"
        return try {
            requireUserId()
            require(productId.isNotBlank() && ID_REGEX.matches(productId)) { "Invalid product ID" }
            val clamped = newStockCount.coerceIn(0L, 100_000L)
            db.collection("products").document(productId).update(
                mapOf(
                    "stockCount" to clamped,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleProductActive(productId: String, active: Boolean): Result<Unit> {
        val docPath = "products/$productId"
        return try {
            requireUserId()
            db.collection("products").document(productId).update(
                mapOf(
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleProductFeatured(productId: String, featured: Boolean): Result<Unit> {
        val docPath = "products/$productId"
        return try {
            requireUserId()
            db.collection("products").document(productId).update(
                mapOf(
                    "featured" to featured,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        val docPath = "products/$productId"
        return try {
            requireUserId()
            db.collection("products").document(productId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun seedSampleCatalog(): Result<Int> {
        val path = "products"
        return try {
            val uid = requireUserId()
            val seeds = ProductCatalogSeed.defaultProducts()
            val existingSkus = db.collection("products").get().await()
                .documents.mapNotNull { it.getString("sku") }.toSet()
            val toInsert = seeds.filter { it.sku !in existingSkus }
            if (toInsert.isEmpty()) {
                return Result.success(0)
            }
            val batch = db.batch()
            toInsert.forEach { draft ->
                val docRef = db.collection("products").document()
                val payload = buildMap<String, Any> {
                    put("sku", draft.sku)
                    put("name", draft.name)
                    put("description", draft.description)
                    put("category", draft.category.wireName)
                    put("unit", draft.unit)
                    put("priceCents", draft.priceCents)
                    if (draft.compareAtPriceCents != null && draft.compareAtPriceCents > 0L) {
                        put("compareAtPriceCents", draft.compareAtPriceCents)
                    }
                    put("stockCount", draft.stockCount)
                    put("lowStockThreshold", draft.lowStockThreshold)
                    put("featured", draft.featured)
                    put("active", draft.active)
                    put("iconKey", draft.iconKey)
                    put("createdBy", uid)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                batch.set(docRef, payload)
            }
            batch.commit().await()
            Result.success(toInsert.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(
        orderId: String,
        newStatus: OrderStatus,
        notes: String?
    ): Result<Unit> {
        val docPath = "orders/$orderId"
        return try {
            requireUserId()
            require(orderId.isNotBlank() && ID_REGEX.matches(orderId)) { "Invalid order ID" }
            val cleanNotes = notes?.trim()?.takeIf { it.isNotEmpty() }?.take(300)
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus.wireName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            updates["notes"] = cleanNotes ?: FieldValue.delete()
            db.collection("orders").document(orderId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveStoreSettings(settings: StoreSettings): Result<Unit> {
        val docPath = "store_settings/config"
        return try {
            requireUserId()
            val cleanName = settings.storeName.trim().ifBlank { "Daily Goods" }.take(60)
            val cleanAnnouncement = settings.announcement.trim().take(160)
            val cleanHours = settings.storeHours.trim().ifBlank { "8:00 AM - 9:00 PM" }.take(80)
            val taxRate = settings.taxRateBps.coerceIn(0L, 3000L)
            val deliveryFee = settings.deliveryFeeCents.coerceIn(0L, 100_000L)
            val freeThreshold = settings.freeDeliveryThresholdCents.coerceIn(0L, 1_000_000L)

            val docRef = db.collection("store_settings").document("config")
            val payload = mapOf(
                "storeName" to cleanName,
                "announcement" to cleanAnnouncement,
                "storeHours" to cleanHours,
                "storeOpen" to settings.storeOpen,
                "taxRateBps" to taxRate,
                "deliveryFeeCents" to deliveryFee,
                "freeDeliveryThresholdCents" to freeThreshold,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.update(payload).await()
            } else {
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addAdminMember(targetUid: String, email: String, label: String): Result<Unit> {
        val cleanUid = targetUid.trim()
        val docPath = "admins/$cleanUid"
        return try {
            val actorUid = requireUserId()
            require(cleanUid.isNotBlank() && cleanUid.length <= 128 && ID_REGEX.matches(cleanUid)) {
                "Target UID must contain only letters, numbers, underscores, or hyphens."
            }
            val cleanLabel = label.trim().ifBlank { "Store Manager" }.take(60)
            val cleanEmail = email.trim().take(120)
            val docRef = db.collection("admins").document(cleanUid)
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.update(mapOf("email" to cleanEmail, "label" to cleanLabel)).await()
            } else {
                docRef.set(
                    mapOf(
                        "uid" to cleanUid,
                        "email" to cleanEmail,
                        "label" to cleanLabel,
                        "addedBy" to actorUid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun removeAdminMember(targetUid: String): Result<Unit> {
        val docPath = "admins/$targetUid"
        return try {
            requireUserId()
            require(targetUid.isNotBlank() && ID_REGEX.matches(targetUid)) { "Invalid UID" }
            db.collection("admins").document(targetUid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
