package com.android.wx2.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File

private enum class HayasakaScreen { HOME, PLAYER, BROWSER }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HayasakaApp(this) }
    }
}

@Composable
private fun HayasakaApp(context: Context) {
    var screen by remember { mutableStateOf(HayasakaScreen.HOME) }
    MaterialTheme(colorScheme = darkColorScheme()) {
        when (screen) {
            HayasakaScreen.HOME -> HomeScreen(
                onBrowser = { screen = HayasakaScreen.BROWSER },
                onPlayer = { screen = HayasakaScreen.PLAYER }
            )
            HayasakaScreen.PLAYER -> PlayerScreen(context, onHome = { screen = HayasakaScreen.HOME })
            HayasakaScreen.BROWSER -> BrowserScreen(onHome = { screen = HayasakaScreen.HOME })
        }
    }
}

@Composable
private fun HomeScreen(onBrowser: () -> Unit, onPlayer: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 22.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text("HAYASAKA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("What would you like\nto do?", style = MaterialTheme.typography.headlineLarge)
            Text("Your music and web, together.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            HomeChoiceCard(
                title = "Browser",
                subtitle = "Explore the web, search, and open websites",
                icon = "🌐",
                onClick = onBrowser
            )
            HomeChoiceCard(
                title = "Music Player",
                subtitle = "Play music and manage your offline library",
                icon = "🎵",
                onClick = onPlayer
            )
            Spacer(Modifier.weight(1f))
            Text("HAYASAKA • MUSIC & WEB", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HomeChoiceCard(title: String, subtitle: String, icon: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(icon, style = MaterialTheme.typography.headlineMedium)
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlayerScreen(context: Context, onHome: () -> Unit) {
    var url by remember { mutableStateOf("") }
    var files by remember { mutableStateOf(listAudio(context)) }
    var pendingDelete by remember { mutableStateOf<File?>(null) }
    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TextButton(onClick = onHome) { Text("← Home") }
            Text("Music Player", style = MaterialTheme.typography.headlineLarge)
            Text("Your offline music library", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                url, { url = it }, label = { Text("Direct audio URL") },
                placeholder = { Text("https://…/song.mp3") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Button(
                onClick = { enqueueDownload(context, url); Toast.makeText(context, "Download started. Refresh after it completes.", Toast.LENGTH_SHORT).show() },
                enabled = url.startsWith("https://"), modifier = Modifier.fillMaxWidth()
            ) { Text("Download track") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Downloaded tracks", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = { files = listAudio(context) }) { Text("Refresh") }
            }
            if (files.isEmpty()) Text("No tracks yet. Download a direct audio-file URL to get started.")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(files, key = { it.absolutePath }) { file ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(start = 14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).clickable { playFile(context, file) }.padding(vertical = 14.dp)) {
                                Text(file.nameWithoutExtension, style = MaterialTheme.typography.titleMedium)
                                Text("Tap to play", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { pendingDelete = file }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete downloaded track")
                            }
                        }
                    }
                }
            }
        }
    }
    pendingDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete download?") },
            text = { Text("Remove " + file.name + " from your offline library?") },
            confirmButton = {
                TextButton(onClick = { file.delete(); files = listAudio(context); pendingDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun BrowserScreen(onHome: () -> Unit) {
    var address by remember { mutableStateOf("https://www.google.com") }
    var currentUrl by remember { mutableStateOf("https://www.google.com") }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageTitle by remember { mutableStateOf("") }

    fun navigate(raw: String) {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return
        val target = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else if (trimmed.contains(".") && !trimmed.contains(" ")) {
            "https://$trimmed"
        } else {
            "https://www.google.com/search?q=" + Uri.encode(trimmed)
        }
        address = target
        currentUrl = target
        webView?.loadUrl(target)
    }

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                TextButton(onClick = onHome) { Text("← Home") }
                Text("Browser", style = MaterialTheme.typography.titleLarge)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedTextField(
                    value = address, onValueChange = { address = it },
                    modifier = Modifier.weight(1f), singleLine = true,
                    label = { Text("Search or enter address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                Button(onClick = { navigate(address) }) { Text("Go") }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { webView?.goBack() }, enabled = webView?.canGoBack() == true) { Text("‹") }
                TextButton(onClick = { webView?.goForward() }, enabled = webView?.canGoForward() == true) { Text("›") }
                TextButton(onClick = { webView?.reload() }) { Text("Reload") }
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) {
                                currentUrl = url
                                address = url
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onReceivedTitle(view: WebView, title: String?) { pageTitle = title.orEmpty() }
                        }
                        webView = this
                        loadUrl(currentUrl)
                    }
                },
                update = { webView = it }
            )
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
