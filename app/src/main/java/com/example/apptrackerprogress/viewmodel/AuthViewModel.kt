package com.example.apptrackerprogress.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Función principal para registrar
    fun registerUser(
        name: String,
        email: String,
        password: String,
        imageUri: Uri?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            onError("Por favor, llena todos los campos")
            return
        }

        // 1. Crear el usuario en Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: return@addOnSuccessListener

                // 2. Si hay foto, subirla a Storage
                if (imageUri != null) {
                    val storageRef = storage.reference.child("profile_pictures/$uid.jpg")
                    storageRef.putFile(imageUri)
                        .addOnSuccessListener {
                            // Obtener el link de descarga de la foto
                            storageRef.downloadUrl.addOnSuccessListener { uri ->
                                saveUserToFirestore(uid, name, email, uri.toString(), onSuccess, onError)
                            }
                        }
                        .addOnFailureListener {
                            // Si falla la foto, guardamos el usuario sin foto
                            saveUserToFirestore(uid, name, email, "", onSuccess, onError)
                        }
                } else {
                    // Si no seleccionó foto, guardamos con texto vacío
                    saveUserToFirestore(uid, name, email, "", onSuccess, onError)
                }
            }
            .addOnFailureListener { exception ->
                onError(exception.localizedMessage ?: "Error al registrar usuario")
            }
    }

    // Función para iniciar sesión con correo y contraseña
    fun loginUser(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (email.isBlank() || password.isBlank()) {
            onError("Por favor, ingresa tu correo y contraseña")
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception.localizedMessage ?: "Credenciales incorrectas o error al iniciar sesión")
            }
    }

    // 3. Guardar los datos en Firestore
    private fun saveUserToFirestore(
        uid: String,
        name: String,
        email: String,
        photoUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userMap = hashMapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "photoUrl" to photoUrl,
            "role" to "trabajador" // Guardamos el rol para futuras validaciones
        )

        db.collection("users").document(uid).set(userMap)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception ->
                onError(exception.localizedMessage ?: "Error al guardar en base de datos")
            }
    }
}