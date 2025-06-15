package com.example.ocrapp

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class MainActivity : ComponentActivity() {
    private val viewModel: OCRViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Carica lo stato salvato all'avvio
        viewModel.loadSavedState(this)
        
        setContent {
            MaterialTheme {
                OCRApp(viewModel = viewModel)
            }
        }
    }
}

data class OCRState(
    val selectedImage: Bitmap? = null,
    val extractedText: String = "",
    val phoneNumbers: List<String> = emptyList(),
    val isProcessing: Boolean = false,
    val error: String? = null
)

class OCRViewModel : ViewModel() {
    private val _state = MutableStateFlow(OCRState())
    val state: StateFlow<OCRState> = _state.asStateFlow()

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    companion object {
        private const val PREFS_NAME = "ocr_app_prefs"
        private const val KEY_LAST_IMAGE = "last_image"
        private const val KEY_LAST_TEXT = "last_text"
        private const val KEY_LAST_PHONE_NUMBERS = "last_phone_numbers"
    }

    fun loadSavedState(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        val savedImageString = prefs.getString(KEY_LAST_IMAGE, null)
        val savedText = prefs.getString(KEY_LAST_TEXT, "")
        val savedPhoneNumbers = prefs.getStringSet(KEY_LAST_PHONE_NUMBERS, emptySet())
        
        var savedBitmap: Bitmap? = null
        
        // Decodifica l'immagine salvata se presente
        savedImageString?.let { imageString ->
            try {
                val imageBytes = Base64.decode(imageString, Base64.DEFAULT)
                savedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            } catch (e: Exception) {
                // Ignora errori di decodifica
            }
        }
        
        // Aggiorna lo stato solo se ci sono dati salvati
        if (savedBitmap != null || !savedText.isNullOrEmpty() || !savedPhoneNumbers.isNullOrEmpty()) {
            _state.value = _state.value.copy(
                selectedImage = savedBitmap,
                extractedText = savedText ?: "",
                phoneNumbers = savedPhoneNumbers?.toList()?.sorted() ?: emptyList()
            )
        }
    }
    
    private fun saveState(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        val currentState = _state.value
        
        // Salva l'immagine come stringa Base64
        currentState.selectedImage?.let { bitmap ->
            try {
                val outputStream = ByteArrayOutputStream()
                // Comprimi l'immagine per ridurre le dimensioni
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val imageBytes = outputStream.toByteArray()
                val imageString = Base64.encodeToString(imageBytes, Base64.DEFAULT)
                editor.putString(KEY_LAST_IMAGE, imageString)
            } catch (e: Exception) {
                // Se c'è un errore nella compressione, rimuovi l'immagine salvata
                editor.remove(KEY_LAST_IMAGE)
            }
        }
        
        // Salva il testo estratto
        editor.putString(KEY_LAST_TEXT, currentState.extractedText)
        
        // Salva i numeri di telefono
        editor.putStringSet(KEY_LAST_PHONE_NUMBERS, currentState.phoneNumbers.toSet())
        
        editor.apply()
    }

