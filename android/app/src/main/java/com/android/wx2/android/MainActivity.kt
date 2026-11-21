package com.android.wx2.android

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HayasakaScreen(this) }
    }
}

@Composable
private fun HayasakaScreen(context: Context) {
    var url by remember { mutableStateOf("") }
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Hayasaka", style = MaterialTheme.typography.headlineLarge)
                Text("Music • Downloads", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Audio file URL") },
                    placeholder = { Text("https://example.com/song.mp3") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = { downloadAudio(context, url) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = url.startsWith("https://") || url.startsWith("http://")
                ) { Text("Download for offline listening") }
                Text("Downloads are saved to your device's Music folder.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun downloadAudio(context: Context, url: String) {
    try {
        val request = DownloadManager.Request(Uri.parse(url.trim()))
            .setTitle("Hayasaka music download")
            .setDescription("Saving audio for offline listening")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "Hayasaka")
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(context, "Could not start download. Check the URL.", Toast.LENGTH_LONG).show()
    }
}