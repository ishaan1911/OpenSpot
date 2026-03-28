package com.wpi.openspot.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.wpi.openspot.R
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanIdFragment : Fragment(R.layout.fragment_scan_id) {

    private lateinit var cameraExecutor: ExecutorService
    private var scanningEnabled = true
    private var scannedName = ""
    private var scannedId = ""

    private val cameraPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else Toast.makeText(requireContext(), "Camera permission required to scan ID", Toast.LENGTH_LONG).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        val btnManualEntry = view.findViewById<MaterialButton>(R.id.btnManualEntry)

        btnManualEntry.setOnClickListener {
            // Navigate to confirm with empty fields for manual entry
            val bundle = Bundle().apply {
                putString("name", "")
                putString("wpiId", "")
            }
            findNavController().navigate(R.id.toConfirm, bundle)
        }

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            cameraPermissionRequest.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(
                    requireView().findViewById<PreviewView>(R.id.previewView).surfaceProvider
                )
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, WpiIdAnalyzer { name, wpiId ->
                        if (scanningEnabled) {
                            scanningEnabled = false
                            requireActivity().runOnUiThread {
                                handleScanResult(name, wpiId)
                            }
                        }
                    })
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Log.e("OpenSpot", "Camera binding failed: ${e.message}")
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun handleScanResult(name: String, wpiId: String) {
        scannedName = name
        scannedId = wpiId

        val tvScannedText = requireView().findViewById<TextView>(R.id.tvScannedText)
        tvScannedText.visibility = View.VISIBLE
        tvScannedText.text = "Detected:\nName: $name\nWPI ID: $wpiId\n\nNavigating to confirm..."

        Log.d("OpenSpot", "Scanned — Name: $name, WPI ID: $wpiId")

        requireView().postDelayed({
            val bundle = Bundle().apply {
                putString("name", name)
                putString("wpiId", wpiId)
            }
            findNavController().navigate(R.id.toConfirm, bundle)
        }, 1500)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }
}

class WpiIdAnalyzer(private val onResult: (name: String, wpiId: String) -> Unit) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val fullText = visionText.text
                Log.d("OpenSpot", "MLKit scanned text: $fullText")

                // Extract name and WPI ID from scanned text
                val name = extractName(fullText)
                val wpiId = extractWpiId(fullText)

                if (name.isNotEmpty() && wpiId.isNotEmpty()) {
                    onResult(name, wpiId)
                }
            }
            .addOnFailureListener { e ->
                Log.e("OpenSpot", "MLKit error: ${e.message}")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun extractName(text: String): String {
        // WPI ID cards have name on a line — look for lines with common name patterns
        val lines = text.lines()
        for (line in lines) {
            val trimmed = line.trim()
            // Name lines typically have 2-4 words, all letters
            if (trimmed.split(" ").size in 2..4 &&
                trimmed.all { it.isLetter() || it.isWhitespace() } &&
                trimmed.length > 4
            ) {
                return trimmed
            }
        }
        return ""
    }

    private fun extractWpiId(text: String): String {
        // WPI IDs are 9-digit numbers
        val pattern = Regex("\\b\\d{9}\\b")
        return pattern.find(text)?.value ?: ""
    }
}
