package com.mzagorscak.spiza.ui.screens.loyaltyCards.add

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.crash.recordError
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import org.koin.compose.koinInject
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.Vision.VNBarcodeObservation
import platform.Vision.VNBarcodeSymbologyAztec
import platform.Vision.VNBarcodeSymbologyCodabar
import platform.Vision.VNBarcodeSymbologyCode128
import platform.Vision.VNBarcodeSymbologyCode39
import platform.Vision.VNBarcodeSymbologyCode39Checksum
import platform.Vision.VNBarcodeSymbologyCode39FullASCII
import platform.Vision.VNBarcodeSymbologyCode39FullASCIIChecksum
import platform.Vision.VNBarcodeSymbologyCode93
import platform.Vision.VNBarcodeSymbologyCode93i
import platform.Vision.VNBarcodeSymbologyDataMatrix
import platform.Vision.VNBarcodeSymbologyEAN13
import platform.Vision.VNBarcodeSymbologyEAN8
import platform.Vision.VNBarcodeSymbologyI2of5
import platform.Vision.VNBarcodeSymbologyI2of5Checksum
import platform.Vision.VNBarcodeSymbologyITF14
import platform.Vision.VNBarcodeSymbologyPDF417
import platform.Vision.VNBarcodeSymbologyQR
import platform.Vision.VNBarcodeSymbologyUPCE
import platform.Vision.VNDetectBarcodesRequest
import platform.Vision.VNImageRequestHandler
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

// Same mapping as the camera scanner (see SupportedTypes): Data Matrix is reported as unsupported,
// and UPC-A arrives as EAN-13 with a leading zero.
internal val SupportedSymbologies: Map<String?, LoyaltyCardFormat?> = mapOf(
    VNBarcodeSymbologyEAN13 to LoyaltyCardFormat.Ean13,
    VNBarcodeSymbologyEAN8 to LoyaltyCardFormat.Ean8,
    VNBarcodeSymbologyUPCE to LoyaltyCardFormat.UpcE,
    VNBarcodeSymbologyCode128 to LoyaltyCardFormat.Code128,
    VNBarcodeSymbologyCode39 to LoyaltyCardFormat.Code39,
    VNBarcodeSymbologyCode39Checksum to LoyaltyCardFormat.Code39,
    VNBarcodeSymbologyCode39FullASCII to LoyaltyCardFormat.Code39,
    VNBarcodeSymbologyCode39FullASCIIChecksum to LoyaltyCardFormat.Code39,
    VNBarcodeSymbologyCode93 to LoyaltyCardFormat.Code93,
    VNBarcodeSymbologyCode93i to LoyaltyCardFormat.Code93,
    VNBarcodeSymbologyITF14 to LoyaltyCardFormat.Itf,
    VNBarcodeSymbologyI2of5 to LoyaltyCardFormat.Itf,
    VNBarcodeSymbologyI2of5Checksum to LoyaltyCardFormat.Itf,
    VNBarcodeSymbologyCodabar to LoyaltyCardFormat.Codabar,
    VNBarcodeSymbologyQR to LoyaltyCardFormat.QrCode,
    VNBarcodeSymbologyAztec to LoyaltyCardFormat.Aztec,
    VNBarcodeSymbologyPDF417 to LoyaltyCardFormat.Pdf417,
    VNBarcodeSymbologyDataMatrix to null,
)

@Composable
actual fun rememberImageCodePicker(onCodeDecoded: (ScannedCode?) -> Unit): () -> Unit {
    val crashLogger = koinInject<CrashLogger>()
    val currentOnCodeDecoded by rememberUpdatedState(onCodeDecoded)
    // PHPickerViewController holds its delegate weakly, so the composition keeps it alive.
    val delegate = remember(crashLogger) {
        ImagePickerDelegate(crashLogger = crashLogger) { code -> currentOnCodeDecoded(code) }
    }
    return remember(delegate) { { delegate.present() } }
}

private class ImagePickerDelegate(
    private val crashLogger: CrashLogger,
    private val onCodeDecoded: (ScannedCode?) -> Unit,
) : NSObject(),
    PHPickerViewControllerDelegateProtocol {
    fun present() {
        val configuration = PHPickerConfiguration().apply {
            filter = PHPickerFilter.imagesFilter
            selectionLimit = 1
        }
        val picker = PHPickerViewController(configuration = configuration)
        picker.delegate = this
        val presenter = topViewController()
        if (presenter == null) {
            crashLogger.logWarning(message = "Image picker not shown: no view controller to present it from")
            return
        }
        presenter.presentViewController(picker, animated = true, completion = null)
    }

    override fun picker(
        picker: PHPickerViewController,
        didFinishPicking: List<*>,
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        // An empty result means the user cancelled.
        val provider = (didFinishPicking.firstOrNull() as? PHPickerResult)?.itemProvider ?: return
        provider.loadDataRepresentationForTypeIdentifier(UTTypeImage.identifier) { data, error ->
            // Called on a background queue, which is also where Vision should run.
            val code = if (data != null) {
                decodeCode(data)
            } else {
                crashLogger.recordError(
                    context = "Loading the picked image failed",
                    error = IllegalStateException(error?.localizedDescription),
                )
                null
            }
            dispatch_async(dispatch_get_main_queue()) { onCodeDecoded(code) }
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun decodeCode(data: NSData): ScannedCode? {
        val request = VNDetectBarcodesRequest()
        // Barcode detection does not depend on the image orientation, so none is passed.
        val handler = VNImageRequestHandler(data = data, options = emptyMap<Any?, Any?>())
        val isPerformed = memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            handler.performRequests(listOf(request), error = error.ptr).also { success ->
                if (success.not()) {
                    crashLogger.recordError(
                        context = "Reading a code from an image failed",
                        error = IllegalStateException(error.value?.localizedDescription),
                    )
                }
            }
        }
        if (isPerformed.not()) return null
        val codes = request.results
            .orEmpty()
            .filterIsInstance<VNBarcodeObservation>()
            .mapNotNull { observation ->
                observation.payloadStringValue?.let { value ->
                    ScannedCode(value = value, format = SupportedSymbologies[observation.symbology])
                }
            }
        return codes.firstOrNull { it.format != null } ?: codes.firstOrNull()
    }
}

private fun topViewController(): UIViewController? {
    val window = UIApplication.sharedApplication.connectedScenes
        .filterIsInstance<UIWindowScene>()
        .flatMap { scene -> scene.windows.filterIsInstance<UIWindow>() }
        .firstOrNull { it.isKeyWindow() }
    var top = window?.rootViewController
    while (top?.presentedViewController != null) {
        top = top.presentedViewController
    }
    return top
}
