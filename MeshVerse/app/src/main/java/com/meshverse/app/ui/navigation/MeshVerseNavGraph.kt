package com.meshverse.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.meshverse.app.ui.screens.AiAssistantScreen
import com.meshverse.app.ui.screens.ChatScreen
import com.meshverse.app.ui.screens.EmergencyScreen
import com.meshverse.app.ui.screens.FeedScreen
import com.meshverse.app.ui.screens.GroupsScreen
import com.meshverse.app.ui.screens.MapScreen
import com.meshverse.app.ui.screens.MediaShareScreen
import com.meshverse.app.ui.screens.MiniAppsScreen
import com.meshverse.app.ui.screens.NearbyScreen
import com.meshverse.app.ui.screens.ProfileScreen
import com.meshverse.app.ui.screens.SettingsScreen
import com.meshverse.app.ui.viewmodel.MainViewModel

private data class Destination(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomDestinations = listOf(
    Destination("chat", "Chats", Icons.Default.Chat),
    Destination("nearby", "Nearby", Icons.Default.Menu),
    Destination("feed", "Feed", Icons.Default.Groups),
    Destination("map", "Maps", Icons.Default.Map),
    Destination("ai", "AI", Icons.Default.SmartToy),
    Destination("more", "More", Icons.Default.Build),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshVerseNavGraph(viewModel: MainViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route ?: "chat"
    val connectedPeers by viewModel.connectedPeerCount.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MeshVerse • ${if (connectedPeers > 0) "$connectedPeers peers" else "Local mode"}") }
            )
        },
        bottomBar = {
            NavigationBar {
                bottomDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.title) },
                        label = { Text(destination.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = "chat", modifier = Modifier.padding(innerPadding)) {
            composable("chat") { ChatScreen(viewModel) }
            composable("nearby") { NearbyScreen(viewModel) }
            composable("feed") { FeedScreen(viewModel) }
            composable("map") { MapScreen(viewModel) }
            composable("ai") { AiAssistantScreen(viewModel) }
            composable("more") { MoreScreen(navController) }
            composable("apps") { MiniAppsScreen() }
            composable("media") { MediaShareScreen(viewModel) }
            composable("groups") { GroupsScreen(viewModel) }
            composable("profile") { ProfileScreen(viewModel) }
            composable("settings") { SettingsScreen() }
            composable("emergency") { EmergencyScreen(viewModel) }
        }
    }
}

@Composable
private fun MoreScreen(navController: NavHostController) {
    val options = listOf(
        "Media Sharing" to "media",
        "Mini Apps" to "apps",
        "Groups" to "groups",
        "Profile" to "profile",
        "Settings" to "settings",
        "Emergency" to "emergency"
    )
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        options.forEach { (label, route) ->
            Text(
                text = label,
                modifier = Modifier
                    .semantics { role = Role.Button }
                    .clickable { navController.navigate(route) }
            )
        }
    }
}
