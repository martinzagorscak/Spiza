package com.mzagorscak.spiza.ui.screens.loyaltyCards.add

import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.crash.recordError
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import org.koin.compose.koinInject
import java.util.concurrent.Executors

@Composable
actual fun CodeScanner(
    onCodeScanned: (ScannedCode) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val crashLogger = koinInject<CrashLogger>()
    val currentOnCodeScanned by rememberUpdatedState(onCodeScanned)
    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            // The default SurfaceView is composited above the window and ignores Compose alpha and clipping, so the preview
            // stayed fully visible during the navigation exit animation. TextureView is drawn as part of the Compose layer.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner, previewView) {
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val scanner = BarcodeScanning.getClient()
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val analysis = ImageAnalysis
            .Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(analysisExecutor, BarcodeAnalyzer(scanner = scanner) { code -> currentOnCodeScanned(code) }) }
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var cameraProvider: ProcessCameraProvider? = null
        var isDisposed = false

        providerFuture.addListener(
            {
                if (isDisposed) return@addListener
                try {
                    cameraProvider = providerFuture.get().also { provider ->
                        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                    }
                } catch (error: Exception) {
                    // No back camera or the camera is in use by another app; the preview just stays empty.
                    crashLogger.recordError(context = "Starting the code scanner camera failed", error = error)
                }
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            isDisposed = true
            cameraProvider?.unbind(preview, analysis)
            analysis.clearAnalyzer()
            scanner.close()
            analysisExecutor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

private class BarcodeAnalyzer(
    private val scanner: BarcodeScanner,
    private val onCodeScanned: (ScannedCode) -> Unit,
) : ImageAnalysis.Analyzer {
    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        scanner
            .process(InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees))
            .addOnSuccessListener { barcodes ->
                barcodes.firstNotNullOfOrNull { it.toScannedCode() }?.let(onCodeScanned)
            }.addOnCompleteListener { imageProxy.close() }
    }
}

// rawValue is the exact encoded content; displayValue is reformatted by ML Kit and must not be stored.
private fun Barcode.toScannedCode(): ScannedCode? =
    rawValue?.let { value ->
        ScannedCode(
            value = value,
            format = when (format) {
                Barcode.FORMAT_EAN_13 -> LoyaltyCardFormat.Ean13
                Barcode.FORMAT_EAN_8 -> LoyaltyCardFormat.Ean8
                Barcode.FORMAT_UPC_A -> LoyaltyCardFormat.UpcA
                Barcode.FORMAT_UPC_E -> LoyaltyCardFormat.UpcE
                Barcode.FORMAT_CODE_128 -> LoyaltyCardFormat.Code128
                Barcode.FORMAT_CODE_39 -> LoyaltyCardFormat.Code39
                Barcode.FORMAT_CODE_93 -> LoyaltyCardFormat.Code93
                Barcode.FORMAT_ITF -> LoyaltyCardFormat.Itf
                Barcode.FORMAT_CODABAR -> LoyaltyCardFormat.Codabar
                Barcode.FORMAT_QR_CODE -> LoyaltyCardFormat.QrCode
                Barcode.FORMAT_AZTEC -> LoyaltyCardFormat.Aztec
                Barcode.FORMAT_PDF417 -> LoyaltyCardFormat.Pdf417
                else -> null
            },
        )
    }
