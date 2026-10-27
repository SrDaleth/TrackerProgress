package com.example.apptrackerprogress.model

data class ProgressUpdate(
    val id: String = "",
    val mediaUrl: String = "", // URL de la foto o video en Storage
    val mediaType: String = "image", // "image" o "video"
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)