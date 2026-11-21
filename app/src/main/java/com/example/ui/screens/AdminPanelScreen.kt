package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.firebase.StoreRepository
import com.example.data.model.AdminMember
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.Product
import com.example.data.model.ProductDraft
import com.example.data.model.StoreSettings
import com.example.ui.AdminDashboardStats
import com.example.ui.AdminInventoryFilter
import com.example.ui.AdminSubTab
import com.example.ui.UiState
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.ErrorStateBanner
import com.example.ui.components.LoadingScreenBox
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.ProductArtworkBadge
import com.example.ui.components.StockStatusPill
import com.example.ui.components.formatCents
import com.example.ui.components.formatTimestamp
import java.util.Locale
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    selectedSubTab: AdminSubTab,
    stats: AdminDashboardStats,
    inventoryState: UiState<List<Product>>,
    adminSearchQuery: String,
    inventoryFilter: AdminInventoryFilter,
    ordersState: UiState<List<Order>>,
    orderStatusFilter: OrderStatus?,
    settings: StoreSettings,
    adminMembersState: UiState<List<AdminMember>>,
    isBusy: Boolean,
    openEditorTrigger: Int = 0,
    onSelectSubTab: (AdminSubTab) -> Unit,
    onAdminSearchChange: (String) -> Unit,
    onInventoryFilterChange: (AdminInventoryFilter) -> Unit,
    onOrderFilterChange: (OrderStatus?) -> Unit,
    onSaveProduct: (String?, ProductDraft, () -> Unit) -> Unit,
    onAdjustStock: (String, Long) -> Unit,
    onToggleActive: (Product) -> Unit,
    onToggleFeatured: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onSeedSampleCatalog: () -> Unit,
    onUpdateOrderStatus: (String, OrderStatus, String?) -> Unit,
    onSaveSettings: (StoreSettings) -> Unit,
    onAddAdminMember: (String, String, String) -> Unit,
    onRemoveAdminMember: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showProductEditor by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var deletingProduct by remember { mutableStateOf<Product?>(null) }

    androidx.compose.runtime.LaunchedEffect(openEditorTrigger) {
        if (openEditorTrigger > 0) {
            editingProduct = null
            showProductEditor = true
        }
    }

    deletingProduct?.let { product ->
        AlertDialog(
            onDismissRequest = { deletingProduct = null },
            title = { Text("Delete ${product.name}?") },
            text = { Text("SKU ${product.sku} will be permanently removed from the catalog.") },
            confirmButton = {
                Button(onClick = {
                    onDeleteProduct(product)
                    deletingProduct = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingProduct = null }) { Text("Cancel") }
            }
        )
    }

    if (showProductEditor) {
        AdminProductEditorSheet(
            editingProduct = editingProduct,
            isBusy = isBusy,
            onDismiss = {
                showProductEditor = false
                editingProduct = null
            },
            onSave = { id, draft, addAnother ->
                onSaveProduct(id, draft) {
                    if (!addAnother) {
                        showProductEditor = false
                        editingProduct = null
                    }
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (selectedSubTab == AdminSubTab.INVENTORY || selectedSubTab == AdminSubTab.OVERVIEW) {
                FloatingActionButton(
                    onClick = {
                        editingProduct = null
                        showProductEditor = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("admin_add_product_fab")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add product")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Control Panel",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "${stats.activeProducts}/${stats.totalProducts} active SKUs • ${stats.pendingOrders} orders pending",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        editingProduct = null
                        showProductEditor = true
                    },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("control_panel_add_product_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Product")
                }
            }

            PrimaryTabRow(selectedTabIndex = selectedSubTab.ordinal) {
                AdminSubTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedSubTab == tab,
                        onClick = { onSelectSubTab(tab) },
                        text = { Text(tab.label, maxLines = 1) },
                        modifier = Modifier.testTag("admin_subtab_${tab.name.lowercase()}")
                    )
                }
            }

            when (selectedSubTab) {
                AdminSubTab.OVERVIEW -> AdminOverviewPane(
                    stats = stats,
                    settings = settings,
                    isBusy = isBusy,
                    onToggleStoreOpen = { open -> onSaveSettings(settings.copy(storeOpen = open)) },
                    onRestockProduct = { prod -> onAdjustStock(prod.id, prod.stockCount + 10L) },
                    onAddProduct = {
                        editingProduct = null
                        showProductEditor = true
                    },
                    onSeedCatalog = onSeedSampleCatalog,
                    onGoToOrders = { onSelectSubTab(AdminSubTab.ORDERS) },
                    onGoToInventory = { onSelectSubTab(AdminSubTab.INVENTORY) }
                )

                AdminSubTab.INVENTORY -> AdminInventoryPane(
                    inventoryState = inventoryState,
                    searchQuery = adminSearchQuery,
                    filter = inventoryFilter,
                    isBusy = isBusy,
                    onSearchChange = onAdminSearchChange,
                    onFilterChange = onInventoryFilterChange,
                    onEditProduct = { product ->
                        editingProduct = product
                        showProductEditor = true
                    },
                    onAdjustStock = onAdjustStock,
                    onToggleActive = onToggleActive,
                    onToggleFeatured = onToggleFeatured,
                    onDeleteProduct = { deletingProduct = it },
                    onSeedCatalog = onSeedSampleCatalog
                )

                AdminSubTab.ORDERS -> AdminOrdersPane(
                    ordersState = ordersState,
                    statusFilter = orderStatusFilter,
                    onFilterChange = onOrderFilterChange,
                    onUpdateStatus = onUpdateOrderStatus
                )

                AdminSubTab.SETTINGS -> AdminSettingsPane(
                    settings = settings,
                    adminMembersState = adminMembersState,
                    isBusy = isBusy,
                    onSaveSettings = onSaveSettings,
                    onAddAdminMember = onAddAdminMember,
                    onRemoveAdminMember = onRemoveAdminMember
                )
            }
        }
    }
}

