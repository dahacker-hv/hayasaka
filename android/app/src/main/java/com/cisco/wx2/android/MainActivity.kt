package com.cisco.wx2.android

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
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

data class SaavnSong(val id:String,val name:String,val artist:String,val album:String,val duration:String,val stream:String,val image:String,val download:String)

class HayasakaActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),1001)
  setContent { MaterialTheme(colorScheme=darkColorScheme()){ MusicScreen(this) } }
 }
}

private fun storeDir(c:Context)=File(c.filesDir,"offline_music").apply{mkdirs()}
private fun clean(s:String)=s.replace(Regex("[^a-zA-Z0-9._ -]"),"_").trim().take(80).ifBlank{"track"}
private fun audioFile(c:Context,s:SaavnSong)=File(storeDir(c),"${clean(s.name)}_${s.id}.mp3")
private fun metaFile(c:Context,s:SaavnSong)=File(storeDir(c),"${clean(s.name)}_${s.id}.json")
private fun artworkFile(c:Context,s:SaavnSong)=File(storeDir(c),"${clean(s.name)}_${s.id}.jpg")
private fun songJson(s:SaavnSong)=JSONObject().put("id",s.id).put("name",s.name).put("artist",s.artist).put("album",s.album).put("duration",s.duration).put("stream",s.stream).put("image",s.image).put("download",s.download)
private fun parseSong(o:JSONObject)=SaavnSong(o.optString("id"),o.optString("name"),o.optString("artist"),o.optString("album"),o.optString("duration"),o.optString("stream"),o.optString("image"),o.optString("download"))
private fun offlineSongs(c:Context):List<SaavnSong> = (storeDir(c).listFiles()?.filter{it.extension=="json"}?.mapNotNull{try{parseSong(JSONObject(it.readText()))}catch(_:Exception){null}} ?: emptyList()).sortedBy{it.name}

@Composable private fun MusicScreen(context:Context){
 var query by remember{mutableStateOf("")}
 var results by remember{mutableStateOf<List<SaavnSong>>(emptyList())}
 var busy by remember{mutableStateOf(false)}
 var status by remember{mutableStateOf("Search songs on Saavn")}
 var current by remember{mutableStateOf("")}
 var library by remember{mutableStateOf(offlineSongs(context))}
 var showOffline by remember{mutableStateOf(false)}
 var downloadingId by remember{mutableStateOf<String?>(null)}
 Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  Text("Brilliant Pala Classes",style=MaterialTheme.typography.headlineSmall)
  Text("Hayasaka Music",style=MaterialTheme.typography.titleMedium)
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   Button(onClick={showOffline=false}){Text("Search")}
   Button(onClick={library=offlineSongs(context);showOffline=true}){Text("Offline (${library.size})")}
  }
  if(current.isNotBlank())Text("Playing: $current",style=MaterialTheme.typography.titleSmall)
  if(!showOffline){
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
    OutlinedTextField(query,{query=it},label={Text("Song or artist")},singleLine=true,modifier=Modifier.weight(1f))
    Button(enabled=!busy&&query.isNotBlank(),onClick={busy=true;status="Searching…";Saavn.search(query.trim()){songs,error->busy=false;results=songs;status=error?: "${songs.size} results found"}}){Text(if(busy)"…" else "Search")}
   }
   Text(status,style=MaterialTheme.typography.bodySmall)
   LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
    items(results,key={it.id}){song->
     SongCard(song,audioFile(context,song).exists(),downloadingId==song.id,
      onDownload={downloadingId=song.id;Offline.download(context,song){_,message->downloadingId=null;library=offlineSongs(context);status=message}},
      onPlay={if(song.stream.isBlank())Toast.makeText(context,"Stream unavailable",Toast.LENGTH_SHORT).show()else{playSong(context,song.stream,song);current=song.name}})
    }
   }
  }else{
   Text("Downloaded audio is stored on this device and works offline.")
   LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
    items(library,key={it.id}){song->
     SongCard(song,true,false,onDownload={},onPlay={playSong(context,audioFile(context,song).toURI().toString(),song);current=song.name})
     TextButton(onClick={audioFile(context,song).delete();metaFile(context,song).delete();artworkFile(context,song).delete();library=offlineSongs(context)}){Text("Remove download")}
    }
   }
  }
 }
}

