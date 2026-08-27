package com.skynet.v380mvr

import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    private lateinit var playerView: PlayerView
    private lateinit var status: TextView
    private var player: ExoPlayer? = null
    private var currentTemp: File? = null

    private val openMvr = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        status.text = "Opening MVR..."
        thread { prepareMvr(uri) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(com.skynet.v380mvr.R.drawable.bg)
        }

        status = TextView(this).apply { text = "V380 MVR Player"; setPadding(24,18,24,12) }
        playerView = PlayerView(this).apply {
            useController = true
            controllerAutoShow = true
            setShowFastForwardButton(false)
            setShowRewindButton(false)
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }

        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun b(text:String, action:()->Unit) = Button(this).apply { setText(text); setOnClickListener{action()} }
        bar.addView(b("📂 Open MVR") { openMvr.launch(arrayOf("*/*")) }, LinearLayout.LayoutParams(0, -2, 2f))
        bar.addView(b("⏪ 10s") { player?.seekBack() }, LinearLayout.LayoutParams(0, -2, 1f))
        bar.addView(b("▶/⏸") { player?.let { if (it.isPlaying) it.pause() else it.play() } }, LinearLayout.LayoutParams(0, -2, 1f))
        bar.addView(b("10s ⏩") { player?.seekForward() }, LinearLayout.LayoutParams(0, -2, 1f))
        bar.addView(b("📸") { screenshot() }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(status, LinearLayout.LayoutParams(-1,-2))
        root.addView(playerView)
        root.addView(bar, LinearLayout.LayoutParams(-1,-2))
        setContentView(root)
    }

    private fun prepareMvr(uri: Uri) {
        try {
            val input = File(cacheDir, "input_${System.currentTimeMillis()}.mvr")
            contentResolver.openInputStream(uri)!!.use { ins ->
                FileOutputStream(input).use { out -> ins.copyTo(out) }
            }
            val bytes = input.readBytes()
            var off = -1
            var p = 0
            while (true) {
                val i = indexOfStartCode(bytes, p)
                if (i < 0) break
                if (i + 4 < bytes.size && (bytes[i+4].toInt() and 0x1f) == 7) { off=i; break }
                p=i+4
            }
            if (off < 0) throw Exception("H.264 video stream not found")
            val raw = File(cacheDir, "stream_${System.currentTimeMillis()}.h264")
            FileOutputStream(raw).use { it.write(bytes, off, bytes.size-off) }
            val mp4 = File(cacheDir, "play_${System.currentTimeMillis()}.mp4")
            val cmd = "-y -err_detect ignore_err -f h264 -i ${q(raw.path)} -c:v copy -an ${q(mp4.path)}"
            val session = FFmpegKit.execute(cmd)
            if (!ReturnCode.isSuccess(session.returnCode)) throw Exception("Video recovery failed")
            runOnUiThread {
                currentTemp?.delete()
                currentTemp = mp4
                player?.release()
                player = ExoPlayer.Builder(this).build().also { exo ->
                    playerView.player = exo
                    exo.setMediaItem(MediaItem.fromUri(Uri.fromFile(mp4)))
                    exo.prepare()
                    exo.play()
                }
                status.text = "Playing: ${displayName(uri)}"
            }
            input.delete(); raw.delete()
        } catch (e: Exception) {
            runOnUiThread { status.text = "Error"; Toast.makeText(this, e.message ?: "Cannot play MVR", Toast.LENGTH_LONG).show() }
        }
    }

    private fun indexOfStartCode(data: ByteArray, from:Int):Int {
        for (i in from until data.size-4)
            if (data[i]==0.toByte() && data[i+1]==0.toByte() && data[i+2]==0.toByte() && data[i+3]==1.toByte()) return i
        return -1
    }
    private fun q(s:String)= "'" + s.replace("'","'\\''") + "'"

    private fun displayName(uri:Uri):String {
        var n="MVR"
        contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use { c ->
            if(c.moveToFirst()) n=c.getString(0)
        }
        return n
    }

    private fun screenshot() {
        val v=playerView.videoSurfaceView ?: return
        v.post {
            val bmp=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888)
            val canvas=android.graphics.Canvas(bmp); v.draw(canvas)
            val dir=getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
            val f=File(dir,"MVR_${System.currentTimeMillis()}.png")
            FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG,100,it) }
            Toast.makeText(this,"Screenshot saved",Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        player?.release()
        currentTemp?.delete()
        super.onDestroy()
    }
}