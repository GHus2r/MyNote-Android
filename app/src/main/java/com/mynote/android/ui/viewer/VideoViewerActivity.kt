package com.mynote.android.ui.viewer

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.mynote.android.R
import java.io.File

/**
 * 内置视频查看器，全屏播放 + 播放控制条
 */
class VideoViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PATH = "video_path"
    }

    private lateinit var vv: VideoView
    private lateinit var btnPlay: TextView
    private lateinit var tvCurrent: TextView
    private lateinit var tvTotal: TextView
    private lateinit var seekBar: SeekBar
    private val handler = Handler(Looper.getMainLooper())
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_viewer)

        vv = findViewById(R.id.vv_player)
        btnPlay = findViewById(R.id.btn_video_play)
        tvCurrent = findViewById(R.id.tv_current_time)
        tvTotal = findViewById(R.id.tv_total_time)
        seekBar = findViewById(R.id.seek_bar)

        findViewById<TextView>(R.id.btn_video_back).setOnClickListener { finish() }

        val path = intent.getStringExtra(EXTRA_PATH) ?: run { finish(); return }
        val file = File(path)
        if (!file.exists()) { Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show(); finish(); return }

        vv.setVideoURI(Uri.fromFile(file))
        vv.setOnPreparedListener { mp ->
            val total = mp.duration
            tvTotal.text = formatTime(total)
            seekBar.max = total
            startPlay()
            updateProgress()
        }
        vv.setOnCompletionListener {
            isPlaying = false; btnPlay.visibility = View.VISIBLE
            seekBar.progress = seekBar.max; tvCurrent.text = tvTotal.text
        }
        vv.setOnErrorListener { _, _, _ ->
            Toast.makeText(this, "无法播放视频", Toast.LENGTH_SHORT).show()
            finish(); true
        }

        btnPlay.setOnClickListener {
            if (isPlaying) pausePlay() else startPlay()
        }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) { vv.seekTo(progress); tvCurrent.text = formatTime(progress) }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun startPlay() {
        vv.start(); isPlaying = true; btnPlay.visibility = View.GONE
    }

    private fun pausePlay() {
        vv.pause(); isPlaying = false; btnPlay.visibility = View.VISIBLE
    }

    private fun updateProgress() {
        handler.postDelayed({
            if (isPlaying) {
                val pos = vv.currentPosition
                seekBar.progress = pos
                tvCurrent.text = formatTime(pos)
            }
            handler.postDelayed({ updateProgress() }, 200)
        }, 200)
    }

    private fun formatTime(ms: Int): String {
        val s = ms / 1000
        return "${"%02d".format(s / 60)}:${"%02d".format(s % 60)}"
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        vv.stopPlayback()
    }
}
