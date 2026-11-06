package com.example.apptrackerprogress

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cloudinary.android.MediaManager
import com.example.apptrackerprogress.domain.MaintenancePlanner
import com.example.apptrackerprogress.domain.isLowStock
import com.example.apptrackerprogress.ui.screens.*
import com.example.apptrackerprogress.ui.theme.AppTheme
import com.example.apptrackerprogress.ui.theme.ThemePrefs
import com.example.apptrackerprogress.viewmodel.*
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inicialización de Cloudinary (subida de fotos)
        try {
            MediaManager.init(this, mapOf("cloud_name" to "pbywmcgz"))
        } catch (e: Exception) {
            // Ya estaba inicializado
        }

        ThemePrefs.load(this) // Tema guardado (claro / oscuro / automático)

        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val workViewModel: WorkViewModel = viewModel()
    val inventoryViewModel: InventoryViewModel = viewModel()
    val vehicleViewModel: VehicleViewModel = viewModel()
    val context = LocalContext.current
    val toast: (String) -> Unit = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }

    // Estado compartido por varias pantallas
    val session by workViewModel.session.collectAsState()
    val works by workViewModel.works.collectAsState()
    val inventoryItems by inventoryViewModel.items.collectAsState()
    val vehicles by vehicleViewModel.vehicles.collectAsState()
    val isOwner = session?.isOwner ?: true

    // Se calcula una sola vez (si no, cambiaría en cada recomposición)
    val startDestination = remember { if (FirebaseAuth.getInstance().currentUser != null) "dashboard" else "login" }
    val back: () -> Unit = { navController.popBackStack() }
    val logout: () -> Unit = {
        FirebaseAuth.getInstance().signOut()
        navController.navigate("login") { popUpTo(0) { inclusive = true } }
    }

    NavHost(navController = navController, startDestination = startDestination) {

        // ---------------- Autenticación ----------------
        composable("login") {
            LoginScreen(
                onLoginClick = { email, password ->
                    authViewModel.loginUser(
                        email = email,
                        password = password,
                        onSuccess = {
                            Toast.makeText(context, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                            navController.navigate("dashboard") { popUpTo("login") { inclusive = true } }
                        },
                        onError = toast
                    )
                },
                onGoogleLoginClick = { toast("Inicio con Google próximamente") },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegisterClick = { name, email, password, photoUri ->
                    authViewModel.registerUser(
                        name = name,
                        email = email,
                        password = password,
                        imageUri = photoUri,
                        onSuccess = {
                            Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show()
                            navController.navigate("dashboard") { popUpTo("login") { inclusive = true } }
                        },
                        onError = toast
                    )
                },
                onNavigateToLogin = back
            )
        }

        // ---------------- Trabajos ----------------
        composable("dashboard") {
            val now = System.currentTimeMillis()
            DashboardScreen(
                workList = works,
                workshopName = session?.workshopName ?: "",
                isOwner = isOwner,
                lowStockCount = inventoryItems.count { it.isLowStock() },
                maintenanceDueCount = vehicles.count {
                    it.maintenanceDue(now) in listOf(MaintenancePlanner.Due.SOON, MaintenancePlanner.Due.OVERDUE)
                },
                onLogoutClick = logout,
                onAddWorkClick = { navController.navigate("add_work") },
                onWorkItemClick = { workId -> navController.navigate("work_detail/$workId") },
                onDeleteClick = { work, returnStock ->
                    workViewModel.deleteWork(
                        work, returnStock,
                        onSuccess = { toast(if (returnStock) "Trabajo eliminado y repuestos devueltos al inventario" else "Trabajo eliminado") },
                        onError = toast
                    )
                },
                onProfileClick = { navController.navigate("profile") },
                onInventoryClick = { navController.navigate("inventory") },
                onVehiclesClick = { navController.navigate("vehicles") },
                onStatsClick = { navController.navigate("stats") }
            )
        }

        composable("add_work") {
            AddWorkScreen(
                vehicles = vehicles,
                onSaveClick = { form, onFailed ->
                    workViewModel.addWork(
                        form = form,
                        onSuccess = {
                            Toast.makeText(context, "Trabajo creado correctamente", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        onError = { error -> onFailed(); toast(error) }
                    )
                },
                onBackClick = back
            )
        }

        composable("work_detail/{workId}") { entry ->
            WorkDetailScreen(
                workId = entry.arguments?.getString("workId") ?: "",
                workViewModel = workViewModel,
                inventoryViewModel = inventoryViewModel,
                onBackClick = back,
                onOpenVehicle = { id -> navController.navigate("vehicle/$id") }
            )
        }

        // ---------------- Vehículos ----------------
        composable("vehicles") {
            VehiclesScreen(vehicleViewModel, onBackClick = back, onOpenVehicle = { id -> navController.navigate("vehicle/$id") })
        }
        composable("vehicle/{vehicleId}") { entry ->
            VehicleDetailScreen(
                vehicleViewModel = vehicleViewModel,
                vehicleId = entry.arguments?.getString("vehicleId") ?: "",
                works = works,
                onBackClick = back,
                onOpenWork = { id -> navController.navigate("work_detail/$id") }
            )
        }

        // ---------------- Inventario ----------------
        composable("inventory") {
            InventoryScreen(
                inventoryViewModel = inventoryViewModel,
                isOwner = isOwner,
                onBackClick = back,
                onOpenCategory = { id -> navController.navigate("inventory/$id") }
            )
        }
        composable("inventory/{categoryId}") { entry ->
            InventoryCategoryScreen(
                inventoryViewModel = inventoryViewModel,
                categoryId = entry.arguments?.getString("categoryId") ?: NO_CATEGORY,
                isOwner = isOwner,
                onBackClick = back
            )
        }

        // ---------------- Estadísticas (solo dueño) ----------------
        composable("stats") {
            val statsViewModel: StatsViewModel = viewModel() // Se crea al abrir: datos de pago frescos
            StatsScreen(statsViewModel, onBackClick = back)
        }

        // ---------------- Perfil y configuración ----------------
        composable("profile") {
            ProfileScreen(
                authViewModel = authViewModel,
                totalWorks = works.size,
                onBackClick = back,
                onSettingsClick = { navController.navigate("settings") },
                onLogoutClick = logout
            )
        }
        composable("settings") { SettingsScreen(onBackClick = back, onOpen = { route -> navController.navigate(route) }) }
        composable("settings/name") { EditNameScreen(authViewModel, onBackClick = back) }
        composable("settings/photo") { EditPhotoScreen(authViewModel, onBackClick = back) }
        composable("settings/password") { ChangePasswordScreen(authViewModel, onBackClick = back) }
        composable("settings/theme") { ThemeScreen(onBackClick = back) }
        composable("settings/workshop") {
            val workshopViewModel: WorkshopViewModel = viewModel()
            WorkshopScreen(workshopViewModel, onBackClick = back)
        }
    }
}
