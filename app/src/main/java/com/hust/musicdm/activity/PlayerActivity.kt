package com.hust.musicdm.activity

import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.hust.musicdm.R
import com.hust.musicdm.model.Song
import com.hust.musicdm.databinding.ActivityPlayerBinding
import java.io.File
import androidx.core.net.toUri

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var songList: List<Song>

    private var currentIndex = 0
    private var isShuffle = false
    private var isRepeat = false
    private var shuffledList: List<Song> = emptyList()

    private val handler = Handler(Looper.getMainLooper())
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            handler.postDelayed(this, 100)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get song list from intent
        songList =
            intent.getParcelableArrayListExtra("songList", Song::class.java) ?: emptyList()

        currentIndex = intent.getIntExtra("position", 0)
        shuffledList = songList

        setupExoPlayer()
        setupClickListeners()
        loadSong()
    }

    private fun setupExoPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayPauseButton(isPlaying)
                if (isPlaying) {
                    handler.post(updateProgressRunnable)
                } else {
                    handler.removeCallbacks(updateProgressRunnable)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    if (isRepeat) {
                        exoPlayer.seekTo(0)
                        exoPlayer.play()
                    } else {
                        skipNext()
                    }
                }
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnPlayPause.setOnClickListener {
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            } else {
                exoPlayer.play()
            }
        }

        binding.btnPrevious.setOnClickListener { skipPrevious() }
        binding.btnNext.setOnClickListener { skipNext() }

        binding.btnRepeat.setOnClickListener {
            isRepeat = !isRepeat
            exoPlayer.repeatMode = if (isRepeat) {
                Player.REPEAT_MODE_ONE
            } else {
                Player.REPEAT_MODE_OFF
            }
            updateRepeatButton()
        }

        binding.btnShuffle.setOnClickListener {
            isShuffle = !isShuffle
            if (isShuffle) {
                shuffledList = songList.shuffled()
            } else {
                shuffledList = songList
            }
            updateShuffleButton()
        }

        binding.waveformView.setOnSeekListener { percent ->
            val seekPosition = (exoPlayer.duration * percent).toLong()
            exoPlayer.seekTo(seekPosition)
        }
    }

    @OptIn(UnstableApi::class)
    private fun loadSong() {
        val list = if (isShuffle) shuffledList else songList
        val song = list.getOrNull(currentIndex) ?: return

        binding.tvSongTitle.text = song.title ?: "Unknown"
        binding.tvArtist.text = song.artist ?: "Unknown Artist"

        // Load album art
        Glide.with(this)
            .load(song.albumArt)
            .placeholder(R.drawable.baseline_music_note_24)
            .error(R.drawable.baseline_music_note_24)
            .into(binding.ivAlbumArt)

        val mediaUri = if (song.isDownloaded && song.localPath != null) {
            Uri.fromFile(File(song.localPath))
        } else {
            song.streamUrl.toUri()
        }

        exoPlayer.setMediaItem(MediaItem.fromUri(mediaUri))
        exoPlayer.prepare()
        exoPlayer.play()
    }

    private fun skipPrevious() {
        val list = if (isShuffle) shuffledList else songList
        currentIndex = if (currentIndex - 1 < 0) list.size - 1 else currentIndex - 1
        loadSong()
    }

    private fun skipNext() {
        val list = if (isShuffle) shuffledList else songList
        currentIndex = (currentIndex + 1) % list.size
        loadSong()
    }

    private fun updateProgress() {
        val elapsed = exoPlayer.currentPosition
        val duration = exoPlayer.duration

        binding.tvElapsed.text = formatTime((elapsed / 1000).toInt())
        binding.tvDuration.text = formatTime((duration / 1000).toInt())

        val progress = if (duration > 0) {
            elapsed.toFloat() / duration.toFloat()
        } else 0f

        binding.waveformView.setProgress(progress)
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        binding.btnPlayPause.setImageResource(
            if (isPlaying) R.drawable.baseline_pause_24
            else R.drawable.baseline_play_arrow_24
        )
    }

    private fun updateRepeatButton() {
        val color = if (isRepeat) {
            ContextCompat.getColor(this, R.color.purple_500)
        } else {
            Color.WHITE
        }
        binding.btnRepeat.setColorFilter(color)
    }

    private fun updateShuffleButton() {
        val color = if (isShuffle) {
            ContextCompat.getColor(this, R.color.purple_500)
        } else {
            Color.WHITE
        }
        binding.btnShuffle.setColorFilter(color)
    }

    private fun formatTime(seconds: Int): String {
        return String.format("%02d:%02d", seconds / 60, seconds % 60)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateProgressRunnable)
        exoPlayer.release()
    }
}