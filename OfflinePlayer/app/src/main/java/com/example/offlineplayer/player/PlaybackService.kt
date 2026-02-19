package com.example.offlineplayer.player

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.offlineplayer.data.SettingsDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    @Inject lateinit var settingsDao: SettingsDao //Automatically provided by Hilt
    private var mediaSession: MediaSession? = null
    private lateinit var exoPlayer: ExoPlayer //Engine that plays the audio
    private var crossFadeMs: Int = 0 //For user's crossfade setting

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            //If crossfade is enabled, do fade logic
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && crossFadeMs > 0) {
                applyCrossfade()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        //Initialize ExoPlayer
        initializePlayer()
        //Load settings from Room db
        loadSettings()
    }

    private fun initializePlayer() {
        //Initialize ExoPlayer
        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true) //Pauses when headphones are unplugged
            .build()

        //Attach listener so applyCrossfade() can fire
        exoPlayer.addListener(playerListener)

        //Initialize MediaSession and link it to the player
        mediaSession = MediaSession.Builder(this, exoPlayer).build()
    }

    private fun loadSettings() {
        //Use Coroutine to collect the Flow from Room db
        kotlinx.coroutines.MainScope().launch {
            settingsDao.getSettings().collect { settings ->
                settings?.let {
                    crossFadeMs = it.crossfadeSeconds * 1000
                }
            }
        }
    }

    //Gets called when a UI wants to connect to the player
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private fun applyCrossfade() {
        //TO IMPLEMENT
        Log.d("OfflinePlayer", "Crossfading audio items (to be implemented) for $crossFadeMs ms")
        //TO IMPLEMENT
    }
}