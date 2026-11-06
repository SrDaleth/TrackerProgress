package com.example.apptrackerprogress.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.apptrackerprogress.data.VehicleRepository
import com.example.apptrackerprogress.model.Vehicle
import com.example.apptrackerprogress.model.WorkForm
import com.google.firebase.Timestamp
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkScreen(
    vehicles: List<Vehicle>,
    onSaveClick: (WorkForm, onFailed: () -> Unit) -> Unit,
    onBackClick: () -> Unit
) {
    var appliedPlate by remember { mutableStateOf("") } // Placa ya autocompletada (para ocultar la sugerencia)
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var clientEmail by remember { mutableStateOf("") }
    var vehicleName by remember { mutableStateOf("") }
    var vehiclePlate by remember { mutableStateOf("") }
    var vehicleModel by remember { mutableStateOf("") }
    var mileage by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var estimatedCost by remember { mutableStateOf("") }
    var estimatedDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Trabajo") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle("Vehículo")
            FormField(vehiclePlate, { vehiclePlate = it.uppercase() }, "Placa (si ya vino, se autocompleta)", Icons.Default.Pin, capitalize = true)

            // Vehículos ya registrados que coinciden con la placa escrita
            val key = VehicleRepository.plateKey(vehiclePlate)
            val suggestions = if (key.length < 2) emptyList()
                else vehicles.filter { it.plateKey.startsWith(key) && it.plateKey != VehicleRepository.plateKey(appliedPlate) }.take(3)
            suggestions.forEach { v ->
                AssistChip(
                    onClick = {
                        vehiclePlate = v.plate
                        vehicleName = v.name
                        vehicleModel = v.model
                        if (v.lastMileage > 0) mileage = v.lastMileage.toString()
                        if (clientName.isBlank()) clientName = v.clientName
                        if (clientPhone.isBlank()) clientPhone = v.clientPhone
                        appliedPlate = v.plate
                    },
                    label = { Text("Usar ${v.plate} · ${v.name} (${v.clientName})") },
                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
                )
            }

            FormField(vehicleName, { vehicleName = it }, "Vehículo (Ej. TVS Apache 200) *", Icons.Default.TwoWheeler)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField(vehicleModel, { vehicleModel = it }, "Año / Modelo", Icons.Default.CalendarMonth, modifier = Modifier.weight(1f))
                FormField(mileage, { mileage = it.filter(Char::isDigit) }, "Kilometraje", Icons.Default.Speed, KeyboardType.Number, Modifier.weight(1f))
            }

            SectionTitle("Cliente")
            FormField(clientName, { clientName = it }, "Nombre del cliente *", Icons.Default.Person)
            FormField(clientPhone, { clientPhone = it.filter { c -> c.isDigit() || c == '+' } }, "Teléfono / WhatsApp", Icons.Default.Phone, KeyboardType.Phone)
            FormField(clientEmail, { clientEmail = it.trim() }, "Correo electrónico", Icons.Default.Email, KeyboardType.Email)

            SectionTitle("Trabajo")
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción inicial o problema") },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                maxLines = 4
            )
            FormField(estimatedCost, { estimatedCost = it.filter(Char::isDigit) }, "Presupuesto estimado ($)", Icons.Default.AttachMoney, KeyboardType.Number)

            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Event, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Entrega estimada: " +
                        (estimatedDateMillis?.let { formatDay(Timestamp(Date(it))) } ?: "sin definir")
                )
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    saving = true
                    onSaveClick(
                        WorkForm(
                            clientName = clientName,
                            clientPhone = clientPhone,
                            clientEmail = clientEmail,
                            vehicleName = vehicleName,
                            vehiclePlate = vehiclePlate,
                            vehicleModel = vehicleModel,
                            vehicleMileage = mileage.toIntOrNull() ?: 0,
                            description = description,
                            estimatedCost = estimatedCost.toDoubleOrNull() ?: 0.0,
                            estimatedCompletionDate = estimatedDateMillis?.let { Timestamp(Date(it)) }
                        )
                    ) { saving = false }
                },
                enabled = !saving, // Evita crear el trabajo dos veces con doble toque
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(if (saving) "Guardando..." else "Guardar y Generar Enlace")
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = estimatedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // El selector devuelve medianoche UTC; guardamos mediodía para evitar saltos de día por zona horaria
                    estimatedDateMillis = state.selectedDateMillis?.plus(12 * 60 * 60 * 1000L)
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun FormField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth(),
    capitalize: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = if (capitalize) KeyboardCapitalization.Characters else KeyboardCapitalization.Sentences
        ),
        singleLine = true,
        modifier = modifier
    )
}
