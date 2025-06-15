// MainActivity.kt
package com.example.filedownloader

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.filedownloader.ui.theme.FileDownloaderTheme
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// Data classes
data class FileItem(
    val index: Int,
    val filename: String
)

// API Interface
interface FileApiService {
    @GET("files")
    suspend fun getFiles(): Response<List<FileItem>>

    companion object {
        private const val BASE_URL = "http://192.168.168.93:8080/" // Cambia con l'IP del tuo server

        fun create(): FileApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(FileApiService::class.java)
        }

        fun getDownloadUrl(index: Int): String {
            return "${BASE_URL}download/$index"
        }
    }
}

// ViewModel
class FileDownloaderViewModel : ViewModel() {
    private val apiService = FileApiService.create()

    var uiState by mutableStateOf(FileDownloaderUiState())
        private set

    fun loadFiles() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            try {
                val response = apiService.getFiles()
                uiState = if (response.isSuccessful) {
                    uiState.copy(
                        files = response.body() ?: emptyList(),
                        isLoading = false
                    )
                } else {
                    uiState.copy(
                        error = "Errore nel caricamento: ${response.code()}",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                uiState = uiState.copy(
                    error = "Errore di rete: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
}

data class FileDownloaderUiState(
    val files: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// Main Activity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FileDownloaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FileDownloaderApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDownloaderApp(
    viewModel: FileDownloaderViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState

    // Launcher per richiedere permessi
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permesso concesso", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permesso negato", Toast.LENGTH_SHORT).show()
        }
    }

    // Carica i file al primo avvio
    LaunchedEffect(Unit) {
        viewModel.loadFiles()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("File Downloader") },
            actions = {
                IconButton(
                    onClick = { viewModel.loadFiles() }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Aggiorna")
                }
            }
        )

        // Contenuto principale
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator()
                }

                uiState.error != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(
                            onClick = { viewModel.loadFiles() }
                        ) {
                            Text("Riprova")
                        }
                    }
                }

                uiState.files.isEmpty() -> {
                    Text("Nessun file trovato")
                }

                else -> {
                    FileList(
                        files = uiState.files,
                        onDownloadClick = { fileItem ->
                            downloadFile(context, fileItem, permissionLauncher)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FileList(
    files: List<FileItem>,
    onDownloadClick: (FileItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(files) { file ->
            FileListItem(
                file = file,
                onDownloadClick = { onDownloadClick(file) }
            )
        }
    }
}

@Composable
fun FileListItem(
    file: FileItem,
    onDownloadClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.filename,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Indice: ${file.index}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onDownloadClick
            ) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Scarica ${file.filename}",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Funzione per gestire il download
private fun downloadFile(
    context: Context,
    fileItem: FileItem,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    // Controlla permessi
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return
    }

    try {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUrl = FileApiService.getDownloadUrl(fileItem.index)

        val request = DownloadManager.Request(downloadUrl.toUri())
            .setTitle("Download: ${fileItem.filename}")
            .setDescription("Scaricando ${fileItem.filename}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileItem.filename)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        downloadManager.enqueue(request)

        Toast.makeText(
            context,
            "Download avviato: ${fileItem.filename}",
            Toast.LENGTH_SHORT
        ).show()

    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Errore nel download: ${e.message}",
            Toast.LENGTH_LONG
        ).show()
    }
}
