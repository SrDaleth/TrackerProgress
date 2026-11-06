package com.example.apptrackerprogress.model

import com.google.firebase.Timestamp

// Estados posibles de un trabajo (en orden)
object WorkStatus {
    const val PENDING = "Pendiente"
    const val IN_PROGRESS = "En Proceso"
    const val READY = "Listo"
    const val DELIVERED = "Entregado"
    val all = listOf(PENDING, IN_PROGRESS, READY, DELIVERED)
}

val PAYMENT_METHODS = listOf("Efectivo", "Transferencia", "Nequi", "Daviplata", "Tarjeta")

// works/{id} — Documento principal. Lo puede leer el cliente con el enlace,
// por eso NO contiene datos de contacto ni de pago.
data class WorkItem(
    val id: String = "",
    val workshopId: String = "", // Taller (todos sus miembros ven el trabajo)
    val workerId: String = "", // Quién lo creó
    // Cliente y vehículo
    val clientName: String = "",
    val vehicleName: String = "", // Ej: TVS Apache RTR 200
    val vehiclePlate: String = "",
    val vehicleModel: String = "", // Año / modelo exacto
    val vehicleMileage: Int = 0, // Kilometraje al ingresar
    val description: String = "",
    val status: String = WorkStatus.PENDING,
    // Financiero (visible para el cliente: presupuesto y total cobrado)
    val estimatedCost: Double = 0.0,
    val finalCost: Double = 0.0, // Suma automática de la subcolección items
    // Trazabilidad
    val createdAt: Timestamp? = null,
    val estimatedCompletionDate: Timestamp? = null,
    val completedAt: Timestamp? = null
)

// works/{id}/private/details — Solo lo lee el taller (contacto y pago)
data class WorkPrivate(
    val clientPhone: String = "",
    val clientEmail: String = "",
    val paid: Boolean = false, // Se llama "paid" y no "isPaid": Firestore mapea mal los "is..." en Kotlin
    val paymentMethod: String = ""
)

// works/{id}/items — Desglose de lo cobrado (repuestos, mano de obra, otros)
data class WorkLineItem(
    val id: String = "",
    val description: String = "",
    val type: String = TYPE_PART,
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0,
    val inventoryItemId: String = "", // Si salió del inventario
    val updateId: String = "", // Si se registró desde un avance (se borra junto con él)
    val createdAt: Timestamp? = null
) {
    companion object {
        const val TYPE_PART = "Repuesto"
        const val TYPE_LABOR = "Mano de obra"
        const val TYPE_OTHER = "Otro"
        val types = listOf(TYPE_PART, TYPE_LABOR, TYPE_OTHER)
    }
}

// works/{id}/history — Auditoría de cambios de estado
data class StatusChange(
    val id: String = "",
    val fromStatus: String = "",
    val toStatus: String = "",
    val changedBy: String = "",
    val timestamp: Timestamp? = null
)

// Todo lo que muestra la pantalla de detalle, combinado en un solo objeto
data class WorkDetail(
    val work: WorkItem,
    val private: WorkPrivate,
    val items: List<WorkLineItem>,
    val history: List<StatusChange>,
    val updates: List<ProgressUpdate>
)

// Datos del formulario de creación
data class WorkForm(
    val clientName: String,
    val clientPhone: String,
    val clientEmail: String,
    val vehicleName: String,
    val vehiclePlate: String,
    val vehicleModel: String,
    val vehicleMileage: Int,
    val description: String,
    val estimatedCost: Double,
    val estimatedCompletionDate: Timestamp?
)
