package com.example.apptrackerprogress.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class WorkViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun addWork(
        clientName: String,
        vehicleName: String,
        description: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onError("Error: Usuario no autenticado")
            return
        }

        if (clientName.isBlank() || vehicleName.isBlank()) {
            onError("El nombre del cliente y del vehículo son obligatorios")
            return
        }

        val newWorkRef = db.collection("works").document()

        val workData = hashMapOf(
            "id" to newWorkRef.id,
            "workerId" to currentUser.uid,
            "clientName" to clientName,
            "vehicleName" to vehicleName,
            "description" to description,
            "status" to "Pendiente"
        )

        newWorkRef.set(workData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception ->
                onError(exception.localizedMessage ?: "Error al guardar el trabajo")
            }
    }

    var workList = androidx.compose.runtime.mutableStateOf<List<com.example.apptrackerprogress.model.WorkItem>>(emptyList())
        private set

    init {
        fetchWorks()
    }

    fun fetchWorks() {
        val currentUser = auth.currentUser ?: return

        db.collection("works")
            .whereEqualTo("workerId", currentUser.uid)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(com.example.apptrackerprogress.model.WorkItem::class.java)
                    }
                    workList.value = list
                }
            }
    }

    fun deleteWork(
        workId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("works").document(workId)
            .delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.localizedMessage ?: "Error al eliminar el trabajo")
            }
    }

    // Subir foto a Cloudinary sin requerir cuenta bancaria ni Firebase Storage
    fun addProgressUpdate(
        workId: String,
        mediaUri: Uri?,
        comment: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (mediaUri == null) {
            onError("Por favor, selecciona una foto o video")
            return
        }

        val updateId = db.collection("works").document(workId).collection("updates").document().id

        // Subimos a Cloudinary con el preset 'unsigned' que creaste
        MediaManager.get().upload(mediaUri)
            .unsigned("tracker_progress") // Asegúrate de que este nombre sea exacto al de Cloudinary
            .option("folder", "work_media/$workId")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    // Cloudinary nos entrega la URL en secure_url
                    val downloadUrl = resultData["secure_url"] as? String ?: ""

                    val updateData = hashMapOf(
                        "id" to updateId,
                        "mediaUrl" to downloadUrl,
                        "mediaType" to "image",
                        "comment" to comment,
                        "timestamp" to System.currentTimeMillis()
                    )

                    // Guardamos la referencia en Firestore
                    db.collection("works").document(workId)
                        .collection("updates").document(updateId)
                        .set(updateData)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { e ->
                            onError(e.localizedMessage ?: "Error al guardar el avance")
                        }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    onError("Error en Cloudinary: ${error.description}")
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            }).dispatch()
    }

    fun getProgressUpdates(
        workId: String,
        onUpdatesLoaded: (List<com.example.apptrackerprogress.model.ProgressUpdate>) -> Unit
    ) {
        db.collection("works").document(workId).collection("updates")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(com.example.apptrackerprogress.model.ProgressUpdate::class.java)
                    }
                    onUpdatesLoaded(list)
                }
            }
    }

    fun generateClientLink(workId: String): String {
        return "https://tallertracker-client.web.app/trabajo/$workId"
    }
}