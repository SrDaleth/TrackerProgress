package com.example.apptrackerprogress.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.apptrackerprogress.model.*
import com.example.apptrackerprogress.data.VehicleRepository
import com.example.apptrackerprogress.util.ImageUtils
import com.example.apptrackerprogress.util.PdfGenerator
import com.example.apptrackerprogress.viewmodel.InventoryViewModel
import com.example.apptrackerprogress.viewmodel.WorkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkDetailScreen(
    workId: String,
    workViewModel: WorkViewModel,
    inventoryViewModel: InventoryViewModel,
    onBackClick: () -> Unit,
    onOpenVehicle: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val toast: (String) -> Unit = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }

    // Datos en tiempo real (un solo Flow combinado desde el repositorio)
    val detail by remember(workId) { workViewModel.detail(workId) }.collectAsState(initial = null)
    val session by workViewModel.session.collectAsState()
    val inventoryItems by inventoryViewModel.items.collectAsState()
    val work = detail?.work
    val private = detail?.private ?: WorkPrivate()
    val lineItems = detail?.items ?: emptyList()
    val history = detail?.history ?: emptyList()
    val updates = detail?.updates ?: emptyList()

    val sharePdf: (PdfGenerator.Kind) -> Unit = { kind ->
        detail?.let { d ->
            runCatching { PdfGenerator.share(context, PdfGenerator.create(context, kind, session?.workshopName ?: "Mi taller", d)) }
                .onFailure { toast("No se pudo generar el PDF") }
        }
    }

    // Formulario de avance
    var comment by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var usedItems by remember { mutableStateOf<List<UsedItem>>(emptyList()) }
    var publicVisible by remember { mutableStateOf(true) }
    var isUploading by remember { mutableStateOf(false) }

    // Diálogos
    var showItemPicker by remember { mutableStateOf(false) }
    var pickedItem by remember { mutableStateOf<InventoryItem?>(null) }
    var showAddLine by remember { mutableStateOf(false) }
    var updateToDelete by remember { mutableStateOf<ProgressUpdate?>(null) }

    val workLabel = work?.let { "${it.vehicleName} - ${it.clientName}" } ?: ""

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) selectedImageUri = uri
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) selectedImageUri = cameraUri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(work?.clientName ?: "Detalle del Trabajo") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
                actions = {
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Enlace de cliente", workViewModel.generateClientLink(workId)))
                        Toast.makeText(context, "¡Enlace copiado al portapapeles!", Toast.LENGTH_LONG).show()
                    }) { Icon(Icons.Default.Share, contentDescription = "Copiar enlace del cliente") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        val w = work
        if (w == null) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ---------- Vehículo y fechas ----------
            item {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(w.vehicleName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        StatusChip(w.status)
                    }
                    val specs = listOfNotNull(
                        w.vehiclePlate.ifBlank { null }?.let { "Placa $it" },
                        w.vehicleModel.ifBlank { null },
                        if (w.vehicleMileage > 0) "%,d km".format(w.vehicleMileage) else null
                    )
                    if (specs.isNotEmpty()) Text(specs.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                    if (w.description.isNotBlank()) Text("Problema / Notas: ${w.description}", style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider()
                    LabelValue("Ingreso", formatDateTime(w.createdAt))
                    LabelValue("Entrega estimada", formatDay(w.estimatedCompletionDate))
                    LabelValue("Finalizado", formatDateTime(w.completedAt))
                    if (w.vehiclePlate.isNotBlank()) {
                        TextButton(onClick = {
                            onOpenVehicle(VehicleRepository.id(w.workshopId.ifBlank { session?.workshopId ?: "" }, w.vehiclePlate))
                        }) { Icon(Icons.Default.History, null); Spacer(Modifier.width(6.dp)); Text("Ver hoja de vida del vehículo") }
                    }
                }
            }

            // ---------- Estado ----------
            item {
                SectionCard(title = "Estado del trabajo") {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WorkStatus.all.forEach { status ->
                            FilterChip(
                                selected = w.status == status,
                                onClick = { workViewModel.updateStatus(w, status, toast) },
                                label = { Text(status) }
                            )
                        }
                    }
                }
            }

            // ---------- Cliente (privado) ----------
            item {
                SectionCard(title = "Cliente") {
                    Text(w.clientName, fontWeight = FontWeight.Bold)
                    if (private.clientPhone.isNotBlank()) LabelValue("Teléfono", private.clientPhone)
                    if (private.clientEmail.isNotBlank()) LabelValue("Correo", private.clientEmail)
                    if (private.clientPhone.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Enviar el enlace de seguimiento por WhatsApp
                            Button(onClick = {
                                val digits = private.clientPhone.filter(Char::isDigit).let { if (it.length == 10) "57$it" else it }
                                val msg = "Hola ${w.clientName}, puedes ver el avance de tu ${w.vehicleName} aquí: ${workViewModel.generateClientLink(workId)}"
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits?text=${Uri.encode(msg)}")))
                                }.onFailure { toast("No se pudo abrir WhatsApp") }
                            }) { Icon(Icons.Default.Chat, null); Spacer(Modifier.width(6.dp)); Text("WhatsApp") }
                            OutlinedButton(onClick = {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${private.clientPhone}")))
                            }) { Icon(Icons.Default.Call, null); Spacer(Modifier.width(6.dp)); Text("Llamar") }
                        }
                    }
                }
            }

            // ---------- Cobro: ítems, totales y pago ----------
            item {
                SectionCard(title = "Cobro") {
                    lineItems.forEach { line ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(line.description, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${line.type} · ${line.quantity} x ${formatMoney(line.unitPrice)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(formatMoney(line.totalPrice), fontWeight = FontWeight.Bold)
                            // Los repuestos de un avance se quitan borrando el avance (así vuelven al inventario)
                            if (line.updateId.isBlank()) {
                                IconButton(onClick = { workViewModel.deleteLineItem(workId, line, toast) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Quitar ítem")
                                }
                            } else Spacer(Modifier.width(48.dp))
                        }
                    }
                    if (lineItems.isEmpty()) Text("Sin ítems cobrados todavía.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { showAddLine = true }) {
                        Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Agregar mano de obra / ítem")
                    }
                    HorizontalDivider()
                    LabelValue("Presupuesto", formatMoney(w.estimatedCost))
                    LabelValue("Total a cobrar", formatMoney(w.finalCost), bold = true)
                    if (w.estimatedCost > 0 && w.finalCost > w.estimatedCost) {
                        Text(
                            "Supera el presupuesto en ${formatMoney(w.finalCost - w.estimatedCost)}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (private.paid) "Pagado ✓" else "Pago pendiente", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Switch(checked = private.paid, onCheckedChange = { workViewModel.updatePayment(workId, it, private.paymentMethod, toast) })
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PAYMENT_METHODS.forEach { method ->
                            FilterChip(
                                selected = private.paymentMethod == method,
                                onClick = { workViewModel.updatePayment(workId, private.paid, method, toast) },
                                label = { Text(method) }
                            )
                        }
                    }
                    // Documentos para el cliente
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { sharePdf(PdfGenerator.Kind.QUOTE) }) {
                            Icon(Icons.Default.PictureAsPdf, null); Spacer(Modifier.width(6.dp)); Text("Cotización")
                        }
                        OutlinedButton(onClick = { sharePdf(PdfGenerator.Kind.RECEIPT) }) {
                            Icon(Icons.Default.Receipt, null); Spacer(Modifier.width(6.dp)); Text("Comprobante")
                        }
                    }
                }
            }

            // ---------- Subir nuevo avance ----------
            item {
                SectionCard(title = "Subir nuevo avance") {
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Descripción del avance (Ej. Cambio de cadena)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        OutlinedButton(onClick = {
                            // El archivo temporal se crea solo cuando se va a usar la cámara
                            val file = java.io.File.createTempFile("captured_img_", ".jpg", context.cacheDir)
                            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            cameraUri = uri
                            cameraLauncher.launch(uri)
                        }) { Text("📷 Tomar Foto") }
                        OutlinedButton(onClick = {
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) { Text("🖼️ Galería") }
                    }
                    if (selectedImageUri != null) {
                        Text("Foto lista para publicar ✓", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }

                    TextButton(onClick = { showItemPicker = true }) {
                        Icon(Icons.Default.Inventory, null); Spacer(Modifier.width(8.dp)); Text("Agregar repuesto del inventario")
                    }
                    usedItems.forEach { used ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("• ${used.name} x${used.quantity} ${used.unit}", modifier = Modifier.weight(1f))
                            IconButton(onClick = { usedItems = usedItems - used }) { Icon(Icons.Default.Close, contentDescription = "Quitar") }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Visible para el cliente")
                            Text(
                                if (publicVisible) "Aparecerá en el enlace del cliente" else "Nota interna: solo la ve el taller",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = publicVisible, onCheckedChange = { publicVisible = it })
                    }

                    Button(
                        onClick = {
                            isUploading = true
                            workViewModel.addProgressUpdate(
                                context, workId, workLabel, selectedImageUri, comment, usedItems, publicVisible,
                                onSuccess = {
                                    isUploading = false
                                    comment = ""
                                    selectedImageUri = null
                                    usedItems = emptyList()
                                    publicVisible = true
                                    Toast.makeText(context, "Avance subido con éxito", Toast.LENGTH_SHORT).show()
                                },
                                onError = { isUploading = false; toast(it) }
                            )
                        },
                        enabled = !isUploading,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (isUploading) "Subiendo..." else "Publicar Avance") }
                }
            }

            // ---------- Avances ----------
            item { Text("Historial de Progresos", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) }
            if (updates.isEmpty()) item { Text("Aún no hay avances.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(updates, key = { it.id }) { update ->
                UpdateCard(
                    update = update,
                    onToggleVisibility = { workViewModel.setUpdateVisibility(workId, update.id, !update.publicVisible, toast) },
                    onDelete = { updateToDelete = update }
                )
            }

            // ---------- Historial de estados ----------
            if (history.isNotEmpty()) {
                item {
                    SectionCard(title = "Historial de estados") {
                        history.forEach { h ->
                            Text(
                                (if (h.fromStatus.isBlank()) "Creado como ${h.toStatus}" else "${h.fromStatus} → ${h.toStatus}") +
                                    " · ${formatDateTime(h.timestamp)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }

    // ---------- Diálogos ----------

    if (showItemPicker) {
        // Descontamos lo ya agregado en este avance para no superar el stock
        val available = inventoryItems.map { item ->
            item to item.quantity - usedItems.filter { it.itemId == item.id }.sumOf { it.quantity }
        }
        AlertDialog(
            onDismissRequest = { showItemPicker = false },
            title = { Text("Selecciona un repuesto") },
            text = {
                if (available.isEmpty()) {
                    Text("No tienes ítems en el inventario. Agrégalos desde la pantalla de Inventario.")
                } else {
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(available, key = { it.first.id }) { (item, stock) ->
                            ListItem(
                                headlineContent = { Text(item.name) },
                                supportingContent = { Text("Disponible: $stock ${item.unit} · ${formatMoney(item.price)}") },
                                modifier = Modifier.clickable(enabled = stock > 0) {
                                    pickedItem = item.copy(quantity = stock)
                                    showItemPicker = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showItemPicker = false }) { Text("Cerrar") } }
        )
    }

    pickedItem?.let { item ->
        QuantityDialog(
            title = "Cantidad de ${item.name}",
            confirmText = "Agregar",
            maxQuantity = item.quantity,
            showNote = false,
            onDismiss = { pickedItem = null },
            onConfirm = { qty, _ ->
                usedItems = usedItems + UsedItem(item.id, item.name, item.unit, qty, item.price)
                pickedItem = null
            }
        )
    }

    if (showAddLine) {
        AddLineItemDialog(
            onDismiss = { showAddLine = false },
            onSave = { desc, type, qty, price ->
                workViewModel.addLineItem(workId, desc, type, qty, price, onSuccess = { showAddLine = false }, onError = toast)
            }
        )
    }

    updateToDelete?.let { update ->
        AlertDialog(
            onDismissRequest = { updateToDelete = null },
            title = { Text("Eliminar avance") },
            text = {
                Text(
                    if (update.usedItems.isEmpty()) "¿Eliminar este avance? Esta acción no se puede deshacer."
                    else "¿Eliminar este avance? Los repuestos usados se devolverán al inventario y se quitarán del cobro."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    workViewModel.deleteProgressUpdate(
                        workId, workLabel, update,
                        onSuccess = { Toast.makeText(context, "Avance eliminado", Toast.LENGTH_SHORT).show() },
                        onError = toast
                    )
                    updateToDelete = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { updateToDelete = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun SectionCard(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun LabelValue(label: String, value: String, bold: Boolean = false) {
    Row {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun UpdateCard(update: ProgressUpdate, onToggleVisibility: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(formatDate(update.timestamp), style = MaterialTheme.typography.labelMedium)
                    if (!update.publicVisible) {
                        Text("🔒 Nota interna", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        if (update.publicVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (update.publicVisible) "Ocultar al cliente" else "Mostrar al cliente"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar avance", tint = MaterialTheme.colorScheme.error)
                }
            }
            if (update.mediaUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageUtils.thumb(update.mediaUrl), // Versión liviana, no la foto original
                    contentDescription = "Evidencia del taller",
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(8.dp))
            }
            if (update.comment.isNotBlank()) Text(update.comment, style = MaterialTheme.typography.bodyMedium)
            if (update.usedItems.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Repuestos usados:", style = MaterialTheme.typography.labelLarge)
                update.usedItems.forEach { Text("• ${it.name} x${it.quantity} ${it.unit}", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun AddLineItemDialog(onDismiss: () -> Unit, onSave: (String, String, Int, Double) -> Unit) {
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(WorkLineItem.TYPE_LABOR) }
    var qty by remember { mutableStateOf("1") }
    var price by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar al cobro") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WorkLineItem.types.forEach { t -> FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) }) }
                }
                OutlinedTextField(description, { description = it }, label = { Text("Descripción (ej. Mano de obra)") }, singleLine = true)
                OutlinedTextField(qty, { qty = it.filter(Char::isDigit) }, label = { Text("Cantidad") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("Precio unitario ($)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(description, type, qty.toIntOrNull() ?: 0, price.toDoubleOrNull() ?: 0.0) }) { Text("Agregar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
