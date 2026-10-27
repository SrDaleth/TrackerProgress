package com.example.apptrackerprogress.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.apptrackerprogress.model.WorkItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    workList: List<WorkItem>, // Recibimos la lista real desde el ViewModel
    onLogoutClick: () -> Unit,
    onAddWorkClick: () -> Unit,
    onWorkItemClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Trabajos") },
                actions = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión")
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
            FloatingActionButton(
                onClick = onAddWorkClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Trabajo", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { paddingValues ->
        if (workList.isEmpty()) {
            // Mensaje si no hay trabajos creados todavía
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tienes trabajos registrados.\n¡Crea uno con el botón '+'!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(workList) { work ->
                    WorkItemCard(
                        work = work,
                        onClick = { onWorkItemClick(work.id) },
                        onDeleteClick = { onDeleteClick(work.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun WorkItemCard(
    work: WorkItem,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "Icono de Taller",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = work.clientName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(text = work.vehicleName, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Estado: ${work.status}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (onDeleteClick != null) {
                // Botón de eliminar con ícono de papelera
                IconButton(onClick = { showDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar trabajo",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // Alerta de confirmación para evitar borrar por error
    if (showDialog && onDeleteClick != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Eliminar trabajo") },
            text = { Text("¿Estás seguro de que deseas eliminar el registro de ${work.clientName}? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        onDeleteClick()
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
