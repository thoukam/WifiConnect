package com.example.wificonnect

import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class FilePreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fileUrl = intent.getStringExtra("fileUrl") ?: return
        val fileType = intent.getStringExtra("fileType") ?: "image"

        if (fileType == "video") {
            val videoView = VideoView(this)
            videoView.setVideoURI(Uri.parse(fileUrl))
            videoView.setMediaController(MediaController(this))
            videoView.start()
            setContentView(videoView)
        } else {
            val imageView = ImageView(this)
            Glide.with(this).load(fileUrl).into(imageView)
            imageView.adjustViewBounds = true
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            setContentView(imageView)
        }
    }
}