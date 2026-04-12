package com.ftt.signal.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import com.ftt.signal.data.model.PairData
import com.ftt.signal.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairPickerSheet(
    currentPair: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query    by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    val cats     = listOf("All", "Forex", "Crypto", "OTC")
    val fm       = LocalFocusManager.current

    val base = when (category) {
        "Forex"  -> PairData.FX
        "Crypto" -> PairData.CRYPTO
        "OTC"    -> PairData.OTC
        else     -> PairData.ALL
    }
    val filtered = if (query.isBlank()) base else {
        val q = query.uppercase().replace(Regex("[^A-Z0-9]"), "")
        base.filter { it.replace(Regex("[/\\-]"), "").contains(q) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor  = S2,
        shape           = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle      = { Box(Modifier.padding(top = 12.dp).width(40.dp).height(4.dp)
            .clip(RoundedCornerShape(2.dp)).background(Divider)) },
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            // Title
            Text("Select Pair", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 12.dp))

            // Search
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Search...", color = T3, fontSize = 13.sp) },
                modifier    = Modifier.fillMaxWidth(),
                singleLine  = true,
                colors      = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Accent,
                    unfocusedBorderColor = Divider,
                    focusedTextColor     = T1,
                    unfocusedTextColor   = T1,
                    cursorColor          = Accent,
                ),
                shape              = RoundedCornerShape(12.dp),
                keyboardOptions    = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions    = KeyboardActions(onDone = { fm.clearFocus() }),
            )

            Spacer(Modifier.height(12.dp))

            // Category tabs
            if (query.isBlank()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cats.forEach { cat ->
                        val sel = cat == category
                        val catCol = if (cat == "All") Accent else when (cat) {
                            "Forex" -> Accent; "Crypto" -> BuyGreen; "OTC" -> WaitYellow; else -> T3 }
                        Box(
                            Modifier.clip(RoundedCornerShape(8.dp))
                                .background(if (sel) catCol.copy(0.15f) else S3)
                                .border(1.dp, if (sel) catCol.copy(0.4f) else Divider, RoundedCornerShape(8.dp))
                                .clickable { category = cat }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(cat, fontSize = 12.sp, color = if (sel) catCol else T3,
                                fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Pair grid
            LazyVerticalGrid(
                columns              = GridCells.Fixed(3),
                verticalArrangement  = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier             = Modifier.heightIn(max = 380.dp),
            ) {
                items(filtered) { pair ->
                    val sel    = pair == currentPair
                    val catCol = when (PairData.category(pair)) {
                        "Crypto" -> BuyGreen; "OTC" -> WaitYellow; else -> Accent }
                    Box(
                        Modifier.clip(RoundedCornerShape(10.dp))
                            .background(if (sel) catCol.copy(0.15f) else S3)
                            .border(1.dp, if (sel) catCol.copy(0.5f) else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { onSelect(pair); onDismiss() }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(pair.replace("-OTC", ""), fontSize = 10.sp,
                                color = if (sel) catCol else T1,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace)
                            if (PairData.isOTC(pair))
                                Text("OTC", fontSize = 8.sp, color = WaitYellow,
                                    fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}
