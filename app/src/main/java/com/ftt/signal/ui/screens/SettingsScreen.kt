package com.ftt.signal.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import com.ftt.signal.ui.theme.*

@Composable
fun SettingsScreen(
    apiBase: String,
    otcApiBase: String,
    onSave: (String, String) -> Unit,
) {
    var api by remember(apiBase)    { mutableStateOf(apiBase) }
    var otc by remember(otcApiBase) { mutableStateOf(otcApiBase) }
    var saved by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("API Settings", style = MaterialTheme.typography.headlineMedium)

        SettingField("API Base URL (Forex / Crypto)", api, { api = it; saved = false },
            "https://asignal.umuhammadiswa.workers.dev")
        SettingField("OTC API Base URL", otc, { otc = it; saved = false },
            "https://asignal.umuhammadiswa.workers.dev")

        Button(
            onClick = { onSave(api, otc); saved = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Brand1, contentColor = androidx.compose.ui.graphics.Color.White)
        ) { Text("Save", fontSize = 15.sp) }

        if (saved) {
            Text("✓ Saved!", color = BuyGreen, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        }

        Spacer(Modifier.height(24.dp))
        Text("About", style = MaterialTheme.typography.titleMedium, color = T3)
        Text("FTT Signal Native v6.5.2\nBuilt with Kotlin + Jetpack Compose",
            fontSize = 12.sp, color = T3)
    }
}

@Composable
private fun SettingField(label: String, value: String, onChange: (String) -> Unit, hint: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, color = T3, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = value, onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(hint, color = TMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = T1, fontFamily = FontFamily.Monospace),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent, unfocusedBorderColor = Divider,
                focusedTextColor = T1, unfocusedTextColor = T1, cursorColor = Accent),
            shape = RoundedCornerShape(12.dp),
        )
    }
}
