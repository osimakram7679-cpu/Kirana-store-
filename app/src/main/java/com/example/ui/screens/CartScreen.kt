package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CartItem
import com.example.data.model.FulfillmentType
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.StoreSettings
import com.example.ui.CartTotals
import com.example.ui.UiState
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.ErrorStateBanner
import com.example.ui.components.LoadingScreenBox
import com.example.ui.components.ProductArtworkBadge
import com.example.ui.components.QuantityStepper
import com.example.ui.components.formatCents

@Composable
fun CartScreen(
    cartState: UiState<List<CartItem>>,
    productsById: Map<String, Product>,
    settings: StoreSettings,
    isBusy: Boolean,
    orderSuccessNumber: String?,
    computeTotals: (List<CartItem>, FulfillmentType, StoreSettings) -> CartTotals,
    onUpdateQty: (String, Long) -> Unit,
    onRemoveItem: (String) -> Unit,
    onBrowseCatalog: () -> Unit,
    onPlaceOrder: (address: String, notes: String?, fulfillment: FulfillmentType, items: List<CartItem>) -> Unit,
    onDismissOrderSuccess: () -> Unit,
    onViewOrders: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fulfillmentType by remember { mutableStateOf(FulfillmentType.DELIVERY) }
    var deliveryAddress by remember { mutableStateOf("412 Oakwood Avenue, Apt 3B") }
    var orderNotes by remember { mutableStateOf("") }

    orderSuccessNumber?.let { orderNum ->
        AlertDialog(
            onDismissRequest = onDismissOrderSuccess,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Order Confirmed",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(44.dp)
                )
            },
            title = { Text("Order $orderNum Placed!") },
            text = {
                Text(
                    "Your order has been sent to ${settings.storeName}. Live stock has been updated and you can track fulfillment status in Orders."
                )
            },
            confirmButton = {
                Button(onClick = {
                    onDismissOrderSuccess()
                    onViewOrders()
                }) {
                    Text("Track Order")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissOrderSuccess) {
                    Text("Done")
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Market Bag",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        when (cartState) {
            is UiState.Loading -> LoadingScreenBox()
            is UiState.Error -> ErrorStateBanner(
                message = cartState.message,
                modifier = Modifier.padding(16.dp)
            )
            is UiState.Success -> {
                val items = cartState.data
                if (items.isEmpty()) {
                    EmptyStatePanel(
                        icon = Icons.Rounded.ShoppingBag,
                        title = "Your market bag is empty",
                        subtitle = "Browse our fresh produce, pantry staples, and home goods to start an order.",
                        actionLabel = "Browse All Products",
                        onAction = onBrowseCatalog,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val totals = computeTotals(items, fulfillmentType, settings)

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!settings.storeOpen) {
                            item {
                                ErrorStateBanner("Store is currently closed for checkout. You can still manage your bag.")
                            }
                        }

                        if (fulfillmentType == FulfillmentType.DELIVERY && settings.freeDeliveryThresholdCents > 0L) {
                            item {
                                val progress = (totals.subtotalCents.toFloat() /
                                    settings.freeDeliveryThresholdCents.toFloat()).coerceIn(0f, 1f)
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = if (totals.freeDeliveryRemainingCents == 0L) {
                                                "You unlocked FREE local delivery!"
                                            } else {
                                                "Add ${formatCents(totals.freeDeliveryRemainingCents)} more for FREE local delivery"
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        items(items, key = { it.productId }) { cartItem ->
                            val liveProduct = productsById[cartItem.productId]
                            val maxStock = liveProduct?.stockCount ?: 99L
                            CartItemRowCard(
                                item = cartItem,
                                maxStock = maxStock,
                                category = liveProduct?.categoryEnum ?: ProductCategory.PANTRY,
                                imageUrl = liveProduct?.imageUrl,
                                onUpdateQty = { qty -> onUpdateQty(cartItem.productId, qty) },
                                onRemove = { onRemoveItem(cartItem.productId) }
                            )
                        }

                        item {
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Fulfillment", style = MaterialTheme.typography.titleMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        FilterChip(
                                            selected = fulfillmentType == FulfillmentType.DELIVERY,
                                            onClick = { fulfillmentType = FulfillmentType.DELIVERY },
                                            label = { Text("Delivery") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Rounded.LocalShipping,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                        FilterChip(
                                            selected = fulfillmentType == FulfillmentType.PICKUP,
                                            onClick = { fulfillmentType = FulfillmentType.PICKUP },
                                            label = { Text("Store Pickup (Free)") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Rounded.Store,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                    }

                                    if (fulfillmentType == FulfillmentType.DELIVERY) {
                                        OutlinedTextField(
                                            value = deliveryAddress,
                                            onValueChange = { deliveryAddress = it },
                                            label = { Text("Delivery Address") },
                                            placeholder = { Text("Street, Apt / Suite") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("delivery_address_input")
                                        )
                                    }

                                    OutlinedTextField(
                                        value = orderNotes,
                                        onValueChange = { orderNotes = it },
                                        label = { Text("Order Notes (optional)") },
                                        placeholder = { Text("Leave at side porch, ripeness preference...") },
                                        maxLines = 2,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    HorizontalDivider()

                                    SummaryLine("Subtotal (${totals.itemCount} items)", formatCents(totals.subtotalCents))
                                    SummaryLine(
                                        "Delivery Fee",
                                        if (totals.deliveryFeeCents == 0L) "FREE" else formatCents(totals.deliveryFeeCents)
                                    )
                                    SummaryLine("Estimated Tax", formatCents(totals.taxCents))
                                    HorizontalDivider()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Total", style = MaterialTheme.typography.titleLarge)
                                        Text(
                                            text = formatCents(totals.totalCents),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    val addressValid = fulfillmentType == FulfillmentType.PICKUP ||
                                        deliveryAddress.trim().length >= 4

                                    Button(
                                        onClick = {
                                            onPlaceOrder(
                                                if (fulfillmentType == FulfillmentType.PICKUP) "Store Counter Pickup" else deliveryAddress,
                                                orderNotes.ifBlank { null },
                                                fulfillmentType,
                                                items
                                            )
                                        },
                                        enabled = settings.storeOpen && !isBusy && addressValid,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("place_order_button")
                                    ) {
                                        if (isBusy) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Placing Order...")
                                        } else {
                                            Text("Place Order • ${formatCents(totals.totalCents)}")
                                        }
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartItemRowCard(
    item: CartItem,
    maxStock: Long,
    category: ProductCategory,
    imageUrl: String?,
    onUpdateQty: (Long) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductArtworkBadge(
                iconKey = item.iconKey,
                category = category,
                imageUrl = imageUrl,
                contentDescription = item.productName,
                modifier = Modifier.size(56.dp),
                iconSize = 28.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatCents(item.unitPriceCents)} / ${item.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                QuantityStepper(
                    quantity = item.quantity,
                    maxStock = maxStock.coerceAtLeast(item.quantity),
                    onDecrement = { onUpdateQty(item.quantity - 1L) },
                    onIncrement = { onUpdateQty(item.quantity + 1L) }
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Remove ${item.productName}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    text = formatCents(item.lineTotalCents),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
