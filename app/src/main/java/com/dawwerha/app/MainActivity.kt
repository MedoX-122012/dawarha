package com.dawwerha.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.dawwerha.app.data.DawwerhaRepository
import com.dawwerha.app.data.Role
import com.dawwerha.app.ui.*

object Brand {
    val DarkGreen = Color(0xFF1B4332)
    val NaturalGreen = Color(0xFF40916C)
    val LightGreen = Color(0xFFD8F3DC)
}

private val LightScheme = lightColorScheme(
    primary = Brand.DarkGreen, onPrimary = Color.White,
    secondary = Brand.NaturalGreen, onSecondary = Color.White,
    primaryContainer = Brand.LightGreen, onPrimaryContainer = Brand.DarkGreen,
    background = Color.White, surface = Color.White
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF95D5B2), onPrimary = Color(0xFF00210F),
    secondary = Brand.NaturalGreen,
    primaryContainer = Color(0xFF2D6A4F), onPrimaryContainer = Color.White,
    background = Color(0xFF0F1F17), surface = Color(0xFF16291F)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    DawwerhaRoot()
                }
            }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "الرئيسية", Icons.Default.Home),
    Tab("scan", "الماسح", Icons.Default.CameraAlt),
    Tab("market", "السوق", Icons.Default.Storefront),
    Tab("impact", "الأثر", Icons.Default.Eco),
    Tab("profile", "حسابي", Icons.Default.Person)
)

@Composable
fun DawwerhaRoot() {
    val repo: DawwerhaRepository = (LocalContext.current.applicationContext as DawwerhaApp).repository
    val nav = rememberNavController()
    val user by repo.currentUser.collectAsState(initial = null)
    val backEntry by nav.currentBackStackEntryAsState()
    val route = backEntry?.destination?.route

    // التوجيه التلقائي حسب حالة الدخول
    LaunchedEffect(user?.id) {
        val current = nav.currentBackStackEntry?.destination?.route
        val onAuth = current == null || current == "login" || current == "register"
        if (user != null && onAuth) nav.navigate("home") { popUpTo(0) { inclusive = true } }
        if (user == null && !onAuth) nav.navigate("login") { popUpTo(0) { inclusive = true } }
    }

    Scaffold(
        bottomBar = {
            if (tabs.any { it.route == route }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo("home")
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = "login", modifier = Modifier.padding(padding)) {
            composable("login") { LoginScreen(repo) { nav.navigate("register") } }
            composable("register") { RegisterScreen(repo) { nav.popBackStack() } }
            composable("home") { user?.let { HomeScreen(repo, it, nav) } }
            composable("scan") { user?.let { ScanScreen(repo, it, nav) } }
            composable("market") { MarketScreen(repo, nav) }
            composable("market/create") {
                user?.let { CreateListingScreen(repo, it) { nav.popBackStack() } }
            }
            composable(
                "listing/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                user?.let { ListingDetailScreen(repo, it, id) { nav.popBackStack() } }
            }
            composable("impact") { user?.let { ImpactScreen(repo, it) } }
            composable("ideas") { user?.let { IdeasScreen(repo, it) { nav.popBackStack() } } }
            composable("profile") { user?.let { ProfileScreen(repo, it, nav) } }
            composable("admin") {
                user?.let { if (it.role == Role.ADMIN) AdminScreen(repo, it) { nav.popBackStack() } }
            }
        }
    }
}
