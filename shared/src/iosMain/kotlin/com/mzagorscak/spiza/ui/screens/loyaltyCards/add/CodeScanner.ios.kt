package com.mzagorscak.spiza.ui.screens.loyaltyCards.add

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeAztecCode
import platform.AVFoundation.AVMetadataObjectTypeCodabarCode
import platform.AVFoundation.AVMetadataObjectTypeCode128Code
import platform.AVFoundation.AVMetadataObjectTypeCode39Code
import platform.AVFoundation.AVMetadataObjectTypeCode39Mod43Code
import platform.AVFoundation.AVMetadataObjectTypeCode93Code
import platform.AVFoundation.AVMetadataObjectTypeDataMatrixCode
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.AVMetadataObjectTypeEAN8Code
import platform.AVFoundation.AVMetadataObjectTypeITF14Code
import platform.AVFoundation.AVMetadataObjectTypeInterleaved2of5Code
import platform.AVFoundation.AVMetadataObjectTypePDF417Code
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.AVMetadataObjectTypeUPCECode
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create

// Data Matrix is detected only so the user is told it is unsupported instead of seeing nothing happen.
// iOS has no separate UPC-A type: UPC-A arrives as EAN-13 with a leading zero.
private val SupportedTypes: Map<String?, LoyaltyCardFormat?> = mapOf(
    AVMetadataObjectTypeEAN13Code to LoyaltyCardFormat.Ean13,
    AVMetadataObjectTypeEAN8Code to LoyaltyCardFormat.Ean8,
    AVMetadataObjectTypeUPCECode to LoyaltyCardFormat.UpcE,
    AVMetadataObjectTypeCode128Code to LoyaltyCardFormat.Code128,
    AVMetadataObjectTypeCode39Code to LoyaltyCardFormat.Code39,
    AVMetadataObjectTypeCode39Mod43Code to LoyaltyCardFormat.Code39,
    AVMetadataObjectTypeCode93Code to LoyaltyCardFormat.Code93,
    AVMetadataObjectTypeITF14Code to LoyaltyCardFormat.Itf,
    AVMetadataObjectTypeInterleaved2of5Code to LoyaltyCardFormat.Itf,
    AVMetadataObjectTypeCodabarCode to LoyaltyCardFormat.Codabar,
    AVMetadataObjectTypeQRCode to LoyaltyCardFormat.QrCode,
    AVMetadataObjectTypeAztecCode to LoyaltyCardFormat.Aztec,
    AVMetadataObjectTypePDF417Code to LoyaltyCardFormat.Pdf417,
    AVMetadataObjectTypeDataMatrixCode to null,
)

@Composable
actual fun CodeScanner(
    onCodeScanned: (ScannedCode) -> Unit,
    modifier: Modifier,
) {
    val currentOnCodeScanned by rememberUpdatedState(onCodeScanned)
    val scannerView = remember { CodeScannerView { code -> currentOnCodeScanned(code) } }

    DisposableEffect(scannerView) {
        scannerView.start()
        onDispose { scannerView.stop() }
    }

    UIKitView(factory = { scannerView }, modifier = modifier)
}

@OptIn(ExperimentalForeignApi::class)
private class CodeScannerView(
    onCodeScanned: (ScannedCode) -> Unit,
) : UIView(frame = CGRectZero.readValue()) {
    private val session = AVCaptureSession()

    // Serial, so a quick stop after start is applied in order; startRunning blocks, so it stays off the main thread.
    private val sessionQueue = dispatch_queue_create("com.mzagorscak.spiza.codeScanner", null)
    private val previewLayer = AVCaptureVideoPreviewLayer(session = session)

    // AVCaptureMetadataOutput does not retain its delegate.
    private val delegate = MetadataDelegate(onCodeScanned)
    private var isConfigured = false

    init {
        previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
        layer.addSublayer(previewLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        previewLayer.frame = bounds
    }

    fun start() {
        if (isConfigured.not()) isConfigured = configure()
        if (isConfigured.not()) return
        dispatch_async(sessionQueue) { session.startRunning() }
    }

    fun stop() {
        dispatch_async(sessionQueue) { session.stopRunning() }
    }

    private fun configure(): Boolean {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return false
        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, error = null) ?: return false
        if (session.canAddInput(input).not()) return false
        session.addInput(input)

        val output = AVCaptureMetadataOutput()
        if (session.canAddOutput(output).not()) return false
        session.addOutput(output)
        output.setMetadataObjectsDelegate(delegate, queue = dispatch_get_main_queue())
        // Types can only be set after the output joined the session, and only those the device offers.
        val available = output.availableMetadataObjectTypes.toSet()
        output.metadataObjectTypes = SupportedTypes.keys.filterNotNull().filter { it in available }
        return true
    }
}

private class MetadataDelegate(
    private val onCodeScanned: (ScannedCode) -> Unit,
) : NSObject(),
    AVCaptureMetadataOutputObjectsDelegateProtocol {
    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection,
    ) {
        val code = didOutputMetadataObjects
            .filterIsInstance<AVMetadataMachineReadableCodeObject>()
            .firstNotNullOfOrNull { metadata ->
                metadata.stringValue?.let { value -> ScannedCode(value = value, format = SupportedTypes[metadata.type]) }
            }
        code?.let(onCodeScanned)
    }
}
