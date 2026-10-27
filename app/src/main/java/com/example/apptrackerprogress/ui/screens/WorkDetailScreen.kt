package com.example.apptrackerprogress.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.apptrackerprogress.model.ProgressUpdate
import com.example.apptrackerprogress.model.WorkItem
import com.example.apptrackerprogress.viewmodel.WorkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkDetailScreen(
    workId: String,
    workViewModel: WorkViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var workItem by remember { mutableStateOf<WorkItem?>(null) }
    var updatesList by remember { mutableStateOf<List<ProgressUpdate>>(emptyList()) }

    var comment by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    // 1. Lanzador para la galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) selectedImageUri = uri }
    )

    // 2. Preparar la URI temporal para la cámara
    val photoFile = remember {
        java.io.File.createTempFile("captured_img_", ".jpg", context.cacheDir)
    }
    val photoUri = remember {
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }

    // 3. Lanzador para la cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                selectedImageUri = photoUri
            }
        }
    )

    // Selector de fotos para el avance
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    // Cargar detalles del trabajo y sus actualizaciones al iniciar
    LaunchedEffect(workId) {
        // Obtenemos los datos del trabajo actual
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("works").document(workId)
            .get()
            .addOnSuccessListener { doc ->
                workItem = doc.toObject(WorkItem::class.java)
            }

        // Escuchamos los avances en tiempo real
        workViewModel.getProgressUpdates(workId) { list ->
            updatesList = list
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workItem?.clientName ?: "Detalle del Trabajo") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Botón para copiar el enlace del cliente
                    IconButton(onClick = {
                        val link = workViewModel.generateClientLink(workId)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Enlace de cliente", link)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "¡Enlace copiado al portapapeles!", Toast.LENGTH_LONG).show()
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Copiar enlace del cliente")
                    }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tarjeta con la información principal
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Vehículo: ${workItem?.vehicleName ?: ""}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Problema / Notas: ${workItem?.description ?: ""}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Estado actual: ${workItem?.status}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Sección para subir nuevo avance
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Subir nuevo avance", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = comment,
                            onValueChange = { comment = it },
                            label = { Text("Descripción del avance (Ej. Cambio de cadena)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Botones para Cámara o Galería
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            OutlinedButton(onClick = {
                                cameraLauncher.launch(photoUri)
                            }) {
                                Text("📷 Tomar Foto")
                            }

                            OutlinedButton(onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }) {
                                Text("🖼️ Galería")
                            }
                        }

                        if (selectedImageUri != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Foto lista para publicar ✓",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                            // Botón de guardar
                            Button(
                                onClick = {
                                    if (selectedImageUri != null) {
                                        isUploading = true
                                        workViewModel.addProgressUpdate(
                                            workId = workId,
                                            mediaUri = selectedImageUri,
                                            comment = comment,
                                            onSuccess = {
                                                isUploading = false
                                                comment = ""
                                                selectedImageUri = null
                                                Toast.makeText(context, "Avance subido con éxito", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { error ->
                                                isUploading = false
                                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    } else {
                                        Toast.makeText(context, "Selecciona una foto primero", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isUploading
                            ) {
                                Text(if (isUploading) "Subiendo..." else "Publicar Avance")
                            }
                        }
                    }
                }

            // Título de la línea de tiempo de avances
            item {
                Text(text = "Historial de Progresos", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            }

            // Lista de avances subidos
            items(updatesList) { update ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (update.mediaUrl.isNotBlank()) {
                            AsyncImage(
                                model = update.mediaUrl,
                                contentDescription = "Evidencia del taller",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text(text = update.comment, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}