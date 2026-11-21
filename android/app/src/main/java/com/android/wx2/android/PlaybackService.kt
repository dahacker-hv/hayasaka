package com.android.wx2.android

import android.content.Intent
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player!!).build()
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PLAY) {
            val path = intent.getStringExtra(EXTRA_PATH)
            if (!path.isNullOrBlank()) {
                val item = MediaItem.fromUri("file://$path")
                player?.setMediaItem(item)
                player?.prepare()
                player?.play()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }
    override fun onDestroy() {
        mediaSession?.release()
        player?.release()
        mediaSession = null
        player = null
        super.onDestroy()
    }
    companion object {
        const val ACTION_PLAY = "com.android.wx2.android.PLAY"
        const val EXTRA_PATH = "path"
    }
}