    fun processImage(bitmap: Bitmap, context: Context) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    selectedImage = bitmap,
                    isProcessing = true,
                    error = null,
                    extractedText = "",
                    phoneNumbers = emptyList()
                )

                // Estrai il testo dall'immagine
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val result = textRecognizer.process(inputImage).await()
                val extractedText = result.text

                // Estrai i numeri di telefono con logica semplificata
                val phoneNumbers = extractPhoneNumbers(extractedText)

                _state.value = _state.value.copy(
                    extractedText = extractedText,
                    phoneNumbers = phoneNumbers,
                    isProcessing = false
                )
                
                // Salva lo stato dopo l'elaborazione
                saveState(context)
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isProcessing = false,
                    error = "Errore durante l'elaborazione: ${e.message}"
                )
            }
        }
    }

    private fun extractPhoneNumbers(text: String): List<String> {
        val phoneNumbers = mutableSetOf<String>()

        // Pattern più permissivo, ma limitiamo dopo con la logica
        val pattern = Regex("""\+39[^\n]+""")

        pattern.findAll(text).forEach { match ->
            val rawNumber = match.value

            // Interrompiamo l'estrazione alle prime newline nel testo dopo +39
            val numberUntilNewline = rawNumber.takeWhile { it != '\n' }

            // Estrai solo le cifre dopo "+39"
            val digitsAfter39 = numberUntilNewline.substring(3).takeWhile { it.isDigit() }

            // Se ci sono cifre dopo +39, crea il numero completo
            if (digitsAfter39.isNotEmpty()) {
                val cleanNumber = "+39$digitsAfter39"
                phoneNumbers.add(cleanNumber)
            }
        }

        return phoneNumbers.toList().sorted()
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
    
    fun clearAllData(context: Context) {
        _state.value = OCRState()
        
        // Rimuovi anche i dati salvati
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OCRApp(viewModel: OCRViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    // Launcher per selezionare immagini
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    viewModel.processImage(bitmap, context)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Errore nel caricamento dell'immagine", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launcher per i permessi
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            imagePickerLauncher.launch("image/*")
        } else {
            Toast.makeText(context, "Permesso necessario per accedere alle immagini", Toast.LENGTH_SHORT).show()
        }
    }

    // Funzione per copiare il testo
    fun copyTextToClipboard(text: String) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Testo OCR", text)
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(context, "Testo copiato negli appunti!", Toast.LENGTH_SHORT).show()
    }

    // Funzione per copiare i numeri di telefono
    fun copyPhoneNumbersToClipboard(phoneNumbers: List<String>) {
        if (phoneNumbers.isEmpty()) {
            Toast.makeText(context, "Nessun numero di telefono rilevato", Toast.LENGTH_SHORT).show()
            return
        }
        
        val phoneText = phoneNumbers.joinToString("\n")
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Numeri di telefono", phoneText)
        clipboardManager.setPrimaryClip(clipData)
        
        val message = if (phoneNumbers.size == 1) {
            "Numero di telefono copiato!"
        } else {
            "${phoneNumbers.size} numeri di telefono copiati!"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // Funzione per richiedere l'immagine
    fun requestImage() {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                imagePickerLauncher.launch("image/*")
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "OCR Scanner",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Pulsante per cancellare tutti i dati
                    if (state.selectedImage != null || state.extractedText.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearAllData(context) }
                        ) {
                            Text("Cancella")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pulsante per selezionare immagine
            Button(
                onClick = { requestImage() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.selectedImage != null) "Seleziona Nuova Immagine" else "Seleziona Immagine",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Immagine selezionata
            state.selectedImage?.let { bitmap ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Immagine selezionata",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Indicatore di caricamento
            if (state.isProcessing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Elaborazione in corso...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Numeri di telefono rilevati (mostra prima se ci sono)
            if (state.phoneNumbers.isNotEmpty() && !state.isProcessing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Numeri di telefono rilevati",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )

                            IconButton(
                                onClick = { copyPhoneNumbersToClipboard(state.phoneNumbers) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Copia numeri",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        state.phoneNumbers.forEach { phoneNumber ->
                            SelectionContainer {
                                Text(
                                    text = phoneNumber,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { copyPhoneNumbersToClipboard(state.phoneNumbers) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (state.phoneNumbers.size == 1) "Copia Numero" 
                                else "Copia ${state.phoneNumbers.size} Numeri"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Testo estratto
            if (state.extractedText.isNotEmpty() && !state.isProcessing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Testo Estratto",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            IconButton(
                                onClick = { copyTextToClipboard(state.extractedText) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copia testo",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        SelectionContainer {
                            Text(
                                text = state.extractedText,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { copyTextToClipboard(state.extractedText) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copia Tutto il Testo")
                        }
                    }
                }
            }

            // Messaggio se non c'è testo
            if (state.selectedImage != null && state.extractedText.isEmpty() && !state.isProcessing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Nessun testo rilevato nell'immagine",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Errori
            state.error?.let { error ->
                LaunchedEffect(error) {
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }
}