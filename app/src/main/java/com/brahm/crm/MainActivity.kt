package com.brahm.crm

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.brahm.crm.ui.AppViewModel
import com.brahm.crm.ui.BrahmTheme
import com.brahm.crm.ui.screens.DashboardScreen
import com.brahm.crm.ui.screens.CallReplyScreen
import com.brahm.crm.ui.screens.ChatThreadScreen
import com.brahm.crm.ui.screens.InboxScreen
import com.brahm.crm.ui.screens.KanbanScreen
import com.brahm.crm.ui.screens.LeadsScreen
import com.brahm.crm.ui.screens.SettingsScreen
import com.brahm.crm.ui.screens.WhatsAppWebScreen
import kotlinx.coroutines.launch

private enum class Dest(val route: String, val title: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "Dashboard", Icons.Filled.Dashboard),
    LEADS("leads", "Leads (CRM)", Icons.Filled.People),
    KANBAN("kanban", "Board", Icons.Filled.ViewKanban),
    INBOX("inbox", "Inbox", Icons.Filled.Inbox),
    CALL_REPLY("call_reply", "Call Auto-Reply", Icons.Filled.PhoneCallback),
    WHATSAPP_WEB("wa_web", "WhatsApp Web", Icons.Filled.Language),
    SETTINGS("settings", "Settings", Icons.Filled.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: AppViewModel = viewModel()
            val systemDark = isSystemInDarkTheme()
            val dark = when (vm.themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemDark
            }
            BrahmTheme(darkTheme = dark) {
                AppRoot(vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot(vm: AppViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Dest.DASHBOARD.route
    val current = Dest.entries.firstOrNull { it.route == currentRoute } ?: Dest.DASHBOARD
    // Full-screen routes (their own toolbar) hide the app chrome.
    val fullScreen = currentRoute.startsWith("thread/")

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !fullScreen,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(20.dp))
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text("BRAHM CRM", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Leads · Board · Inbox", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(16.dp))
                Dest.entries.forEach { dest ->
                    NavigationDrawerItem(
                        icon = { Icon(dest.icon, contentDescription = null) },
                        label = { Text(dest.title) },
                        selected = backStack?.destination?.hierarchy?.any { it.route == dest.route } == true,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (currentRoute != dest.route) {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (!fullScreen) {
                    TopAppBar(
                        title = { Text(current.title, fontWeight = FontWeight.SemiBold) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu")
                            }
                        }
                    )
                }
            }
        ) { inner ->
            NavHost(
                navController = navController,
                startDestination = Dest.DASHBOARD.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
            ) {
                composable(Dest.DASHBOARD.route) { DashboardScreen(vm) }
                composable(Dest.LEADS.route) { LeadsScreen(vm) }
                composable(Dest.KANBAN.route) { KanbanScreen(vm) }
                composable(Dest.INBOX.route) {
                    InboxScreen(vm, onOpenPerson = { person ->
                        navController.navigate("thread/" + java.net.URLEncoder.encode(person, "UTF-8"))
                    })
                }
                composable(Dest.CALL_REPLY.route) { CallReplyScreen(vm) }
                composable(Dest.WHATSAPP_WEB.route) { WhatsAppWebScreen() }
                composable(Dest.SETTINGS.route) { SettingsScreen(vm) }
                composable("thread/{person}") { backStackEntry ->
                    val enc = backStackEntry.arguments?.getString("person") ?: ""
                    val person = runCatching { java.net.URLDecoder.decode(enc, "UTF-8") }.getOrDefault(enc)
                    ChatThreadScreen(vm, person, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
