package com.example.apptrackerprogress


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.apptrackerprogress.ui.screens.DashboardScreen
import com.example.apptrackerprogress.ui.screens.LoginScreen
import com.example.apptrackerprogress.ui.screens.RegisterScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.apptrackerprogress.ui.screens.AddWorkScreen
import com.example.apptrackerprogress.ui.screens.WorkDetailScreen
import com.example.apptrackerprogress.viewmodel.AuthViewModel
import com.example.apptrackerprogress.viewmodel.WorkViewModel
import com.cloudinary.android.MediaManager

// IMPORTANTE: Asegúrate de mantener la importación del tema de tu app que Android Studio
// generó automáticamente (por ejemplo: com.tudominio.tuapp.ui.theme.TallerTrackerTheme)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // AQUÍ AGREGAS LA INICIALIZACIÓN DE CLOUDINARY
        val config = mapOf(
            "cloud_name" to "pbywmcgz" // Tu Cloud Name de Cloudinary
        )
        try {
            MediaManager.init(this, config)
        } catch (e: Exception) {
            // Previene cierres si ya estaba inicializado
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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
    val context = LocalContext.current

    // Verificamos si ya hay un usuario con sesión iniciada en Firebase
    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    val startDestination = if (currentUser != null) "dashboard" else "login"

    // Pasamos startDestination dinámicamente al NavHost
    NavHost(navController = navController, startDestination = startDestination) {

        composable("login") {
            LoginScreen(
                onLoginClick = { email, password ->
                    authViewModel.loginUser(
                        email = email,
                        password = password,
                        onSuccess = {
                            Toast.makeText(context, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                            navController.navigate("dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onError = { errorMessage ->
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                onGoogleLoginClick = {
                    // TODO: Google Sign In
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegisterClick = { name, email, password, photoUri ->
                    // TODO: Aquí agregaremos la creación de cuenta en Firebase
                    authViewModel.registerUser(
                        name = name,
                        email = email,
                        password = password,
                        imageUri = photoUri,
                        onSuccess = {
                            Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show()
                            // Aquí en el futuro navegaremos a la pantalla principal (Dashboard)
                            // Por ahora, volvemos al login
                            navController.navigate("dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onError = { errorMessage ->
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                onNavigateToLogin = {
                    // Volvemos atrás en la pila de pantallas para regresar al login
                    navController.popBackStack()
                }
            )
        }

        composable("add_work") {
            AddWorkScreen(
                onSaveClick = { clientName, vehicleName, description ->
                    workViewModel.addWork(
                        clientName = clientName,
                        vehicleName = vehicleName,
                        description = description,
                        onSuccess = {
                            Toast.makeText(context, "Trabajo creado correctamente", Toast.LENGTH_SHORT).show()
                            navController.popBackStack() // Volvemos al dashboard
                        },
                        onError = { error ->
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable("work_detail/{workId}") { backStackEntry ->
            val workId = backStackEntry.arguments?.getString("workId") ?: ""
            WorkDetailScreen(
                workId = workId,
                workViewModel = workViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable("dashboard") {
            DashboardScreen(
                workList = workViewModel.workList.value, // Conectamos la lista en tiempo real
                onLogoutClick = {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                    navController.navigate("login") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                },
                onAddWorkClick = {
                    navController.navigate("add_work")
                },
                onWorkItemClick = { workId ->
                    // Navegar al detalle del trabajo
                    navController.navigate("work_detail/$workId")
                },
                onDeleteClick = { workId ->
                    workViewModel.deleteWork(
                        workId = workId,
                        onSuccess = {
                            Toast.makeText(context, "Trabajo eliminado", Toast.LENGTH_SHORT).show()
                        },
                        onError = { error ->
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }
    }
}