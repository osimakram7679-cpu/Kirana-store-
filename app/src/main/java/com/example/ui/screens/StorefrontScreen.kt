package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.StoreSettings
import com.example.ui.UiState
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.ErrorStateBanner
import com.example.ui.components.LoadingScreenBox
import com.example.ui.components.ProductArtworkBadge
import com.example.ui.components.QuantityStepper
import com.example.ui.components.StockStatusPill
import com.example.ui.components.StoreBrandHeader
import com.example.ui.components.categoryIcon
import com.example.ui.components.formatCents

@Composable
fun StorefrontScreen(
    productsState: UiState<List<Product>>,
    settings: StoreSettings,
    cartQuantities: Map<String, Long>,
    isAdmin: Boolean,
    isBusy: Boolean,
    onBrowseAll: () -> Unit,
    onBrowseCategory: (ProductCategory) -> Unit,
    onOpenProduct: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onUpdateCartQty: (String, Long) -> Unit,
    onOpenAdmin: () -> Unit,
    onQuickAddProduct: () -> Unit = {},
    onSeedSampleCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                StoreBrandHeader(
                    storeName = settings.storeName,
                    storeOpen = settings.storeOpen
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Editorial Hero Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(188.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(onClick = onBrowseAll)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_store_banner),
                        contentDescription = "General store essentials",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.72f),
                                        Color.Black.copy(alpha = 0.25f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                            shape = CircleShape
                        ) {
                            Text(
                                text = if (settings.freeDeliveryThresholdCents > 0L) {
                                    "FREE DELIVERY OVER ${formatCents(settings.freeDeliveryThresholdCents)}"
                                } else {
                                    "FREE LOCAL DELIVERY"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Column {
                            Text(
                                text = settings.announcement.ifBlank { "Fresh neighborhood essentials daily." },
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = settings.storeHours,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isAdmin) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                    ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AdminPanelSettings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Store Control Panel",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Add products, restock & manage orders",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = onQuickAddProduct,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("storefront_add_product_button")
                            ) {
                                Text("+ Add")
                            }
                            FilledTonalButton(
                                onClick = onOpenAdmin,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("storefront_admin_button")
                            ) {
                                Text("Panel")
                            }
                        }
                    }
                }
            }
        }

        // Visual Department Spotlights
        item {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shop by Department",
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(
                        onClick = onBrowseAll,
                        modifier = Modifier.testTag("browse_all_products_button")
                    ) {
                        Text("See All")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        DepartmentArtCard(
                            title = "Pantry & Staples",
                            subtitle = "Honey, oils & pasta",
                            drawableId = R.drawable.img_cat_pantry,
                            onClick = { onBrowseCategory(ProductCategory.PANTRY) }
                        )
                    }
                    item {
                        DepartmentArtCard(
                            title = "Fresh Produce",
                            subtitle = "Orchard fruit & greens",
                            drawableId = R.drawable.img_cat_fresh,
                            onClick = { onBrowseCategory(ProductCategory.PRODUCE) }
                        )
                    }
                    item {
                        DepartmentArtCard(
                            title = "Home & Care",
                            subtitle = "Soaps, candles & totes",
                            drawableId = R.drawable.img_cat_home,
                            onClick = { onBrowseCategory(ProductCategory.HOME_CARE) }
                        )
                    }
                    items(
                        items = listOf(
                            ProductCategory.DAIRY_BAKERY,
                            ProductCategory.BEVERAGES,
                            ProductCategory.SNACKS,
                            ProductCategory.HARDWARE_UTILITY
                        ),
                        key = { it.name }
                    ) { cat ->
                        CompactDepartmentCard(
                            category = cat,
                            onClick = { onBrowseCategory(cat) }
                        )
                    }
                }
            }
        }

        // Catalog Content
        when (productsState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        LoadingScreenBox()
                    }
                }
            }

            is UiState.Error -> {
                item {
                    ErrorStateBanner(
                        message = productsState.message,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            is UiState.Success -> {
                val products = productsState.data
                if (products.isEmpty()) {
                    item {
                        EmptyStatePanel(
                            icon = Icons.Rounded.Storefront,
                            title = "Shelves are ready to stock",
                            subtitle = if (isAdmin) {
                                "Tap below to stock 16 general store essentials across 7 departments, or add custom SKUs in Admin."
                            } else {
                                "No active products yet. Check back shortly once the store admin stocks the catalog."
                            },
                            actionLabel = if (isAdmin && !isBusy) "Stock 16 Sample Products" else null,
                            onAction = if (isAdmin) onSeedSampleCatalog else null
                        )
                    }
                } else {
                    val featured = products.filter { it.featured || it.onSale }.ifEmpty { products.take(6) }
                    item {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalOffer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Featured & Weekly Specials",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(featured, key = { it.id }) { product ->
                                    FeaturedProductCard(
                                        product = product,
                                        cartQty = cartQuantities[product.id] ?: 0L,
                                        onClick = { onOpenProduct(product) },
                                        onAdd = { onAddToCart(product) },
                                        onUpdateQty = { qty -> onUpdateCartQty(product.id, qty) }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Everyday Store Picks",
                                style = MaterialTheme.typography.titleLarge
                            )
                            OutlinedButton(onClick = onBrowseAll) {
                                Text("Browse ${products.size} Items")
                            }
                        }
                    }

                    items(products.take(8), key = { "pick_${it.id}" }) { product ->
                        StorefrontProductRowCard(
                            product = product,
                            cartQty = cartQuantities[product.id] ?: 0L,
                            onClick = { onOpenProduct(product) },
                            onAdd = { onAddToCart(product) },
                            onUpdateQty = { qty -> onUpdateCartQty(product.id, qty) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DepartmentArtCard(
    title: String,
    subtitle: String,
    drawableId: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(168.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Image(
                painter = painterResource(id = drawableId),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(98.dp)
            )
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CompactDepartmentCard(
    category: ProductCategory,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(134.dp)
            .height(152.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = categoryIcon(category),
                        contentDescription = category.wireName,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column {
                Text(
                    text = category.wireName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Browse",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun FeaturedProductCard(
    product: Product,
    cartQty: Long,
    onClick: () -> Unit,
    onAdd: () -> Unit,
    onUpdateQty: (Long) -> Unit
) {
    Card(
        modifier = Modifier
            .width(196.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                ProductArtworkBadge(
                    iconKey = product.iconKey,
                    category = product.categoryEnum,
                    imageUrl = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp),
                    iconSize = 42.dp
                )
                if (product.onSale) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "SAVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (product.featured) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("PICK", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${product.unit} • ${product.categoryEnum.shortLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))
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
            Spacer(modifier = Modifier.height(10.dp))
            if (product.outOfStock) {
                StockStatusPill(product = product)
            } else if (cartQty > 0L) {
                QuantityStepper(
                    quantity = cartQty,
                    maxStock = product.stockCount,
                    onDecrement = { onUpdateQty(cartQty - 1L) },
                    onIncrement = { onUpdateQty(cartQty + 1L) }
                )
            } else {
                Button(
                    onClick = onAdd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("Add to Bag", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun StorefrontProductRowCard(
    product: Product,
    cartQty: Long,
    onClick: () -> Unit,
    onAdd: () -> Unit,
    onUpdateQty: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductArtworkBadge(
                iconKey = product.iconKey,
                category = product.categoryEnum,
                imageUrl = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier.size(64.dp),
                iconSize = 30.dp
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
                    text = "${product.unit} • ${product.sku}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatCents(product.priceCents),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    StockStatusPill(product = product)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (!product.outOfStock) {
                if (cartQty > 0L) {
                    QuantityStepper(
                        quantity = cartQty,
                        maxStock = product.stockCount,
                        onDecrement = { onUpdateQty(cartQty - 1L) },
                        onIncrement = { onUpdateQty(cartQty + 1L) }
                    )
                } else {
                    FilledTonalButton(
                        onClick = onAdd,
                        modifier = Modifier.height(40.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp)
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}
