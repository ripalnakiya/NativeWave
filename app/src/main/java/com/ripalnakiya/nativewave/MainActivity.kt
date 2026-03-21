package com.ripalnakiya.nativewave

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.ripalnakiya.nativewave.databinding.ActivityMainBinding
import com.ripalnakiya.nativewave.ui.RenderMode

private const val TAG = "MainActivity"

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

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        recorder = AudioRecorder(binding.waveSurface)

        updateButtonUI(binding.waveSurface.renderMode)

        setupListeners()
    }

    private fun setupListeners() {
        binding.staticButton.setOnClickListener {
            setRenderMode(RenderMode.STATIC)
        }

        binding.movingButton.setOnClickListener {
            setRenderMode(RenderMode.MOVING)
        }

        binding.micButton.setOnClickListener {
            if (hasAudioPermission()) {
                toggleRecording()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun setRenderMode(mode: RenderMode) {
        binding.waveSurface.renderMode = mode
        updateButtonUI(mode)
    }

    private fun updateButtonUI(mode: RenderMode) {
        val neonPink = ContextCompat.getColorStateList(this, R.color.neon_pink)
        val white = ContextCompat.getColorStateList(this, R.color.white)
        val isStatic = mode == RenderMode.STATIC
        styleButton(binding.staticButton, isActive = isStatic, neonPink, white)
        styleButton(binding.movingButton, isActive = !isStatic, neonPink, white)
    }

    private fun styleButton(button: Button, isActive: Boolean, activeColor: ColorStateList?, inactiveColor: ColorStateList?) {
        button.setTextColor(if (isActive) inactiveColor else activeColor)
        button.backgroundTintList = if (isActive) activeColor else inactiveColor
    }

    private fun toggleRecording() {
        if (!AudioRecorder.isRecording) {
            recorder.startRecording()
            updateMicUI(true)
        } else {
            recorder.stopRecording()
            updateMicUI(false)
        }
    }

    private fun updateMicUI(isRecording: Boolean) {
        val iconColor = if (isRecording) R.color.white else R.color.blue
        val bgColor = if (isRecording) R.color.blue else R.color.white
        binding.micButton.imageTintList = ContextCompat.getColorStateList(this, iconColor)
        binding.micButton.backgroundTintList = ContextCompat.getColorStateList(this, bgColor)
    }
}