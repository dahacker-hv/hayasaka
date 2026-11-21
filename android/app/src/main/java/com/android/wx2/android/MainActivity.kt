package com.android.wx2.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HayasakaApp(this) }
    }
}

@Composable
private fun HayasakaApp(context: Context) {
    var url by remember { mutableStateOf("") }
    var files by remember { mutableStateOf(listAudio(context)) }
    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Hayasaka", style = MaterialTheme.typography.headlineLarge)
                Text("Your offline music library", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(url, { url = it }, label = { Text("Direct audio URL") }, placeholder = { Text("https://…/song.mp3") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(onClick = { enqueueDownload(context, url); Toast.makeText(context, "Download started. Refresh after it completes.", Toast.LENGTH_SHORT).show() }, enabled = url.startsWith("https://"), modifier = Modifier.fillMaxWidth()) { Text("Download track") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Downloaded tracks", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { files = listAudio(context) }) { Text("Refresh") }
                }
                if (files.isEmpty()) Text("No tracks yet. Download a direct audio-file URL to get started.")
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(files, key = { it.absolutePath }) { file ->
                        Card(Modifier.fillMaxWidth().clickable { playFile(context, file) }) {
                            Column(Modifier.padding(14.dp)) {
                                Text(file.nameWithoutExtension, style = MaterialTheme.typography.titleMedium)
                                Text("Tap to play", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun musicDir(context: Context): File = File(context.getExternalFilesDir(null), "HayasakaMusic").apply { mkdirs() }
private fun listAudio(context: Context): List<File> = musicDir(context).listFiles()?.filter { it.isFile && it.extension.lowercase() in setOf("mp3","m4a","aac","ogg","wav","flac") }?.sortedBy { it.name.lowercase() } ?: emptyList()

private fun enqueueDownload(context: Context, raw: String) {
    try {
        musicDir(context)
        val uri = Uri.parse(raw.trim())
        val name = uri.lastPathSegment?.takeIf { it.isNotBlank() } ?: "track.mp3"
        val request = android.app.DownloadManager.Request(uri)
            .setTitle(name).setDescription("Downloading into Hayasaka")
            .setAllowedOverMetered(true).setAllowedOverRoaming(false)
            .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, null, "HayasakaMusic/$name")
        (context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager).enqueue(request)
    } catch (_: Exception) { Toast.makeText(context, "Invalid URL", Toast.LENGTH_LONG).show() }
}
private fun playFile(context: Context, file: File) {
    val i = Intent(context, PlaybackService::class.java).setAction(PlaybackService.ACTION_PLAY).putExtra(PlaybackService.EXTRA_PATH, file.absolutePath)
    ContextCompat.startForegroundService(context, i)
}
