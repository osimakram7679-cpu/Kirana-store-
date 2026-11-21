package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.data.model.ProductCatalogSeed
import com.example.data.model.ProductCategory
import com.example.data.model.ProductDraft
import com.example.ui.components.productIconForKey
import java.util.Locale
import kotlin.math.roundToLong

private val UNIT_PRESETS = listOf("Each", "250g", "500g", "1kg", "500ml", "1 Litre", "Pack of 4", "Pack of 6")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProductEditorSheet(
    editingProduct: Product?,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (productId: String?, draft: ProductDraft, addAnother: Boolean) -> Unit
) {
    BackHandler(onBack = onDismiss)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(editingProduct) { mutableStateOf(editingProduct?.name ?: "") }
    var sku by remember(editingProduct) {
        mutableStateOf(editingProduct?.sku ?: "DG-SKU-${(100..999).random()}")
    }
    var description by remember(editingProduct) { mutableStateOf(editingProduct?.description ?: "") }
    var category by remember(editingProduct) {
        mutableStateOf(editingProduct?.categoryEnum ?: ProductCategory.PANTRY)
    }
    var unit by remember(editingProduct) { mutableStateOf(editingProduct?.unit ?: "Each") }
    var priceDollars by remember(editingProduct) {
        mutableStateOf(
            editingProduct?.let { String.format(Locale.US, "%.2f", it.priceCents / 100.0) } ?: ""
        )
    }
    var compareAtDollars by remember(editingProduct) {
        mutableStateOf(
            editingProduct?.compareAtPriceCents?.let {
                String.format(Locale.US, "%.2f", it / 100.0)
            } ?: ""
        )
    }
    var stockText by remember(editingProduct) {
        mutableStateOf(editingProduct?.stockCount?.toString() ?: "20")
    }
    var lowStockText by remember(editingProduct) {
        mutableStateOf(editingProduct?.lowStockThreshold?.toString() ?: "5")
    }
    var iconKey by remember(editingProduct) {
        mutableStateOf(editingProduct?.iconKey ?: "honey")
    }
    var imageUrl by remember(editingProduct) {
        mutableStateOf(editingProduct?.imageUrl ?: "")
    }
    var featured by remember(editingProduct) { mutableStateOf(editingProduct?.featured ?: false) }
    var active by remember(editingProduct) { mutableStateOf(editingProduct?.active ?: true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun validateAndBuildDraft(): ProductDraft? {
        val cleanName = name.trim()
        val cleanSku = sku.trim().uppercase()
        val parsedPrice = priceDollars.trim().toDoubleOrNull()
        val priceCents = parsedPrice?.let { (it * 100.0).roundToLong() } ?: 0L
        val parsedCompare = compareAtDollars.trim().toDoubleOrNull()
        val compareCents = parsedCompare?.let { (it * 100.0).roundToLong() }
        val stock = stockText.toLongOrNull() ?: -1L
        val lowThreshold = lowStockText.toLongOrNull() ?: 5L

        return when {
            cleanName.length < 2 -> {
                validationError = "Product name must be at least 2 characters."
                null
            }
            cleanSku.length < 2 -> {
                validationError = "SKU must be at least 2 characters."
                null
            }
            priceCents <= 0L -> {
                validationError = "Enter a valid price greater than $0.00."
                null
            }
            stock < 0L -> {
                validationError = "Enter a valid stock count (0 or higher)."
                null
            }
            else -> {
                validationError = null
                ProductDraft(
                    sku = cleanSku,
                    name = cleanName,
                    description = description.trim(),
                    category = category,
                    unit = unit.trim().ifBlank { "Each" },
                    priceCents = priceCents,
                    compareAtPriceCents = compareCents?.takeIf { it > 0L },
                    stockCount = stock,
                    lowStockThreshold = lowThreshold.coerceIn(0L, 1000L),
                    featured = featured,
                    active = active,
                    iconKey = iconKey,
                    imageUrl = imageUrl.trim().ifBlank { null }
                )
            }
        }
    }

    fun resetForNextProduct() {
        name = ""
        sku = "DG-SKU-${(100..999).random()}"
        description = ""
        priceDollars = ""
        compareAtDollars = ""
        stockText = "20"
        imageUrl = ""
        featured = false
        active = true
        validationError = null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (editingProduct == null) "Add Product" else "Edit Product",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Published items appear immediately in All Products",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close editor")
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Product Name *") },
                placeholder = { Text("e.g. Organic Basmati Rice") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_product_name_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it.uppercase() },
                    label = { Text("SKU *") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { sku = "DG-SKU-${(100..999).random()}" }) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Generate SKU",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit / Pack *") },
                    placeholder = { Text("500g, Each") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(UNIT_PRESETS, key = { it }) { preset ->
                    FilterChip(
                        selected = unit == preset,
                        onClick = { unit = preset },
                        label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Text("Department Category", style = MaterialTheme.typography.labelLarge)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(ProductCategory.entries, key = { it.name }) { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.shortLabel) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = priceDollars,
                    onValueChange = { priceDollars = it },
                    label = { Text("Price ($) *") },
                    placeholder = { Text("12.99") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_product_price_input")
                )
                OutlinedTextField(
                    value = compareAtDollars,
                    onValueChange = { compareAtDollars = it },
                    label = { Text("Compare At ($)") },
                    placeholder = { Text("Optional") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = stockText,
                    onValueChange = { stockText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Stock Count *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_product_stock_input")
                )
                OutlinedTextField(
                    value = lowStockText,
                    onValueChange = { lowStockText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Low Stock Alert") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Artwork Icon", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ProductCatalogSeed.iconChoices, key = { it.first }) { (key, label) ->
                    FilterChip(
                        selected = iconKey == key,
                        onClick = { iconKey = key },
                        leadingIcon = {
                            Icon(
                                imageVector = productIconForKey(key, category),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(label) }
                    )
                }
            }

            OutlinedTextField(
                value = imageUrl,
                onValueChange = { imageUrl = it },
                label = { Text("Custom Image URL (optional)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Visible in Storefront", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Turn off to keep SKU as a hidden draft",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = active, onCheckedChange = { active = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Featured Deal", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Highlight on Storefront home carousel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = featured, onCheckedChange = { featured = it })
            }

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    validateAndBuildDraft()?.let { draft ->
                        onSave(editingProduct?.id, draft, false)
                    }
                },
                enabled = !isBusy,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("admin_save_product_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving...")
                } else {
                    Text(if (editingProduct == null) "Publish Product" else "Save Changes")
                }
            }

            if (editingProduct == null) {
                OutlinedButton(
                    onClick = {
                        validateAndBuildDraft()?.let { draft ->
                            onSave(null, draft, true)
                            resetForNextProduct()
                        }
                    },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_save_and_add_another_button")
                ) {
                    Text("Save & Add Another Product")
                }
            }
        }
    }
}
