package com.example.apptrackerprogress.model

data class ProgressUpdate(
    val id: String = "",
    val authorId: String = "", // Quién subió el avance
    val mediaUrl: String = "", // URL de la foto en Cloudinary
    val mediaType: String = "image", // "image" o "video"
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val publicVisible: Boolean = true, // false = nota interna, el cliente no la ve
    val usedItems: List<UsedItem> = emptyList() // Repuestos del inventario gastados en este avance
)
