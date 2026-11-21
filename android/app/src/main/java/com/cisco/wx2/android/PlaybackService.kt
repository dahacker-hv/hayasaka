package com.cisco.wx2.android

import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService:MediaSessionService(){
 private var player:ExoPlayer?=null;private var session:MediaSession?=null
 override fun onCreate(){super.onCreate();player=ExoPlayer.Builder(this).build();session=MediaSession.Builder(this,player!!).build()}
 override fun onGetSession(controllerInfo:MediaSession.ControllerInfo):MediaSession?=session
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
 if(intent?.action==ACTION_PLAY){val src=intent.getStringExtra(EXTRA_PATH);if(!src.isNullOrBlank()){player?.setMediaItem(MediaItem.fromUri(src));player?.prepare();player?.play()}}
 return super.onStartCommand(intent,flags,startId)
 }
 override fun onDestroy(){session?.release();player?.release();session=null;player=null;super.onDestroy()}
 companion object{const val ACTION_PLAY="com.cisco.wx2.android.PLAY";const val EXTRA_PATH="path"}
}