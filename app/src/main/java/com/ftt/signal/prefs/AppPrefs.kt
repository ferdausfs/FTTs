package com.ftt.signal.prefs

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("ftt_prefs")

class AppPrefs(private val context: Context) {
    companion object {
        val API_BASE      = stringPreferencesKey("api_base")
        val OTC_API_BASE  = stringPreferencesKey("otc_api_base")
        val CUR_PAIR      = stringPreferencesKey("cur_pair")
        val SOUND_ON      = booleanPreferencesKey("sound_on")
        val SL_PIPS       = floatPreferencesKey("sl_pips")
        val TP_PIPS       = floatPreferencesKey("tp_pips")
        val WL_PAIRS      = stringPreferencesKey("wl_pairs")
        val WL_INTERVAL   = intPreferencesKey("wl_interval")
        val ALERT_THRESH  = intPreferencesKey("alert_thresh")
        val LOT_SIZE      = floatPreferencesKey("lot_size")
        val PIP_VALUE     = floatPreferencesKey("pip_value")
        val API_USED      = intPreferencesKey("api_used")
        val API_DATE      = stringPreferencesKey("api_date")

        const val DEFAULT_API     = "https://asignal.umuhammadiswa.workers.dev"
        const val DEFAULT_OTC_API = "https://asignal.umuhammadiswa.workers.dev"
    }

    val apiBase: Flow<String>     = context.dataStore.data.map { it[API_BASE]     ?: DEFAULT_API }
    val otcApiBase: Flow<String>  = context.dataStore.data.map { it[OTC_API_BASE] ?: DEFAULT_OTC_API }
    val curPair: Flow<String>     = context.dataStore.data.map { it[CUR_PAIR]     ?: "EUR/USD" }
    val soundOn: Flow<Boolean>    = context.dataStore.data.map { it[SOUND_ON]     ?: true }
    val slPips: Flow<Float>       = context.dataStore.data.map { it[SL_PIPS]      ?: 15f }
    val tpPips: Flow<Float>       = context.dataStore.data.map { it[TP_PIPS]      ?: 30f }
    val wlPairsStr: Flow<String>  = context.dataStore.data.map { it[WL_PAIRS]     ?: "" }
    val wlInterval: Flow<Int>     = context.dataStore.data.map { it[WL_INTERVAL]  ?: 1 }
    val alertThresh: Flow<Int>    = context.dataStore.data.map { it[ALERT_THRESH] ?: 100 }
    val lotSize: Flow<Float>      = context.dataStore.data.map { it[LOT_SIZE]     ?: 0.1f }
    val pipValue: Flow<Float>     = context.dataStore.data.map { it[PIP_VALUE]    ?: 10f }
    val apiUsed: Flow<Int>        = context.dataStore.data.map { it[API_USED]     ?: 0 }
    val apiDate: Flow<String>     = context.dataStore.data.map { it[API_DATE]     ?: "" }

    suspend fun set(key: Preferences.Key<String>,  v: String)  = context.dataStore.edit { it[key] = v }
    suspend fun set(key: Preferences.Key<Boolean>, v: Boolean) = context.dataStore.edit { it[key] = v }
    suspend fun set(key: Preferences.Key<Float>,   v: Float)   = context.dataStore.edit { it[key] = v }
    suspend fun set(key: Preferences.Key<Int>,     v: Int)     = context.dataStore.edit { it[key] = v }
}
