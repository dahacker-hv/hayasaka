package com.cisco.wx2.android

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.concurrent.Executors

data class SaavnSong(
    val id:String, val name:String, val artist:String, val album:String, val duration:String,
    val stream:String, val image:String, val download:String
)
class HayasakaActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{MaterialTheme(colorScheme=darkColorScheme()){MusicScreen(this)}}}
}
private fun musicFiles(c:Context)=File(c.filesDir,"offline_music").apply{mkdirs()}
private fun safeName(s:String)=s.replace(Regex("[^a-zA-Z0-9._ -]"),"_").trim().take(90).ifBlank{"track"}
private fun localFile(c:Context,s:SaavnSong)=File(musicFiles(c),"${safeName(s.name)}_${s.id}.m4a")
private fun songJson(s:SaavnSong)=JSONObject().put("id",s.id).put("name",s.name).put("artist",s.artist).put("album",s.album).put("duration",s.duration).put("stream",s.stream).put("image",s.image).put("download",s.download)
private fun parseSong(o:JSONObject)=SaavnSong(o.optString("id"),o.optString("name"),o.optString("artist"),o.optString("album"),o.optString("duration"),o.optString("stream"),o.optString("image"),o.optString("download"))
private fun listDownloaded(c:Context):List<SaavnSong> = musicFiles(c).listFiles()?.filter{it.name.endsWith(".json")}?.mapNotNull{try{parseSong(JSONObject(it.readText()))}catch(_:Exception){null}}?.sortedBy{it.name}?:emptyList()
@Composable private fun MusicScreen(context:Context){
 var query by remember{mutableStateOf("")};var results by remember{mutableStateOf<List<SaavnSong>>(emptyList())};var busy by remember{mutableStateOf(false)}
 var status by remember{mutableStateOf("Search songs on Saavn")};var now by remember{mutableStateOf("")};var library by remember{mutableStateOf(listDownloaded(context))};var showLibrary by remember{mutableStateOf(false)};var downloading by remember{mutableStateOf<String?>(null)}
 Scaffold { p -> Column(Modifier.fillMaxSize().padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
 Text("Brilliant Pala Classes",style=MaterialTheme.typography.headlineSmall);Text("Music • Saavn")
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={showLibrary=false}){Text("Search")};Button(onClick={library=listDownloaded(context);showLibrary=true}){Text("Offline library (${library.size})")}}
 if(!showLibrary){
 Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
 OutlinedTextField(query,{query=it},label={Text("Song / artist")},singleLine=true,modifier=Modifier.weight(1f))
 Button(enabled=!busy&&query.isNotBlank(),onClick={busy=true;status="Searching…";Saavn.search(query.trim()){list,error->busy=false;results=list;status=error?: "${list.size} results"}}){Text(if(busy)"…" else "Search")}
 }
 Text(status,style=MaterialTheme.typography.bodySmall);if(now.isNotBlank())Text("Playing: $now",style=MaterialTheme.typography.titleSmall)
 LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)){
 items(results,key={it.id}){song->SongCard(context,song,localFile(context,song).exists(),downloading==song.id,{downloading=song.id;status="Downloading…";Offline.download(context,song){ok,msg->downloading=null;library=listDownloaded(context);status=msg}}, {if(song.stream.isBlank())Toast.makeText(context,"No stream available",Toast.LENGTH_SHORT).show() else {startStream(context,song.stream,song.name,song.artist,song.album,song.image);now=song.name;status="Streaming via Saavn"}})}
 }
 }else{
 Text("Stored on this device • Available offline",style=MaterialTheme.typography.bodySmall)
 LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){items(library,key={it.id}){song->SongCard(context,song,true,false,{},{startStream(context,localFile(context,song).absolutePath,song.name,song.artist,song.album,song.image);now=song.name;status="Playing offline"});TextButton(onClick={localFile(context,song).delete();File(musicFiles(context),"${safeName(song.name)}_${song.id}.json").delete();File(musicFiles(context),"${safeName(song.name)}_${song.id}.jpg").delete();library=listDownloaded(context)}){Text("Remove download")}}}
 }}
}
@Composable private fun SongCard(c:Context,s:SaavnSong,isSaved:Boolean,isDownloading:Boolean,onDownload:()->Unit,onPlay:()->Unit){
 Card(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
 Column(Modifier.weight(1f)){Text(s.name,style=MaterialTheme.typography.titleSmall);Text(s.artist,style=MaterialTheme.typography.bodySmall);if(s.album.isNotBlank())Text(s.album,style=MaterialTheme.typography.labelSmall);Text(s.duration,style=MaterialTheme.typography.labelSmall)}
 Button(onClick=onPlay){Text("Play")}
 if(!isSaved&&!isDownloading)TextButton(onClick=onDownload){Text("↓ Save")}
 if(isSaved)Text("✓ Saved",modifier=Modifier.padding(8.dp))
 if(isDownloading)CircularProgressIndicator(Modifier.size(24.dp).padding(3.dp))
 }}}
