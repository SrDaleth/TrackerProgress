package com.example.apptrackerprogress.model

// Modelo de datos para cada trabajo/reparación
data class WorkItem(
    val id: String = "",
    val workerId: String = "",
    val clientName: String = "",
    val vehicleName: String = "", // Ej: TVS Apache RTR 200
    val description: String = "",
    val status: String = "Pendiente"
)