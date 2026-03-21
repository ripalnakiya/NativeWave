package com.ripalnakiya.nativewave

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.ripalnakiya.nativewave.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var recorder: AudioRecorder
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                toggleRecording()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        recorder = AudioRecorder(binding.waveSurface)

        binding.micButton.setOnClickListener {
            if (hasAudioPermission()) {
                toggleRecording()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun toggleRecording() {
        if (!recorder.isRecording) {
            recorder.startRecording()
            updateUI(true)
        } else {
            recorder.stopRecording()
            updateUI(false)
        }
    }

    private fun updateUI(isRecording: Boolean) {
        val iconColor = if (isRecording) R.color.blue else R.color.white
        val bgColor = if (isRecording) R.color.white else R.color.blue

        binding.micButton.imageTintList = ContextCompat.getColorStateList(this, iconColor)
        binding.micButton.backgroundTintList = ContextCompat.getColorStateList(this, bgColor)
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
}