package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.firebase.StoreRepository
import com.example.data.firebase.signOut
import com.example.data.model.Product
import com.example.ui.StoreTab
import com.example.ui.StoreViewModel
import com.example.ui.UiState
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CartScreen
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.ControlPanelLockedScreen
import com.example.ui.screens.OrdersAccountScreen
import com.example.ui.screens.ProductDetailSheet
import com.example.ui.screens.StorefrontScreen
import com.example.ui.theme.DailyGoodsTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailyGoodsTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        key(user.uid) {
            AuthenticatedStoreShell(
                user = user,
                onSignedOut = { currentUser = null }
            )
        }
    }
}

@Composable
private fun AuthenticatedStoreShell(
    user: FirebaseUser,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val viewModel: StoreViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                StoreViewModel(
                    repository = StoreRepository(db),
                    currentUserId = user.uid,
                    currentUserEmail = user.email ?: "",
                    isEmailVerified = user.isEmailVerified,
                    currentDisplayName = user.displayName ?: "Shopper",
                    currentPhotoUrl = user.photoUrl?.toString()
                )
            }
        }
    )

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedAdminTab by viewModel.selectedAdminTab.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val storeSettings by viewModel.storeSettings.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val activeProductsState by viewModel.activeProductsState.collectAsStateWithLifecycle()
    val filteredCatalogState by viewModel.filteredCatalogProducts.collectAsStateWithLifecycle()
    val catalogFilter by viewModel.catalogFilter.collectAsStateWithLifecycle()
    val cartState by viewModel.cartItemsState.collectAsStateWithLifecycle()
    val cartQuantities by viewModel.cartQuantityByProductId.collectAsStateWithLifecycle()
    val totalCartCount by viewModel.totalCartItemCount.collectAsStateWithLifecycle()
    val userOrdersState by viewModel.userOrdersState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val orderSuccessNumber by viewModel.orderSuccessNumber.collectAsStateWithLifecycle()

    val adminProductsState by viewModel.filteredAdminProducts.collectAsStateWithLifecycle()
    val adminSearchQuery by viewModel.adminSearchQuery.collectAsStateWithLifecycle()
    val adminInventoryFilter by viewModel.adminInventoryFilter.collectAsStateWithLifecycle()
    val adminOrdersState by viewModel.filteredAdminOrders.collectAsStateWithLifecycle()
    val adminOrderFilter by viewModel.adminOrderFilter.collectAsStateWithLifecycle()
    val adminMembersState by viewModel.adminMembersState.collectAsStateWithLifecycle()
    val adminStats by viewModel.adminDashboardStats.collectAsStateWithLifecycle()

    var storefrontSelectedProduct by remember { mutableStateOf<Product?>(null) }
    var openEditorTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (selectedTab != StoreTab.STOREFRONT && storefrontSelectedProduct == null) {
        BackHandler { viewModel.selectTab(StoreTab.STOREFRONT) }
    }

    val visibleTabs = StoreTab.entries

    val productsById = remember(activeProductsState) {
        val list = (activeProductsState as? UiState.Success)?.data ?: emptyList()
        list.associateBy { it.id }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        visibleTabs.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        cartCount = totalCartCount,
                                        pendingAdminCount = adminStats.pendingOrders
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        visibleTabs.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        cartCount = totalCartCount,
                                        pendingAdminCount = adminStats.pendingOrders
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        StoreTab.STOREFRONT -> StorefrontScreen(
                            productsState = activeProductsState,
                            settings = storeSettings,
                            cartQuantities = cartQuantities,
                            isAdmin = isAdmin,
                            isBusy = isBusy,
                            onBrowseAll = { viewModel.selectTab(StoreTab.CATALOG) },
                            onBrowseCategory = { cat -> viewModel.browseCategory(cat) },
                            onOpenProduct = { product -> storefrontSelectedProduct = product },
                            onAddToCart = { product -> viewModel.addToCart(product, 1L) },
                            onUpdateCartQty = { prodId, qty -> viewModel.setCartQuantity(prodId, qty) },
                            onOpenAdmin = { viewModel.selectTab(StoreTab.ADMIN) },
                            onQuickAddProduct = {
                                viewModel.selectTab(StoreTab.ADMIN)
                                openEditorTrigger++
                            },
                            onSeedSampleCatalog = { viewModel.seedSampleCatalog() }
                        )

                        StoreTab.CATALOG -> CatalogScreen(
                            productsState = filteredCatalogState,
                            filterState = catalogFilter,
                            cartQuantities = cartQuantities,
                            isAdmin = isAdmin,
                            onSearchChange = viewModel::updateSearchQuery,
                            onCategorySelect = viewModel::updateCategoryFilter,
                            onToggleInStock = viewModel::toggleInStockOnly,
                            onSortChange = viewModel::updateSortOption,
                            onAddToCart = viewModel::addToCart,
                            onUpdateCartQty = viewModel::setCartQuantity,
                            onSeedSampleCatalog = viewModel::seedSampleCatalog,
                            onQuickAddProduct = {
                                viewModel.selectTab(StoreTab.ADMIN)
                                openEditorTrigger++
                            }
                        )

                        StoreTab.CART -> CartScreen(
                            cartState = cartState,
                            productsById = productsById,
                            settings = storeSettings,
                            isBusy = isBusy,
                            orderSuccessNumber = orderSuccessNumber,
                            computeTotals = viewModel::computeCartTotals,
                            onUpdateQty = viewModel::setCartQuantity,
                            onRemoveItem = viewModel::removeCartItem,
                            onBrowseCatalog = { viewModel.selectTab(StoreTab.CATALOG) },
                            onPlaceOrder = viewModel::placeOrder,
                            onDismissOrderSuccess = viewModel::dismissOrderSuccess,
                            onViewOrders = { viewModel.selectTab(StoreTab.ORDERS) }
                        )

                        StoreTab.ORDERS -> OrdersAccountScreen(
                            displayName = viewModel.currentDisplayName,
                            email = viewModel.currentUserEmail,
                            userId = viewModel.currentUserId,
                            userProfile = userProfile,
                            isAdmin = isAdmin,
                            ordersState = userOrdersState,
                            onCancelOrder = viewModel::cancelUserOrder,
                            onOpenAdmin = { viewModel.selectTab(StoreTab.ADMIN) },
                            onBrowseCatalog = { viewModel.selectTab(StoreTab.CATALOG) },
                            onSignOut = {
                                signOut(
                                    credentialManager = credentialManager,
                                    scope = scope,
                                    onSignOutComplete = onSignedOut
                                )
                            },
                            onCopiedUid = {}
                        )

                        StoreTab.ADMIN -> if (isAdmin) {
                            AdminPanelScreen(
                                selectedSubTab = selectedAdminTab,
                                stats = adminStats,
                                inventoryState = adminProductsState,
                                adminSearchQuery = adminSearchQuery,
                                inventoryFilter = adminInventoryFilter,
                                ordersState = adminOrdersState,
                                orderStatusFilter = adminOrderFilter,
                                settings = storeSettings,
                                adminMembersState = adminMembersState,
                                isBusy = isBusy,
                                openEditorTrigger = openEditorTrigger,
                                onSelectSubTab = viewModel::selectAdminSubTab,
                                onAdminSearchChange = viewModel::updateAdminSearchQuery,
                                onInventoryFilterChange = viewModel::updateAdminInventoryFilter,
                                onOrderFilterChange = viewModel::updateAdminOrderFilter,
                                onSaveProduct = viewModel::saveProduct,
                                onAdjustStock = viewModel::adjustStock,
                                onToggleActive = viewModel::toggleProductActive,
                                onToggleFeatured = viewModel::toggleProductFeatured,
                                onDeleteProduct = viewModel::deleteProduct,
                                onSeedSampleCatalog = viewModel::seedSampleCatalog,
                                onUpdateOrderStatus = viewModel::updateOrderStatus,
                                onSaveSettings = viewModel::saveStoreSettings,
                                onAddAdminMember = viewModel::addAdminMember,
                                onRemoveAdminMember = viewModel::removeAdminMember
                            )
                        } else {
                            ControlPanelLockedScreen(
                                currentEmail = viewModel.currentUserEmail,
                                userId = viewModel.currentUserId,
                                onSwitchAccount = {
                                    signOut(
                                        credentialManager = credentialManager,
                                        scope = scope,
                                        onSignOutComplete = onSignedOut
                                    )
                                },
                                onCopiedUid = {
                                    // Clipboard already updated
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    storefrontSelectedProduct?.let { product ->
        BackHandler { storefrontSelectedProduct = null }
        ProductDetailSheet(
            product = product,
            inCartQty = cartQuantities[product.id] ?: 0L,
            onDismiss = { storefrontSelectedProduct = null },
            onAddToCart = { qty ->
                viewModel.addToCart(product, qty)
                storefrontSelectedProduct = null
            }
        )
    }
}

@Composable
private fun TabIconWithBadge(
    tab: StoreTab,
    selected: Boolean,
    cartCount: Int,
    pendingAdminCount: Int
) {
    val icon: ImageVector = when (tab) {
        StoreTab.STOREFRONT -> if (selected) Icons.Rounded.Storefront else Icons.Outlined.Storefront
        StoreTab.CATALOG -> if (selected) Icons.Rounded.Inventory2 else Icons.Outlined.Inventory2
        StoreTab.CART -> if (selected) Icons.Rounded.ShoppingBag else Icons.Outlined.ShoppingBag
        StoreTab.ORDERS -> if (selected) Icons.Rounded.ReceiptLong else Icons.Outlined.ReceiptLong
        StoreTab.ADMIN -> if (selected) Icons.Rounded.AdminPanelSettings else Icons.Outlined.AdminPanelSettings
    }

    when {
        tab == StoreTab.CART && cartCount > 0 -> {
            BadgedBox(badge = { Badge { Text(cartCount.toString()) } }) {
                Icon(icon, contentDescription = tab.label)
            }
        }
        tab == StoreTab.ADMIN && pendingAdminCount > 0 -> {
            BadgedBox(badge = { Badge { Text(pendingAdminCount.toString()) } }) {
                Icon(icon, contentDescription = tab.label)
            }
        }
        else -> Icon(icon, contentDescription = tab.label)
    }
}
