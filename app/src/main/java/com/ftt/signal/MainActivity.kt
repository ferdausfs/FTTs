package com.ftt.signal

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.ftt.signal.ui.components.PairPickerSheet
import com.ftt.signal.ui.screens.*
import com.ftt.signal.ui.theme.*
import com.ftt.signal.viewmodel.*
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single Activity — entry point of the app.
 *
 * @AndroidEntryPoint enables Hilt injection in this Activity.
 * ViewModels are created via [hiltViewModel] in the composable tree,
 * so no manual ViewModelProvider calls are needed.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep screen on while the signal dashboard is visible
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        enableEdgeToEdge()
        setContent { FttTheme { FttApp() } }
    }
}

// ── Root composable ──────────────────────────────────────────

@Composable
fun FttApp() {
    // hiltViewModel() creates or retrieves the scoped ViewModel from
    // the Hilt component — no ViewModelProvider needed
    val sigVm: SignalViewModel    = hiltViewModel()
    val jrnVm: JournalViewModel   = hiltViewModel()
    val wlVm:  WatchlistViewModel = hiltViewModel()

    val nav          = rememberNavController()
    val sigState     by sigVm.state.collectAsStateWithLifecycle()
    val curPair      by sigVm.curPair.collectAsStateWithLifecycle()
    val soundOn      by sigVm.soundOn.collectAsStateWithLifecycle()
    val slPips       by sigVm.slPips.collectAsStateWithLifecycle()
    val tpPips       by sigVm.tpPips.collectAsStateWithLifecycle()
    val apiBase      by sigVm.apiBase.collectAsStateWithLifecycle()
    val otcBase      by sigVm.otcApiBase.collectAsStateWithLifecycle()
    val journals     by jrnVm.allEntries.collectAsStateWithLifecycle()
    val wlState      by wlVm.state.collectAsStateWithLifecycle()
    val context      = androidx.compose.ui.platform.LocalContext.current
    val prefs        = remember(context) { com.ftt.signal.prefs.AppPrefs(context) }
    val lotSize      by prefs.lotSize.collectAsStateWithLifecycle(initialValue = 0.1f)
    val pipValue     by prefs.pipValue.collectAsStateWithLifecycle(initialValue = 10f)
    var showPicker   by remember { mutableStateOf(false) }

    val tabs = listOf(
        "signal"    to "\uD83D\uDCCA",
        "tf"        to "\uD83D\uDCD0",
        "journal"   to "\uD83D\uDCD3",
        "watchlist" to "\uD83D\uDC41",
        "analytics" to "\uD83D\uDCC8",
    )

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            NavigationBar(
                containerColor  = S2,
                tonalElevation  = 0.dp,
                modifier        = Modifier.height(64.dp),
            ) {
                val back by nav.currentBackStackEntryAsState()
                val cur  = back?.destination
                tabs.forEach { (route, icon) ->
                    val sel   = cur?.hierarchy?.any { it.route == route } == true
                    val label = route.replaceFirstChar { it.uppercase() }
                    val badge = if (route == "watchlist" && wlState.newCount > 0) wlState.newCount else 0
                    NavigationBarItem(
                        selected = sel,
                        onClick  = {
                            nav.navigate(route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        icon  = {
                            BadgedBox(badge = {
                                if (badge > 0) Badge { Text("$badge", fontSize = 8.sp) }
                            }) {
                                Text(icon, fontSize = 18.sp)
                            }
                        },
                        label  = {
                            Text(
                                label,
                                fontSize   = 10.sp,
                                fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = Accent,
                            selectedTextColor   = Accent,
                            unselectedIconColor = T3,
                            unselectedTextColor = T3,
                            indicatorColor      = AccentDim,
                        ),
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            NavHost(nav, startDestination = "signal") {
                composable("signal") {
                    SignalScreen(
                        state          = sigState,
                        curPair        = curPair,
                        slPips         = slPips,
                        tpPips         = tpPips,
                        soundOn        = soundOn,
                        onRefresh      = { sigVm.fetchSignal(forceRefresh = true) },
                        onToggleSound  = sigVm::toggleSound,
                        onSaveSLTP     = sigVm::saveSLTP,
                        onOpenSettings = { nav.navigate("settings") },
                        calcSLTP       = sigVm::calcSLTP,
                    )
                }
                composable("tf") {
                    TfScreen(signal = sigState.signal)
                }
                composable("journal") {
                    JournalScreen(
                        journals,
                        jrnVm::markResult,
                        jrnVm::delete,
                        jrnVm::saveNote,
                        jrnVm::clearAll,
                    )
                }
                composable("watchlist") {
                    WatchlistScreen(
                        state         = wlState,
                        onToggleScan  = wlVm::toggleScan,
                        onRunScan     = wlVm::runScan,
                        onAddPair     = wlVm::addPair,
                        onRemovePair  = wlVm::removePair,
                        onSetInterval = wlVm::setInterval,
                        onSetFilter   = wlVm::setFilter,
                        onSetSort     = wlVm::setSort,
                        onPairClick   = { pair ->
                            sigVm.selectPair(pair)
                            nav.navigate("signal") {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        onAddToJournal  = wlVm::addToJournal,
                        onClearNewCount = wlVm::clearNewCount,
                    )
                }
                composable("analytics") {
                    AnalyticsScreen(
                        entries    = journals,
                        lotSize    = lotSize,
                        pipValue   = pipValue,
                        onSavePL   = sigVm::saveLotPip,
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        apiBase    = apiBase,
                        otcApiBase = otcBase,
                        onSave     = sigVm::saveApiSettings,
                    )
                }
            }

            if (showPicker) {
                PairPickerSheet(
                    currentPair = curPair,
                    onSelect    = { sigVm.selectPair(it) },
                    onDismiss   = { showPicker = false },
                )
            }
        }
    }
}
