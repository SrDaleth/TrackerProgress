package com.example.apptrackerprogress.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.apptrackerprogress.domain.WorkFilter
import com.example.apptrackerprogress.model.WorkItem
import com.example.apptrackerprogress.model.WorkStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    workList: List<WorkItem>,
    workshopName: String,
    isOwner: Boolean,
    lowStockCount: Int,
    maintenanceDueCount: Int,
    onLogoutClick: () -> Unit,
    onAddWorkClick: () -> Unit,
    onWorkItemClick: (String) -> Unit,
    onDeleteClick: (WorkItem, returnStock: Boolean) -> Unit,
    onProfileClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onVehiclesClick: () -> Unit,
    onStatsClick: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    // Solo se recalcula cuando cambian la lista o los filtros
    val filtered = remember(workList, query, statusFilter) { WorkFilter.apply(workList, query, statusFilter) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mis Trabajos")
                        Text(workshopName, style = MaterialTheme.typography.labelMedium)
                    }
                },
                actions = {
                    IconButton(onClick = onInventoryClick) { Icon(Icons.Default.Inventory, contentDescription = "Inventario") }
                    IconButton(onClick = onProfileClick) { Icon(Icons.Default.AccountCircle, contentDescription = "Mi Perfil") }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Más opciones") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            MenuEntry("Vehículos", Icons.Default.TwoWheeler) { menuOpen = false; onVehiclesClick() }
                            if (isOwner) MenuEntry("Estadísticas", Icons.Default.BarChart) { menuOpen = false; onStatsClick() }
                            MenuEntry("Cerrar sesión", Icons.AutoMirrored.Filled.ExitToApp) { menuOpen = false; onLogoutClick() }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddWorkClick, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Trabajo", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Buscador y filtros
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Buscar por cliente, placa o vehículo") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, contentDescription = "Limpiar") }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = statusFilter == null, onClick = { statusFilter = null }, label = { Text("Todos (${workList.size})") })
                    WorkStatus.all.forEach { s ->
                        FilterChip(
                            selected = statusFilter == s,
                            onClick = { statusFilter = if (statusFilter == s) null else s },
                            label = { Text("$s (${workList.count { it.status == s }})") }
                        )
                    }
                }
            }

            // Alertas
            if (maintenanceDueCount > 0) {
                item { AlertCard(Icons.Default.Notifications, "$maintenanceDueCount vehículos con mantenimiento próximo o vencido", onVehiclesClick) }
            }
            if (lowStockCount > 0) {
                item { AlertCard(Icons.Default.Warning, "$lowStockCount ítems del inventario con stock bajo", onInventoryClick) }
            }

            if (filtered.isEmpty()) {
                item {
                    Text(
                        if (workList.isEmpty()) "No tienes trabajos registrados.\n¡Crea uno con el botón '+'!" else "No hay trabajos que coincidan con la búsqueda.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                }
            }
            items(filtered, key = { it.id }) { work ->
                WorkItemCard(
                    work = work,
                    onClick = { onWorkItemClick(work.id) },
                    onDeleteClick = if (isOwner) ({ returnStock -> onDeleteClick(work, returnStock) }) else null // Solo el dueño elimina
                )
            }
        }
    }
}

@Composable
private fun MenuEntry(text: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(text) }, leadingIcon = { Icon(icon, contentDescription = null) }, onClick = onClick)
}

@Composable
private fun AlertCard(icon: ImageVector, text: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(8.dp))
            Text(text, color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Ver", color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
fun WorkItemCard(
    work: WorkItem,
    onClick: () -> Unit,
    onDeleteClick: ((returnStock: Boolean) -> Unit)? = null
) {
    var showDialog by remember { mutableStateOf(false) }
    // Si el trabajo no se terminó, lo normal es devolver los repuestos (se canceló)
    val finished = work.status == WorkStatus.READY || work.status == WorkStatus.DELIVERED
    var returnStock by remember(work.status) { mutableStateOf(!finished) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(work.clientName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    work.vehicleName + if (work.vehiclePlate.isNotBlank()) " · ${work.vehiclePlate}" else "",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(work.status)
                    if (work.estimatedCompletionDate != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Entrega: ${formatDay(work.estimatedCompletionDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (onDeleteClick != null) {
                IconButton(onClick = { showDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar trabajo", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDialog && onDeleteClick != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Eliminar trabajo") },
            text = {
                Column {
                    Text("¿Eliminar el trabajo de ${work.clientName} (${work.vehicleName})? Se borrarán sus avances, cobros e historial. Esta acción no se puede deshacer.")
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = returnStock, onCheckedChange = { returnStock = it })
                        Text("Devolver al inventario los repuestos usados", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (finished && returnStock) {
                        Text(
                            "Este trabajo ya estaba terminado: devuelve los repuestos solo si realmente no se instalaron.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false; onDeleteClick(returnStock) }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancelar") } }
        )
    }
}