private object Saavn{
 private val executor=Executors.newSingleThreadExecutor();private val main=Handler(Looper.getMainLooper())
 fun search(q:String,done:(List<SaavnSong>,String?)->Unit){executor.execute{try{
 val u="https://saavn.dev/api/search/songs?query="+URLEncoder.encode(q,"UTF-8")+"&limit=30"
 val c=URL(u).openConnection() as HttpURLConnection;c.connectTimeout=15000;c.readTimeout=15000
 if(c.responseCode !in 200..299)throw Exception("API HTTP ${c.responseCode}")
 val json=JSONObject(c.inputStream.bufferedReader().use{it.readText()});val arr=json.optJSONObject("data")?.optJSONArray("results");val out=mutableListOf<SaavnSong>()
 if(arr!=null)for(i in 0 until arr.length()){val x=arr.optJSONObject(i)?:continue
 val primary=x.optJSONObject("artists")?.optJSONArray("primary");val names=mutableListOf<String>();if(primary!=null)for(j in 0 until primary.length())primary.optJSONObject(j)?.optString("name")?.takeIf{it.isNotBlank()}?.let(names::add)
 val streams=x.optJSONArray("downloadUrl");var low="";var high="";if(streams!=null)for(j in streams.length()-1 downTo 0){val u=streams.optJSONObject(j)?.optString("url")?:"";if(u.startsWith("http")){if(high.isBlank())high=u;low=u}}
 val imgs=x.optJSONArray("image");var art="";if(imgs!=null)for(j in imgs.length()-1 downTo 0){val u=imgs.optJSONObject(j)?.optString("url")?:"";if(u.startsWith("http")){art=u;break}}
 val d=x.optInt("duration");val duration=if(d>0)String.format("%d:%02d",d/60,d%60)else ""
 out.add(SaavnSong(x.optString("id"),x.optString("name","Unknown"),names.joinToString(", "),x.optJSONObject("album")?.optString("name").orEmpty(),duration,high,art,low))
 };main.post{done(out,null)}
 }catch(e:Exception){main.post{done(emptyList(),e.message?: "Search failed")}}}}
}
private object Offline{
 private val executor=Executors.newCachedThreadPool()
 fun download(c:Context,s:SaavnSong,done:(Boolean,String)->Unit){executor.execute{try{
 val url=s.download.ifBlank{s.stream};if(url.isBlank())throw Exception("Download URL unavailable")
 val target=localFile(c,s);val conn=URL(url).openConnection();conn.connectTimeout=20000;conn.readTimeout=30000
 conn.getInputStream().use{input->target.outputStream().use{output->input.copyTo(output)}}
 if(s.image.isNotBlank())try{URL(s.image).openStream().use{input->File(musicFiles(c),"${safeName(s.name)}_${s.id}.jpg").outputStream().use{output->input.copyTo(output)}}catch(_:Exception){}
 File(musicFiles(c),"${safeName(s.name)}_${s.id}.json").writeText(songJson(s).toString())
 Handler(Looper.getMainLooper()).post{done(true,"Saved for offline listening")}
 }catch(e:Exception){localFile(c,s).delete();Handler(Looper.getMainLooper()).post{done(false,"Download failed: ${e.message}")}}}
}
private fun startStream(context:Context,url:String,title:String,artist:String,album:String,art:String){val i=Intent(context,PlaybackService::class.java).setAction(PlaybackService.ACTION_PLAY).putExtra(PlaybackService.EXTRA_PATH,url).putExtra(PlaybackService.EXTRA_TITLE,title).putExtra(PlaybackService.EXTRA_ARTIST,artist).putExtra(PlaybackService.EXTRA_ALBUM,album).putExtra(PlaybackService.EXTRA_ART,art);ContextCompat.startForegroundService(context,i}