@Composable private fun SongCard(song:SaavnSong,saved:Boolean,loading:Boolean,onDownload:()->Unit,onPlay:()->Unit){
 Card(Modifier.fillMaxWidth()){
  Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
   Column(Modifier.weight(1f)){
    Text(song.name,style=MaterialTheme.typography.titleSmall)
    Text(song.artist,style=MaterialTheme.typography.bodySmall)
    if(song.album.isNotBlank())Text(song.album,style=MaterialTheme.typography.labelSmall)
    if(song.duration.isNotBlank())Text(song.duration,style=MaterialTheme.typography.labelSmall)
   }
   Button(onClick=onPlay){Text("Play")}
   if(saved)Text("✓ Saved",Modifier.padding(8.dp))
   else if(loading)CircularProgressIndicator(Modifier.size(24.dp).padding(4.dp))
   else TextButton(onClick=onDownload){Text("↓ Save")}
  }
 }
}

private object Saavn{
 private val executor=Executors.newSingleThreadExecutor()
 private val main=Handler(Looper.getMainLooper())
 fun search(q:String,done:(List<SaavnSong>,String?)->Unit){
  executor.execute{
   try{
    val url="https://saavn.dev/api/search/songs?query="+URLEncoder.encode(q,"UTF-8")+"&limit=30"
    val conn=URL(url).openConnection() as HttpURLConnection
    conn.connectTimeout=15000;conn.readTimeout=15000
    if(conn.responseCode !in 200..299)throw Exception("Search HTTP ${conn.responseCode}")
    val root=JSONObject(conn.inputStream.bufferedReader().use{it.readText()})
    val array=root.optJSONObject("data")?.optJSONArray("results")
    val songs=mutableListOf<SaavnSong>()
    if(array!=null)for(i in 0 until array.length()){
     val item=array.optJSONObject(i)?:continue
     val primary=item.optJSONObject("artists")?.optJSONArray("primary")
     val artistNames=mutableListOf<String>()
     if(primary!=null)for(j in 0 until primary.length())primary.optJSONObject(j)?.optString("name")?.takeIf{it.isNotBlank()}?.let{artistNames.add(it)}
     val urls=item.optJSONArray("downloadUrl")
     var stream="";var download=""
     if(urls!=null)for(j in urls.length()-1 downTo 0){
      val u=urls.optJSONObject(j)?.optString("url").orEmpty()
      if(u.startsWith("http")){if(stream.isBlank())stream=u;download=u}
     }
     val imgs=item.optJSONArray("image");var cover=""
     if(imgs!=null)for(j in imgs.length()-1 downTo 0){val u=imgs.optJSONObject(j)?.optString("url").orEmpty();if(u.startsWith("http")){cover=u;break}}
     val seconds=item.optInt("duration");val duration=if(seconds>0)String.format("%d:%02d",seconds/60,seconds%60)else ""
     songs.add(SaavnSong(item.optString("id"),item.optString("name","Unknown"),artistNames.joinToString(", "),item.optJSONObject("album")?.optString("name").orEmpty(),duration,stream,cover,download))
    }
    main.post{done(songs,null)}
   }catch(e:Exception){main.post{done(emptyList(),e.message?: "Search failed")}}
  }
 }
}

private object Offline{
 private val executor=Executors.newCachedThreadPool()
 fun download(context:Context,song:SaavnSong,done:(Boolean,String)->Unit){
  executor.execute{
   try{
    val source=song.download.ifBlank{song.stream}
    if(source.isBlank())throw Exception("No download link available")
    val connection=URL(source).openConnection();connection.connectTimeout=20000;connection.readTimeout=30000
    connection.getInputStream().use{input->audioFile(context,song).outputStream().use{output->input.copyTo(output)}}
    if(song.image.isNotBlank())try{URL(song.image).openStream().use{input->artworkFile(context,song).outputStream().use{output->input.copyTo(output)}}}catch(_:Exception){}
    metaFile(context,song).writeText(songJson(song).toString())
    Handler(Looper.getMainLooper()).post{done(true,"Saved for offline listening")}
   }catch(e:Exception){audioFile(context,song).delete();metaFile(context,song).delete();Handler(Looper.getMainLooper()).post{done(false,"Download failed: ${e.message}")}}
  }
 }
}
private fun playSong(context:Context,url:String,song:SaavnSong){
 val cover=artworkFile(context,song)
 val artwork=if(url==audioFile(context,song).toURI().toString()&&cover.exists())cover.toURI().toString()else song.image
 val intent=Intent(context,PlaybackService::class.java).setAction(PlaybackService.ACTION_PLAY)
  .putExtra(PlaybackService.EXTRA_PATH,url).putExtra(PlaybackService.EXTRA_TITLE,song.name)
  .putExtra(PlaybackService.EXTRA_ARTIST,song.artist).putExtra(PlaybackService.EXTRA_ALBUM,song.album).putExtra(PlaybackService.EXTRA_ART,artwork)
 ContextCompat.startForegroundService(context,intent)
}
