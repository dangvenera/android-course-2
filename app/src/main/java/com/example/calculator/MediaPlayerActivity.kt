package com.example.calculator

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.util.concurrent.TimeUnit

class MediaPlayerActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var playButton: Button
    private lateinit var pauseButton: Button
    private lateinit var seekBar: SeekBar
    private lateinit var volumeBar: SeekBar
    private lateinit var fileListView: ListView
    private lateinit var statusText: TextView
    private lateinit var currentTimeText: TextView
    private lateinit var totalTimeText: TextView

    private lateinit var audioManager: AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var isUserSeeking = false

    private var musicList = mutableListOf<Pair<String, String>>()
    private var currentTrackIndex = -1

    // отслежка системной громкости
    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                volumeBar.progress = currentVolume
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_player)

        // инициализация для интерфейса
        playButton = findViewById(R.id.btn_play)
        pauseButton = findViewById(R.id.btn_pause)
        seekBar = findViewById(R.id.seekBar)
        volumeBar = findViewById(R.id.volumeBar)
        fileListView = findViewById(R.id.fileList)
        statusText = findViewById(R.id.statusText)
        currentTimeText = findViewById(R.id.currentTimeText)
        totalTimeText = findViewById(R.id.totalTimeText)

        val nextButton = findViewById<Button>(R.id.btn_next)
        val prevButton = findViewById<Button>(R.id.btn_prev)

        // настройка аудиоменеджера для громкости
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        volumeBar.max = maxVolume
        volumeBar.progress = currentVolume

        volumeBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // регистрирую изменение громкости
        registerReceiver(volumeReceiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))

        // запрос разрешения
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                loadMusicFiles()
            } else {
                Toast.makeText(this, "Разрешение не предоставлено", Toast.LENGTH_SHORT).show()
            }
        }

        requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)

        // запуск музыки
        playButton.setOnClickListener {
            if (mediaPlayer == null) {
                Toast.makeText(this, "Выберите трек", Toast.LENGTH_SHORT).show()
            } else {
                mediaPlayer?.start()
                updateSeekBar()
            }
        }

        // пауза
        pauseButton.setOnClickListener {
            mediaPlayer?.pause()
        }

        nextButton.setOnClickListener {
            if (currentTrackIndex < musicList.size - 1) {
                currentTrackIndex++
                playCurrentTrack()
            }
        }

        prevButton.setOnClickListener {
            if (currentTrackIndex > 0) {
                currentTrackIndex--
                playCurrentTrack()
            }
        }

        // сикбар для перемотки
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentTimeText.text = formatTime(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(sb: SeekBar?) {
                isUserSeeking = false
                mediaPlayer?.seekTo(sb?.progress ?: 0)
            }
        })
    }

    // загрузка музыки через медиастор
    private fun loadMusicFiles() {
        musicList.clear()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            android.provider.MediaStore.Audio.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL)
        } else {
            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            android.provider.MediaStore.Audio.Media.DISPLAY_NAME,
            android.provider.MediaStore.Audio.Media.DATA
        )

        val cursor = contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${android.provider.MediaStore.Audio.Media.DISPLAY_NAME} ASC"
        )

        cursor?.use {
            val nameColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DISPLAY_NAME)
            val dataColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
            while (it.moveToNext()) {
                val name = it.getString(nameColumn)
                val path = it.getString(dataColumn)
                musicList.add(Pair(name, path))
            }
        }

        if (musicList.isEmpty()) {
            statusText.text = "Музыкальные файлы не найдены"
            return
        }

        val names = musicList.map { it.first }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
        fileListView.adapter = adapter

        fileListView.setOnItemClickListener { _, _, position, _ ->
            currentTrackIndex = position
            playCurrentTrack()
        }
    }

    private fun playCurrentTrack() {
        if (currentTrackIndex < 0 || currentTrackIndex >= musicList.size) return
        val (_, path) = musicList[currentTrackIndex]
        playFile(File(path))
    }

    // воспроизведение файла
    private fun playFile(file: File) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer()
        mediaPlayer?.setDataSource(file.path)
        mediaPlayer?.prepare()
        mediaPlayer?.start()

        statusText.text = "Играет: ${file.name}"
        seekBar.max = mediaPlayer!!.duration
        totalTimeText.text = formatTime(mediaPlayer!!.duration.toLong())
        updateSeekBar()
    }

    // обновление позиции и времени трека
    private fun updateSeekBar() {
        mediaPlayer?.let {
            if (!isUserSeeking) {
                seekBar.progress = it.currentPosition
                currentTimeText.text = formatTime(it.currentPosition.toLong())
            }
            if (it.isPlaying) {
                handler.postDelayed({ updateSeekBar() }, 500)
            }
        }
    }

    // форматирование в минуты/секунды
    private fun formatTime(ms: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) -
                TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(volumeReceiver)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
