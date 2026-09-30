package com.example.jarvis.voice

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class VoiceMemoItem(
    val file: File,
    val title: String,
    val timestampMillis: Long,
    val durationSeconds: Int,
    val sizeBytes: Long
)

class VoiceMemoManager(private val context: Context) {

    private val voiceDir = File(context.filesDir, "voice_memos").apply {
        if (!exists()) mkdirs()
    }

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var currentRecordingFile: File? = null
    private var recordingStartTime = 0L

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()

    private val _currentRmsDb = MutableStateFlow(0f)
    val currentRmsDb: StateFlow<Float> = _currentRmsDb.asStateFlow()

    private val _currentlyPlayingPath = MutableStateFlow<String?>(null)
    val currentlyPlayingPath: StateFlow<String?> = _currentlyPlayingPath.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val _memoList = MutableStateFlow<List<VoiceMemoItem>>(emptyList())
    val memoList: StateFlow<List<VoiceMemoItem>> = _memoList.asStateFlow()

    private var recordTimerJob: Job? = null
    private var playbackProgressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        refreshMemoList()
    }

    fun refreshMemoList() {
        val files = voiceDir.listFiles { f -> f.extension == "m4a" }?.sortedByDescending { it.lastModified() } ?: emptyList()
        _memoList.value = files.map { file ->
            val duration = getAudioDurationSec(file)
            val title = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.US).format(Date(file.lastModified()))
            VoiceMemoItem(
                file = file,
                title = title,
                timestampMillis = file.lastModified(),
                durationSeconds = duration,
                sizeBytes = file.length()
            )
        }
    }

    private fun getAudioDurationSec(file: File): Int {
        return try {
            val mp = MediaPlayer()
            mp.setDataSource(file.absolutePath)
            mp.prepare()
            val dur = mp.duration / 1000
            mp.release()
            dur
        } catch (_: Exception) {
            0
        }
    }

    fun startRecording(): Boolean {
        if (_isRecording.value) return false
        stopPlayback()

        return try {
            val timestamp = System.currentTimeMillis()
            val fileName = "JARVIS_VOICE_$timestamp.m4a"
            val file = File(voiceDir, fileName)
            currentRecordingFile = file

            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            rec.setAudioSource(MediaRecorder.AudioSource.MIC)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            rec.setAudioEncodingBitRate(128000)
            rec.setAudioSamplingRate(44100)
            rec.setOutputFile(file.absolutePath)
            rec.prepare()
            rec.start()

            recorder = rec
            _isRecording.value = true
            recordingStartTime = System.currentTimeMillis()
            _recordingDurationSeconds.value = 0

            recordTimerJob = scope.launch {
                while (_isRecording.value) {
                    val durSec = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
                    _recordingDurationSeconds.value = durSec

                    val amp = try {
                        recorder?.maxAmplitude ?: 0
                    } catch (_: Exception) { 0 }
                    val db = if (amp > 0) (20 * kotlin.math.log10(amp.toDouble() / 32767.0)).toFloat().coerceIn(-40f, 0f) else -40f
                    _currentRmsDb.value = db

                    delay(100)
                }
            }
            true
        } catch (_: Exception) {
            _isRecording.value = false
            false
        }
    }

    fun stopRecording(): File? {
        if (!_isRecording.value) return null
        recordTimerJob?.cancel()

        val saved = try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            currentRecordingFile
        } catch (_: Exception) {
            null
        } finally {
            _isRecording.value = false
            _recordingDurationSeconds.value = 0
            _currentRmsDb.value = 0f
            refreshMemoList()
        }
        return saved
    }

    fun togglePlay(memo: VoiceMemoItem) {
        if (_currentlyPlayingPath.value == memo.file.absolutePath) {
            stopPlayback()
        } else {
            playMemo(memo.file)
        }
    }

    private fun playMemo(file: File) {
        stopPlayback()
        try {
            val mp = MediaPlayer()
            mp.setDataSource(file.absolutePath)
            mp.prepare()
            mp.setOnCompletionListener {
                stopPlayback()
            }
            mp.start()
            player = mp
            _currentlyPlayingPath.value = file.absolutePath

            playbackProgressJob = scope.launch {
                while (player != null && player?.isPlaying == true) {
                    val current = player?.currentPosition ?: 0
                    val total = player?.duration ?: 1
                    _playbackProgress.value = (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    delay(100)
                }
            }
        } catch (_: Exception) {
            stopPlayback()
        }
    }

    fun stopPlayback() {
        playbackProgressJob?.cancel()
        playbackProgressJob = null
        try {
            player?.stop()
            player?.release()
        } catch (_: Exception) {}
        player = null
        _currentlyPlayingPath.value = null
        _playbackProgress.value = 0f
    }

    fun deleteMemo(memo: VoiceMemoItem) {
        if (_currentlyPlayingPath.value == memo.file.absolutePath) {
            stopPlayback()
        }
        try {
            memo.file.delete()
        } catch (_: Exception) {}
        refreshMemoList()
    }
}
