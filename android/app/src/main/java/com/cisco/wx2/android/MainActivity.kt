package com.cisco.wx2.android

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Build
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
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),1001);setContent{MaterialTheme(colorScheme=darkColorScheme()){MusicScreen(this)}}}
}
private fun storeDir(c:Context)=File(c.filesDir,"offline_music").apply{mkdirs()}
private fun clean(s:String)=s.replace(Regex("[^a-zA-Z0-9._ -]"),"_").trim().take(80).ifBlank{"track"}
private fun audioFile(c:Context,s:SaavnSong)=File(storeDir(c),"${clean(s.name)}_${s.id}.mp3")
private fun metaFile(c:Context,s:SaavnSong)=File(storeDir(c),"${clean(s.name)}_${s.id}.json")
private fun toJson(s:SaavnSong)=JSONObject().put("id",s.id).put("name",s.name).put("artist",s.artist).put("album",s.album).put("duration",s.duration).put("stream",s.stream).put("image",s.image).put("download",s.download)
private fun fromJson(o:JSONObject)=SaavnSong(o.optString("id"),o.optString("name"),o.optString("artist"),o.optString("album"),o.optString("duration"),o.optString("stream"),o.optString("image"),o.optString("download"))
private fun offlineList(c:Context):List<SaavnSong> = (storeDir(c).listFiles()?.filter{it.extension=="json"}?.mapNotNull{try{fromJson(JSONObject(it.readText()))}catch(_:Exception){null}} ?: emptyList()).sortedBy{it.name}
@Composable private fun MusicScreen(context:Context){
 var query by remember{mutableStateOf("")};var results by remember{mutableStateOf<List<SaavnSong>>(emptyList())};var busy by remember{mutableStateOf(false)}
 var status by remember{mutableStateOf("Search songs on Saavn")};var playing by remember{mutableStateOf("")};var library by remember{mutableStateOf(offlineList(context))};var showLibrary by remember{mutableStateOf(false)};var downloadId by remember{mutableStateOf<String?>(null)}
 Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
 Text("Brilliant Pala Classes",style=MaterialTheme.typography.headlineSmall);Text("Hayasaka Music • Saavn")
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={showLibrary=false}){Text("Search")};Button(onClick={library=offlineList(context);showLibrary=true}){Text("Offline library (${library.size})")}}
 if(playing.isNotBlank())Text("Playing: $playing",style=MaterialTheme.typography.titleSmall)
 if(!showLibrary){
 Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
 OutlinedTextField(query,{query=it},label={Text("Song / artist")},singleLine=true,modifier=Modifier.weight(1f))
 Button(enabled=!busy&&query.isNotBlank(),onClick={busy=true;status="Searching…";Saavn.search(query.trim()){list,error->busy=false;results=list;status=error?: "${list.size} results"}}){Text(if(busy)"…" else "Search")}
 }
 Text(status,style=MaterialTheme.typography.bodySmall)
 LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){items(results,key={it.id}){song->
 SongCard(song,audioFile(context,song).exists(),downloadId==song.id,
 {downloadId=song.id;Offline.download(context,song){ok,msg->downloadId=null;library=offlineList(context);status=msg}},
 {if(song.stream.isBlank())Toast.makeText(context,"No stream link",Toast.LENGTH_SHORT).show()else{startPlayback(context,song.stream,song);playing=song.name}})
 }}
 }else{
 Text("Saved on device • playable without internet")
 LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){items(library,key={it.id}){song->
 SongCard(song,true,false,{}, {startPlayback(context,audioFile(context,song).toURI().toString(),song);playing=song.name})
 TextButton(onClick={audioFile(context,song).delete();metaFile(context,song).delete();File(storeDir(context),"${clean(song.name)}_${song.id}.jpg").delete();library=offlineList(context)}){Text("Remove download")}
 }}
 }
}
 } // close MusicScreen
