package com.example.offlineplayer.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaControllerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    var controller: MediaController? = null
        private set

    init {
        setupController()
    }

    private fun setupController() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture?.addListener({
            controller = controllerFuture?.get()
        }, MoreExecutors.directExecutor())
    }

    fun releaseController() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}