@Composable
private fun AdminOverviewPane(
    stats: AdminDashboardStats,
    settings: StoreSettings,
    isBusy: Boolean,
    onToggleStoreOpen: (Boolean) -> Unit,
    onRestockProduct: (Product) -> Unit,
    onAddProduct: () -> Unit,
    onSeedCatalog: () -> Unit,
    onGoToOrders: () -> Unit,
    onGoToInventory: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Storefront Status", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (settings.storeOpen) "Accepting customer orders" else "Checkout paused",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.storeOpen,
                        onCheckedChange = onToggleStoreOpen
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    title = "Revenue",
                    value = formatCents(stats.totalRevenueCents),
                    subtitle = "${stats.totalOrders} total orders",
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Pending Queue",
                    value = stats.pendingOrders.toString(),
                    subtitle = "Need packing",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    title = "Active SKUs",
                    value = "${stats.activeProducts}/${stats.totalProducts}",
                    subtitle = "Live in catalog",
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Stock Alerts",
                    value = (stats.lowStockProducts.size + stats.outOfStockProducts.size).toString(),
                    subtitle = "${stats.outOfStockProducts.size} sold out",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Product Control",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Add custom products with SKU, pricing, and stock, or populate 16 sample essentials across all 7 departments.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onAddProduct,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("overview_add_product_button")
                        ) {
                            Icon(Icons.Rounded.AddBox, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Product")
                        }
                        OutlinedButton(
                            onClick = onSeedCatalog,
                            enabled = !isBusy,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_seed_catalog_button")
                        ) {
                            Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Seed 16 SKUs")
                        }
                    }
                }
            }
        }

        val alertItems = (stats.outOfStockProducts + stats.lowStockProducts).distinctBy { it.id }
        if (alertItems.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Low & Out-of-Stock Alerts", style = MaterialTheme.typography.titleLarge)
                    }
                    TextButton(onClick = onGoToInventory) { Text("All SKUs") }
                }
            }

            items(alertItems.take(6), key = { "alert_${it.id}" }) { product ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.name, style = MaterialTheme.typography.titleSmall)
                            StockStatusPill(product = product)
                        }
                        OutlinedButton(onClick = { onRestockProduct(product) }) {
                            Text("+10 Restock")
                        }
                    }
                }
            }
        }

        if (stats.recentOrders.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Store Orders", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onGoToOrders) { Text("Manage All") }
                }
            }
            items(stats.recentOrders, key = { "recent_${it.id}" }) { order ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "${order.orderNumber} • ${order.customerName}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "${order.itemCount} items • ${formatCents(order.totalCents)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OrderStatusChip(status = order.statusEnum)
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AdminInventoryPane(
    inventoryState: UiState<List<Product>>,
    searchQuery: String,
    filter: AdminInventoryFilter,
    isBusy: Boolean,
    onSearchChange: (String) -> Unit,
    onFilterChange: (AdminInventoryFilter) -> Unit,
    onEditProduct: (Product) -> Unit,
    onAdjustStock: (String, Long) -> Unit,
    onToggleActive: (Product) -> Unit,
    onToggleFeatured: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onSeedCatalog: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search SKU, name, department...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AdminInventoryFilter.entries, key = { it.name }) { item ->
                    FilterChip(
                        selected = filter == item,
                        onClick = { onFilterChange(item) },
                        label = { Text(item.label) }
                    )
                }
            }
        }

        when (inventoryState) {
            is UiState.Loading -> LoadingScreenBox()
            is UiState.Error -> ErrorStateBanner(
                message = inventoryState.message,
                modifier = Modifier.padding(16.dp)
            )
            is UiState.Success -> {
                val products = inventoryState.data
                if (products.isEmpty()) {
                    EmptyStatePanel(
                        icon = Icons.Rounded.Inventory2,
                        title = "No SKUs match filter",
                        subtitle = "Tap the + button to add a product, or stock 16 general store essentials.",
                        actionLabel = if (!isBusy && filter == AdminInventoryFilter.ALL && searchQuery.isBlank()) {
                            "Stock 16 Sample Products"
                        } else null,
                        onAction = onSeedCatalog
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 84.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(products, key = { it.id }) { product ->
                            AdminProductManageCard(
                                product = product,
                                onEdit = { onEditProduct(product) },
                                onAdjustStock = { next -> onAdjustStock(product.id, next) },
                                onToggleActive = { onToggleActive(product) },
                                onToggleFeatured = { onToggleFeatured(product) },
                                onDelete = { onDeleteProduct(product) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminProductManageCard(
    product: Product,
    onEdit: () -> Unit,
    onAdjustStock: (Long) -> Unit,
    onToggleActive: () -> Unit,
    onToggleFeatured: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProductArtworkBadge(
                    iconKey = product.iconKey,
                    category = product.categoryEnum,
                    imageUrl = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier.size(52.dp),
                    iconSize = 26.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${product.sku} • ${product.category} • ${formatCents(product.priceCents)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onToggleFeatured) {
                    Icon(
                        imageVector = if (product.featured) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                        contentDescription = "Toggle featured",
                        tint = if (product.featured) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit ${product.name}")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete ${product.name}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StockStatusPill(product = product)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { onAdjustStock((product.stockCount - 1L).coerceAtLeast(0L)) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) { Text("-1") }
                    OutlinedButton(
                        onClick = { onAdjustStock(product.stockCount + 1L) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) { Text("+1") }
                    OutlinedButton(
                        onClick = { onAdjustStock(product.stockCount + 10L) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) { Text("+10") }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (product.active) "Live in Storefront" else "Hidden Draft",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(checked = product.active, onCheckedChange = { onToggleActive() })
            }
        }
    }
}

@Composable
private fun AdminOrdersPane(
    ordersState: UiState<List<Order>>,
    statusFilter: OrderStatus?,
    onFilterChange: (OrderStatus?) -> Unit,
    onUpdateStatus: (String, OrderStatus, String?) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { onFilterChange(null) },
                    label = { Text("All Orders") }
                )
            }
            items(OrderStatus.entries, key = { it.name }) { status ->
                FilterChip(
                    selected = statusFilter == status,
                    onClick = { onFilterChange(if (statusFilter == status) null else status) },
                    label = { Text(status.label) }
                )
            }
        }

        when (ordersState) {
            is UiState.Loading -> LoadingScreenBox()
            is UiState.Error -> ErrorStateBanner(
                message = ordersState.message,
                modifier = Modifier.padding(16.dp)
            )
            is UiState.Success -> {
                val orders = ordersState.data
                if (orders.isEmpty()) {
                    EmptyStatePanel(
                        icon = Icons.Rounded.LocalShipping,
                        title = "No orders in this queue",
                        subtitle = "Customer orders appear here in real time as soon as checkout completes."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(orders, key = { it.id }) { order ->
                            AdminOrderCard(
                                order = order,
                                onUpdateStatus = { nextStatus, note ->
                                    onUpdateStatus(order.id, nextStatus, note)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminOrderCard(
    order: Order,
    onUpdateStatus: (OrderStatus, String?) -> Unit
) {
    val nextStatus = order.statusEnum.nextAdminStep()
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${order.orderNumber} • ${order.customerName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${formatTimestamp(order.createdAt)} • ${order.fulfillmentEnum.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OrderStatusChip(status = order.statusEnum)
            }

            Text(
                text = "Address: ${order.deliveryAddress}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            order.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${item.quantity}x ${item.productName} (${item.sku})",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatCents(item.unitPriceCents * item.quantity),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total: ${formatCents(order.totalCents)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (order.statusEnum != OrderStatus.CANCELLED &&
                        order.statusEnum != OrderStatus.DELIVERED
                    ) {
                        TextButton(onClick = { onUpdateStatus(OrderStatus.CANCELLED, order.notes) }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (nextStatus != null) {
                        Button(
                            onClick = { onUpdateStatus(nextStatus, order.notes) },
                            modifier = Modifier.testTag("advance_order_${order.orderNumber}")
                        ) {
                            Text("Mark ${nextStatus.label}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSettingsPane(
    settings: StoreSettings,
    adminMembersState: UiState<List<AdminMember>>,
    isBusy: Boolean,
    onSaveSettings: (StoreSettings) -> Unit,
    onAddAdminMember: (String, String, String) -> Unit,
    onRemoveAdminMember: (String) -> Unit
) {
    var storeName by remember(settings) { mutableStateOf(settings.storeName) }
    var announcement by remember(settings) { mutableStateOf(settings.announcement) }
    var storeHours by remember(settings) { mutableStateOf(settings.storeHours) }
    var taxPercent by remember(settings) {
        mutableStateOf(String.format(Locale.US, "%.2f", settings.taxRateBps / 100.0))
    }
    var deliveryFeeDollars by remember(settings) {
        mutableStateOf(String.format(Locale.US, "%.2f", settings.deliveryFeeCents / 100.0))
    }
    var freeThresholdDollars by remember(settings) {
        mutableStateOf(String.format(Locale.US, "%.2f", settings.freeDeliveryThresholdCents / 100.0))
    }
    var storeOpen by remember(settings) { mutableStateOf(settings.storeOpen) }

    var newAdminUid by remember { mutableStateOf("") }
    var newAdminEmail by remember { mutableStateOf("") }
    var newAdminLabel by remember { mutableStateOf("Store Manager") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Storefront Configuration", style = MaterialTheme.typography.titleLarge)

                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Store Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = announcement,
                        onValueChange = { announcement = it },
                        label = { Text("Banner Announcement") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = storeHours,
                        onValueChange = { storeHours = it },
                        label = { Text("Store Hours") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = taxPercent,
                            onValueChange = { taxPercent = it },
                            label = { Text("Tax %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = deliveryFeeDollars,
                            onValueChange = { deliveryFeeDollars = it },
                            label = { Text("Delivery ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = freeThresholdDollars,
                            onValueChange = { freeThresholdDollars = it },
                            label = { Text("Free Over ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Store Open for Checkout", style = MaterialTheme.typography.titleSmall)
                        Switch(checked = storeOpen, onCheckedChange = { storeOpen = it })
                    }

                    Button(
                        onClick = {
                            val taxBps = ((taxPercent.toDoubleOrNull() ?: 8.25) * 100.0).roundToLong()
                            val deliveryCents = ((deliveryFeeDollars.toDoubleOrNull() ?: 4.99) * 100.0).roundToLong()
                            val freeCents = ((freeThresholdDollars.toDoubleOrNull() ?: 35.0) * 100.0).roundToLong()
                            onSaveSettings(
                                settings.copy(
                                    storeName = storeName,
                                    announcement = announcement,
                                    storeHours = storeHours,
                                    storeOpen = storeOpen,
                                    taxRateBps = taxBps.coerceIn(0L, 3000L),
                                    deliveryFeeCents = deliveryCents.coerceIn(0L, 100_000L),
                                    freeDeliveryThresholdCents = freeCents.coerceIn(0L, 1_000_000L)
                                )
                            )
                        },
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_store_settings_button")
                    ) {
                        Text("Save Store Settings")
                    }
                }
            }
        }

        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Admin Team Access", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Grant Admin Console access to another signed-in teammate by pasting their Firebase UID.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newAdminUid,
                        onValueChange = { newAdminUid = it },
                        label = { Text("User Firebase UID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newAdminEmail,
                            onValueChange = { newAdminEmail = it },
                            label = { Text("Email (optional)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newAdminLabel,
                            onValueChange = { newAdminLabel = it },
                            label = { Text("Role Label") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            if (newAdminUid.isNotBlank()) {
                                onAddAdminMember(newAdminUid, newAdminEmail, newAdminLabel)
                                newAdminUid = ""
                                newAdminEmail = ""
                            }
                        },
                        enabled = !isBusy && newAdminUid.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Grant Admin Access")
                    }

                    HorizontalDivider()

                    when (adminMembersState) {
                        is UiState.Loading -> Text("Loading admin roster...", style = MaterialTheme.typography.bodySmall)
                        is UiState.Error -> ErrorStateBanner(adminMembersState.message)
                        is UiState.Success -> {
                            val members = adminMembersState.data
                            if (members.isEmpty()) {
                                Text(
                                    text = "Bootstrapped owner admin active. Add additional team members above.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                members.forEach { member ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(member.label, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                text = "${member.email.ifBlank { member.uid }}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { onRemoveAdminMember(member.uid) }) {
                                            Icon(
                                                Icons.Rounded.DeleteOutline,
                                                contentDescription = "Remove admin ${member.label}",
                                                tint = MaterialTheme.colorScheme.error
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
}

@Composable
fun ControlPanelLockedScreen(
    currentEmail: String,
    userId: String,
    onSwitchAccount: () -> Unit,
    onCopiedUid: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = androidx.compose.foundation.shape.CircleShape,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Control panel locked",
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Control Panel Access Required",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You are signed in as ${currentEmail.ifBlank { userId }}. Only verified store admins (${StoreRepository.BOOTSTRAPPED_ADMIN_EMAIL} or team members added in Settings) can add products and manage inventory.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Your Firebase UID", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = userId,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString(userId))
                        onCopiedUid()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy UID"
                        )
                    }
                }
                Button(
                    onClick = onSwitchAccount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("control_panel_switch_account_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Switch to Store Admin Account")
                }
            }
        }
    }
}

