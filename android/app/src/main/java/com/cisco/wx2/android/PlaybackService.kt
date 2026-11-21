package com.cisco.wx2.android

import android.content.Intent
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService:MediaSessionService(){
 private var player:ExoPlayer?=null
 private var session:MediaSession?=null
 override fun onCreate(){
  super.onCreate()
  player=ExoPlayer.Builder(this).build()
  session=MediaSession.Builder(this,player!!).build()
 }
 override fun onGetSession(controllerInfo:MediaSession.ControllerInfo):MediaSession?=session
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
  if(intent?.action==ACTION_PLAY){
   val source=intent.getStringExtra(EXTRA_PATH)
   if(!source.isNullOrBlank()){
    val title=intent.getStringExtra(EXTRA_TITLE).orEmpty()
    val artist=intent.getStringExtra(EXTRA_ARTIST).orEmpty()
    val album=intent.getStringExtra(EXTRA_ALBUM).orEmpty()
    val artwork=intent.getStringExtra(EXTRA_ART).orEmpty()
    val metadata=MediaMetadata.Builder().setTitle(title).setArtist(artist).setAlbumTitle(album).apply{if(artwork.isNotBlank())setArtworkUri(Uri.parse(artwork))}.build()
    val item=MediaItem.Builder().setUri(source).setMediaMetadata(metadata).build()
    player?.setMediaItem(item);player?.prepare();player?.play()
   }
  }
  return super.onStartCommand(intent,flags,startId)
 }
 override fun onDestroy(){session?.release();player?.release();session=null;player=null;super.onDestroy()}
 companion object{
  const val ACTION_PLAY="com.cisco.wx2.android.PLAY"
  const val EXTRA_PATH="path"
  const val EXTRA_TITLE="title"
  const val EXTRA_ARTIST="artist"
  const val EXTRA_ALBUM="album"
  const val EXTRA_ART="artwork"
 }
}
