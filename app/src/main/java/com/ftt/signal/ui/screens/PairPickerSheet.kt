package com.ftt.signal.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.ftt.signal.ui.components.PairCategory
import com.ftt.signal.ui.components.PairData
import com.ftt.signal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairPickerSheet(
    selectedPair: String,
    onSelect:     (String) -> Unit,
    onDismiss:    () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(0) }

    val filteredCategories: List<PairCategory> = if (searchQuery.isBlank()) {
        PairData.CATEGORIES
    } else {
        PairData.CATEGORIES.map { cat ->
            cat.copy(pairs = cat.pairs.filter {
                it.contains(searchQuery.uppercase().trim())
            })
        }.filter { it.pairs.isNotEmpty() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Surface,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Text(
                "Select Pair",
                style      = MaterialTheme.typography.titleLarge,
                color      = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(bottom = 12.dp),
            )

            // Search
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder   = { Text("Search pairs...", color = TextMuted) },
                modifier      = Modifier.fillMaxWidth(),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Accent,
                    unfocusedBorderColor = Divider,
                    focusedTextColor     = TextPrimary,
                    unfocusedTextColor   = TextPrimary,
                    cursorColor          = Accent,
                ),
                shape         = RoundedCornerShape(10.dp),
                singleLine    = true,
            )

            Spacer(Modifier.height(12.dp))

            // Category tabs
            if (searchQuery.isBlank()) {
                Row(
                    modifier              = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PairData.CATEGORIES.forEachIndexed { idx, cat ->
                        val isSelected = idx == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AccentDim else SurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) Accent.copy(.5f) else Divider,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedCategory = idx }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        ) {
                            Text(
                                cat.name,
                                fontSize   = 12.sp,
                                color      = if (isSelected) Accent else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Pair list
            val displayCategories = if (searchQuery.isBlank()) {
                listOf(filteredCategories.getOrNull(selectedCategory) ?: PairData.CATEGORIES[0])
            } else {
                filteredCategories
            }

            LazyColumn(
                modifier            = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                displayCategories.forEach { cat ->
                    if (searchQuery.isNotBlank()) {
                        item {
                            Text(
                                cat.name,
                                style  = MaterialTheme.typography.labelLarge,
                                color  = TextMuted,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                            )
                        }
                    }
                    items(cat.pairs) { pair ->
                        PairItem(
                            pair        = pair,
                            isSelected  = pair == selectedPair,
                            isOTC       = PairData.isOTC(pair),
                            onClick     = { onSelect(pair) },
                        )
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun PairItem(
    pair:       String,
    isSelected: Boolean,
    isOTC:      Boolean,
    onClick:    () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) AccentDim else SurfaceVariant)
            .border(
                1.dp,
                if (isSelected) Accent.copy(.5f) else Color.Transparent,
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(pair, color = if (isSelected) Accent else TextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize   = 14.sp)
            if (isOTC) {
                Text("OTC", fontSize = 10.sp, color = Gold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Gold.copy(.1f))
                        .padding(horizontal = 5.dp, vertical = 2.dp))
            }
        }
        if (isSelected) {
            Text("✓", color = Accent, fontWeight = FontWeight.Bold)
        }
    }
}
