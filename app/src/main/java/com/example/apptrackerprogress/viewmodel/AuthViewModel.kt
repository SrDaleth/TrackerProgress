package com.example.apptrackerprogress.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
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
                val uid = authResult.user?.uid
                if (uid == null) {
                    onError("Error al registrar usuario")
                    return@addOnSuccessListener
                }

                // 2. Si hay foto, subirla a Storage
                if (imageUri != null) {
                    val storageRef = storage.reference.child("profile_pictures/$uid.jpg")
                    storageRef.putFile(imageUri)
                        .addOnSuccessListener {
                            // Obtener el link de descarga de la foto
                            storageRef.downloadUrl
                                .addOnSuccessListener { uri ->
                                    saveUserToFirestore(uid, name, email, uri.toString(), onSuccess, onError)
                                }
                                .addOnFailureListener {
                                    // Sin URL de descarga: guardamos el usuario sin foto
                                    saveUserToFirestore(uid, name, email, "", onSuccess, onError)
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

    // Perfil del usuario con sesión iniciada (documento users/{uid})
    fun loadProfile(onLoaded: (Map<String, Any?>) -> Unit, onError: (String) -> Unit) {
        val user = auth.currentUser ?: return onError("No hay sesión iniciada")
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                // Si no existe el documento, usamos los datos básicos de Auth
                onLoaded(doc.data ?: mapOf("name" to (user.displayName ?: ""), "email" to (user.email ?: "")))
            }
            .addOnFailureListener { e -> onError(e.localizedMessage ?: "Error al cargar el perfil") }
    }

    fun updateName(name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = auth.currentUser ?: return onError("No hay sesión iniciada")
        if (name.isBlank()) return onError("El nombre no puede estar vacío")
        val ref = db.collection("users").document(user.uid)
        val fail: (Exception) -> Unit = { e -> onError(e.localizedMessage ?: "Error al actualizar el nombre") }

        // Si el perfil no existe (cuentas antiguas o registro incompleto) lo creamos completo
        ref.get().addOnSuccessListener { doc ->
            val task = if (doc.exists()) {
                ref.update("name", name.trim())
            } else {
                ref.set(
                    hashMapOf(
                        "uid" to user.uid,
                        "name" to name.trim(),
                        "email" to (user.email ?: ""),
                        "photoUrl" to (user.photoUrl?.toString() ?: ""),
                        "role" to "trabajador"
                    )
                )
            }
            task.addOnSuccessListener { onSuccess() }.addOnFailureListener(fail)
        }.addOnFailureListener(fail)
    }

    // Cambia la foto de perfil: se sube a Cloudinary (igual que los avances) y se guarda la URL
    fun updatePhoto(imageUri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = auth.currentUser ?: return onError("No hay sesión iniciada")
        MediaManager.get().upload(imageUri)
            .unsigned("tracker_progress")
            .option("folder", "profile_pictures")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
                override fun onError(requestId: String, error: ErrorInfo) {
                    onError("Error al subir la foto: ${error.description}")
                }
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val url = resultData["secure_url"] as? String ?: ""
                    // merge: funciona aunque el documento del perfil aún no exista
                    db.collection("users").document(user.uid)
                        .set(mapOf("photoUrl" to url), SetOptions.merge())
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { e -> onError(e.localizedMessage ?: "Error al guardar la foto") }
                }
            }).dispatch()
    }

    // Cambia la contraseña: Firebase exige volver a autenticarse con la contraseña actual
    fun changePassword(current: String, new: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = auth.currentUser ?: return onError("No hay sesión iniciada")
        val email = user.email ?: return onError("Tu cuenta no usa correo y contraseña")
        if (new.length < 6) return onError("La nueva contraseña debe tener al menos 6 caracteres")

        user.reauthenticate(EmailAuthProvider.getCredential(email, current))
            .addOnSuccessListener {
                user.updatePassword(new)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onError(e.localizedMessage ?: "Error al cambiar la contraseña") }
            }
            .addOnFailureListener { onError("La contraseña actual no es correcta") }
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
                // Revertimos la cuenta de Auth para no dejar un usuario "a medias"
                // (así el usuario puede volver a registrarse con el mismo correo)
                val message = exception.localizedMessage ?: "Error al guardar en base de datos"
                val user = auth.currentUser
                if (user != null && user.uid == uid) {
                    user.delete().addOnCompleteListener { onError(message) }
                } else {
                    onError(message)
                }
            }
    }
}