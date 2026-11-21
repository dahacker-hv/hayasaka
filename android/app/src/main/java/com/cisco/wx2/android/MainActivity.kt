package com.cisco.wx2.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.concurrent.Executors

data class SaavnSong(val id:String,val name:String,val artist:String,val album:String,val duration:String,val stream:String)

class HayasakaActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{MaterialTheme(colorScheme=darkColorScheme()){MusicScreen(this)}}}
}
@Composable private fun MusicScreen(context:Context){
 var query by remember{mutableStateOf("")};var results by remember{mutableStateOf<List<SaavnSong>>(emptyList())};var busy by remember{mutableStateOf(false)}
 var status by remember{mutableStateOf("Search songs on Saavn")};var now by remember{mutableStateOf("")}
 Scaffold { p -> Column(Modifier.fillMaxSize().padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
 Text("Brilliant Pala Classes",style=MaterialTheme.typography.headlineSmall);Text("Music search • Saavn")
 Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
 OutlinedTextField(query,{query=it},label={Text("Song / artist")},singleLine=true,modifier=Modifier.weight(1f))
 Button(enabled=!busy&&query.isNotBlank(),onClick={busy=true;status="Searching…";Saavn.search(query.trim()){list,error->busy=false;results=list;status=error?: "${list.size} results"}}){Text(if(busy)"…" else "Search")}
 }
 Text(status,style=MaterialTheme.typography.bodySmall);if(now.isNotBlank())Text("Playing: $now",style=MaterialTheme.typography.titleSmall)
 LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)){
 items(results,key={it.id}){song->Card(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
 Column(Modifier.weight(1f)){Text(song.name,style=MaterialTheme.typography.titleSmall);Text(song.artist,style=MaterialTheme.typography.bodySmall);if(song.album.isNotBlank())Text(song.album,style=MaterialTheme.typography.labelSmall)}
 Button(onClick={if(song.stream.isBlank())Toast.makeText(context,"No stream link for this result",Toast.LENGTH_SHORT).show() else {startStream(context,song.stream);now=song.name;status="Streaming via Saavn"}}){Text("Play")}
 }}}
 }
 }}
}
private object Saavn{
 private val executor=Executors.newSingleThreadExecutor();private val main=Handler(Looper.getMainLooper())
 fun search(q:String,done:(List<SaavnSong>,String?)->Unit){executor.execute{try{
 val url="https://saavn.dev/api/search/songs?query="+URLEncoder.encode(q,"UTF-8")+"&limit=30"
 val c=URL(url).openConnection() as HttpURLConnection;c.connectTimeout=12000;c.readTimeout=12000
 if(c.responseCode !in 200..299)throw Exception("Saavn API HTTP ${c.responseCode}")
 val json=JSONObject(c.inputStream.bufferedReader().use{it.readText()});val arr=json.optJSONObject("data")?.optJSONArray("results");val out=mutableListOf<SaavnSong>()
 if(arr!=null)for(i in 0 until arr.length()){val x=arr.optJSONObject(i)?:continue
 val primary=x.optJSONObject("artists")?.optJSONArray("primary");val names=mutableListOf<String>();if(primary!=null)for(j in 0 until primary.length())primary.optJSONObject(j)?.optString("name")?.takeIf{it.isNotBlank()}?.let(names::add)
 val streams=x.optJSONArray("downloadUrl");var stream="";if(streams!=null)for(j in streams.length()-1 downTo 0){val u=streams.optJSONObject(j)?.optString("url")?:"";if(u.startsWith("http")){stream=u;break}}
 val dur=x.optInt("duration");val duration=if(dur>0)String.format("%d:%02d",dur/60,dur%60)else ""
 out.add(SaavnSong(x.optString("id"),x.optString("name","Unknown track"),names.joinToString(", "),x.optJSONObject("album")?.optString("name").orEmpty(),duration,stream))
 }
 main.post{done(out,null)}
 }catch(e:Exception){main.post{done(emptyList(),e.message?: "Search failed")}}}}
}
private fun startStream(context:Context,url:String){val i=Intent(context,PlaybackService::class.java).setAction(PlaybackService.ACTION_PLAY).putExtra(PlaybackService.EXTRA_PATH,url);ContextCompat.startForegroundService(context,i)}