@Composable private fun SongCard(song:SaavnSong,saved:Boolean,loading:Boolean,onDownload:()->Unit,onPlay:()->Unit){
 Card(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
 Column(Modifier.weight(1f)){Text(song.name,style=MaterialTheme.typography.titleSmall);Text(song.artist,style=MaterialTheme.typography.bodySmall);if(song.album.isNotBlank())Text(song.album,style=MaterialTheme.typography.labelSmall);Text(song.duration,style=MaterialTheme.typography.labelSmall)}
 Button(onClick=onPlay){Text("Play")}
 if(saved)Text("✓ Saved",Modifier.padding(8.dp))else if(loading)CircularProgressIndicator(Modifier.size(24.dp).padding(4.dp))else TextButton(onClick=onDownload){Text("↓ Save")}
 }}
}
private object Saavn{
 private val worker=Executors.newSingleThreadExecutor();private val main=Handler(Looper.getMainLooper())
 fun search(q:String,done:(List<SaavnSong>,String?)->Unit){worker.execute{try{
 val c=URL("https://saavn.dev/api/search/songs?query="+URLEncoder.encode(q,"UTF-8")+"&limit=30").openConnection() as HttpURLConnection;c.connectTimeout=15000;c.readTimeout=15000
 if(c.responseCode !in 200..299)throw Exception("Saavn API HTTP ${c.responseCode}")
 val root=JSONObject(c.inputStream.bufferedReader().use{it.readText()});val arr=root.optJSONObject("data")?.optJSONArray("results");val list=mutableListOf<SaavnSong>()
 if(arr!=null)for(i in 0 until arr.length()){val x=arr.optJSONObject(i)?:continue
 val artists=x.optJSONObject("artists")?.optJSONArray("primary");val names=mutableListOf<String>();if(artists!=null)for(j in 0 until artists.length())artists.optJSONObject(j)?.optString("name")?.takeIf{it.isNotBlank()}?.let(names::add)
 val links=x.optJSONArray("downloadUrl");var url="";var low="";if(links!=null)for(j in links.length()-1 downTo 0){val v=links.optJSONObject(j)?.optString("url")?:"";if(v.startsWith("http")){if(url.isBlank())url=v;low=v}}
 val images=x.optJSONArray("image");var cover="";if(images!=null)for(j in images.length()-1 downTo 0){val v=images.optJSONObject(j)?.optString("url")?:"";if(v.startsWith("http")){cover=v;break}}
 val secs=x.optInt("duration");val dur=if(secs>0)String.format("%d:%02d",secs/60,secs%60)else ""
 list.add(SaavnSong(x.optString("id"),x.optString("name","Unknown song"),names.joinToString(", "),x.optJSONObject("album")?.optString("name").orEmpty(),dur,url,cover,low))
 };main.post{done(list,null)}
 }catch(e:Exception){main.post{done(emptyList(),e.message?: "Search failed")}}}}
}
private object Offline{
 private val pool=Executors.newCachedThreadPool()
 fun download(context:Context,song:SaavnSong,done:(Boolean,String)->Unit){pool.execute{try{
 val url=song.download.ifBlank{song.stream};if(url.isBlank())throw Exception("No download URL")
 val con=URL(url).openConnection();con.connectTimeout=20000;con.readTimeout=30000
 con.getInputStream().use{input->audioFile(context,song).outputStream().use{output->input.copyTo(output)}}
 if(song.image.isNotBlank())try{
 URL(song.image).openStream().use{input->File(storeDir(context),"${clean(song.name)}_${song.id}.jpg").outputStream().use{output->input.copyTo(output)}}
 }catch(_:Exception){}
 metaFile(context,song).writeText(toJson(song).toString())
 Handler(Looper.getMainLooper()).post{done(true,"Saved for offline playback")}
 }catch(e:Exception){audioFile(context,song).delete();Handler(Looper.getMainLooper()).post{done(false,"Download failed: ${e.message}")}}}}
}
private fun startPlayback(context:Context,url:String,song:SaavnSong){val artFile=File(storeDir(context),"${clean(song.name)}_${song.id}.jpg");val artwork=if(url==audioFile(context,song).toURI().toString()&&artFile.exists())artFile.toURI().toString()else song.image;val i=Intent(context,PlaybackService::class.java).setAction(PlaybackService.ACTION_PLAY).putExtra(PlaybackService.EXTRA_PATH,url).putExtra(PlaybackService.EXTRA_TITLE,song.name).putExtra(PlaybackService.EXTRA_ARTIST,song.artist).putExtra(PlaybackService.EXTRA_ALBUM,song.album).putExtra(PlaybackService.EXTRA_ART,artwork);ContextCompat.startForegroundService(context,i)}
