package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.ui.CatalogFilterState
import com.example.ui.ProductSortOption
import com.example.ui.UiState
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.ErrorStateBanner
import com.example.ui.components.LoadingScreenBox
import com.example.ui.components.ProductArtworkBadge
import com.example.ui.components.QuantityStepper
import com.example.ui.components.StockStatusPill
import com.example.ui.components.formatCents

@Composable
fun CatalogScreen(
    productsState: UiState<List<Product>>,
    filterState: CatalogFilterState,
    cartQuantities: Map<String, Long>,
    isAdmin: Boolean,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (ProductCategory?) -> Unit,
    onToggleInStock: () -> Unit,
    onSortChange: (ProductSortOption) -> Unit,
    onAddToCart: (Product, Long) -> Unit,
    onUpdateCartQty: (String, Long) -> Unit,
    onSeedSampleCatalog: () -> Unit,
    onQuickAddProduct: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "All Products",
                    style = MaterialTheme.typography.headlineMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAdmin) {
                        Button(
                            onClick = onQuickAddProduct,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("catalog_add_product_button")
                        ) {
                            Text("+ Add", style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    FilterChip(
                        selected = filterState.inStockOnly,
                        onClick = onToggleInStock,
                        label = { Text("In Stock") },
                        leadingIcon = if (filterState.inStockOnly) {
                            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = { sortMenuExpanded = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.Sort, contentDescription = "Sort", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(filterState.sortOption.label, style = MaterialTheme.typography.labelMedium)
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        ProductSortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    onSortChange(option)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = filterState.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search items, SKU, or category...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (filterState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("catalog_search_input")
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = filterState.selectedCategory == null,
                    onClick = { onCategorySelect(null) },
                    label = { Text("All") }
                )
            }
            items(ProductCategory.entries, key = { it.name }) { category ->
                FilterChip(
                    selected = filterState.selectedCategory == category,
                    onClick = {
                        onCategorySelect(
                            if (filterState.selectedCategory == category) null else category
                        )
                    },
                    label = { Text(category.shortLabel) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (productsState) {
            is UiState.Loading -> LoadingScreenBox()
            is UiState.Error -> ErrorStateBanner(
                message = productsState.message,
                modifier = Modifier.padding(16.dp)
            )
            is UiState.Success -> {
                val items = productsState.data
                if (items.isEmpty()) {
                    EmptyStatePanel(
                        icon = Icons.Rounded.Storefront,
                        title = "No matching products",
                        subtitle = "Try clearing your search or category filter.",
                        actionLabel = if (isAdmin && filterState.searchQuery.isBlank() && filterState.selectedCategory == null) {
                            "Stock 16 Sample Products"
                        } else null,
                        onAction = if (isAdmin) onSeedSampleCatalog else null
                    )
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val columns = when {
                            maxWidth >= 900.dp -> 4
                            maxWidth >= 600.dp -> 3
                            else -> 2
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(items, key = { it.id }) { product ->
                                ProductGridCard(
                                    product = product,
                                    cartQty = cartQuantities[product.id] ?: 0L,
                                    onOpen = { selectedProduct = product },
                                    onAdd = { onAddToCart(product, 1L) },
                                    onUpdateQty = { qty -> onUpdateCartQty(product.id, qty) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedProduct?.let { product ->
        BackHandler { selectedProduct = null }
        ProductDetailSheet(
            product = product,
            inCartQty = cartQuantities[product.id] ?: 0L,
            onDismiss = { selectedProduct = null },
            onAddToCart = { qty ->
                onAddToCart(product, qty)
                selectedProduct = null
            }
        )
    }
}

@Composable
fun ProductGridCard(
    product: Product,
    cartQty: Long,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
    onUpdateQty: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("product_card_${product.sku}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            ProductArtworkBadge(
                iconKey = product.iconKey,
                category = product.categoryEnum,
                imageUrl = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                iconSize = 40.dp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = product.categoryEnum.shortLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )
            Text(
                text = product.unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatCents(product.priceCents),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (product.onSale && product.compareAtPriceCents != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatCents(product.compareAtPriceCents),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            StockStatusPill(product = product)
            Spacer(modifier = Modifier.height(10.dp))
            if (product.outOfStock) {
                OutlinedButton(
                    onClick = onOpen,
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Text("Sold Out")
                }
            } else if (cartQty > 0L) {
                QuantityStepper(
                    quantity = cartQty,
                    maxStock = product.stockCount,
                    onDecrement = { onUpdateQty(cartQty - 1L) },
                    onIncrement = { onUpdateQty(cartQty + 1L) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                Button(
                    onClick = onAdd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("add_to_cart_${product.sku}"),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailSheet(
    product: Product,
    inCartQty: Long,
    onDismiss: () -> Unit,
    onAddToCart: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedQty by remember(product.id) { mutableLongStateOf(1L) }
    val maxAddable = (product.stockCount - inCartQty).coerceAtLeast(0L)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                ) {
                    Text(
                        text = "${product.category} • ${product.sku}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close product details")
                }
            }

            ProductArtworkBadge(
                iconKey = product.iconKey,
                category = product.categoryEnum,
                imageUrl = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                iconSize = 60.dp
            )

            Text(text = product.name, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Unit: ${product.unit}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = formatCents(product.priceCents),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (product.onSale && product.compareAtPriceCents != null) {
                    Text(
                        text = formatCents(product.compareAtPriceCents),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
                StockStatusPill(product = product)
            }

            Text(
                text = product.description.ifBlank { "Fresh everyday general store essential." },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (inCartQty > 0L) {
                Text(
                    text = "Currently $inCartQty in your bag.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (!product.outOfStock && maxAddable > 0L) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuantityStepper(
                        quantity = selectedQty,
                        maxStock = maxAddable,
                        onDecrement = { if (selectedQty > 1L) selectedQty-- },
                        onIncrement = { if (selectedQty < maxAddable) selectedQty++ }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { onAddToCart(selectedQty) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("detail_add_to_cart_button")
                    ) {
                        Text("Add ${formatCents(product.priceCents * selectedQty)}")
                    }
                }
            } else {
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(if (product.outOfStock) "Currently Out of Stock" else "Max Stock Already in Cart")
                }
            }
        }
    }
}
