package com.ftt.signal

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.ftt.signal.ui.screens.HistoryScreen
import com.ftt.signal.ui.screens.SignalScreen
import com.ftt.signal.ui.theme.*
import com.ftt.signal.viewmodel.SignalViewModel

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* silent */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        enableEdgeToEdge()
        setContent {
            FttsTheme {
                FttsApp()
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: String) {
    object Signal  : Screen("signal",  "Signal",  "📊")
    object History : Screen("history", "History", "📈")
}

private val navItems = listOf(Screen.Signal, Screen.History)

@Composable
fun FttsApp() {
    val navController = rememberNavController()
    val viewModel: SignalViewModel = viewModel()

    val signalState  by viewModel.signalState.collectAsStateWithLifecycle()
    val historyState by viewModel.historyState.collectAsStateWithLifecycle()
    val selectedPair by viewModel.selectedPair.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Background,
        bottomBar = { FttsBottomBar(navController = navController) },
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Signal.route,
            modifier         = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            composable(Screen.Signal.route) {
                SignalScreen(
                    uiState      = signalState,
                    selectedPair = selectedPair,
                    onPairSelect = { pair -> viewModel.selectPair(pair) },
                    onRefresh    = { viewModel.fetchSignal() },
                    onToggleAuto = { viewModel.toggleAutoRefresh() },
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    uiState      = historyState,
                    selectedPair = selectedPair,
                    onRefresh    = { viewModel.fetchHistory() },
                    onReport     = { id, result -> viewModel.reportResult(id, result) },
                )
            }
        }
    }
}

@Composable
private fun FttsBottomBar(navController: androidx.navigation.NavController) {
    val navBackStackEntry  by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        containerColor = Surface,
        tonalElevation = 0.dp,
        modifier       = Modifier.height(64.dp),
    ) {
        navItems.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                selected = selected,
                onClick  = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState    = true
                    }
                },
                icon  = { Text(screen.icon, fontSize = 20.sp) },
                label = {
                    Text(
                        screen.label,
                        fontSize   = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = Accent,
                    selectedTextColor   = Accent,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                    indicatorColor      = AccentDim,
                ),
            )
        }
    }
}
