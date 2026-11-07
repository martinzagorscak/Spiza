package com.mzagorscak.spiza.ui.screens.loyaltyCards.add

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.mzagorscak.spiza.auth.CurrentActivityProvider
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.crash.recordError
import com.mzagorscak.spiza.permissions.findComponentActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
actual fun rememberImageCodePicker(onCodeDecoded: (ScannedCode?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val activityProvider = koinInject<CurrentActivityProvider>()
    // Inside NavDisplay the registry owner is not provided and the context can be a wrapper, so the activity is
    // located the same way as for the permissions controller (see rememberPlatformPermissionsController).
    val registryOwner = LocalActivityResultRegistryOwner.current
        ?: remember(context, activityProvider) {
            context.findComponentActivity() ?: (activityProvider.current as? ComponentActivity)
        }
    if (registryOwner == null) {
        val crashLogger = koinInject<CrashLogger>()
        return remember(crashLogger) {
            { crashLogger.logWarning(message = "Image picker not shown: no activity to launch it from") }
        }
    }
    var pickImage: () -> Unit = {}
    CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) {
        pickImage = rememberPhotoPickerLauncher(onCodeDecoded = onCodeDecoded)
    }
    return pickImage
}

@Composable
private fun rememberPhotoPickerLauncher(onCodeDecoded: (ScannedCode?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val crashLogger = koinInject<CrashLogger>()
    val scope = rememberCoroutineScope()
    val currentOnCodeDecoded by rememberUpdatedState(onCodeDecoded)
    // The Photo Picker (or ACTION_OPEN_DOCUMENT on devices without it) grants access to the chosen image only,
    // so READ_MEDIA_IMAGES is not needed — Play policy only allows it for apps whose core feature is broad photo access.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            currentOnCodeDecoded(decodeCode(context = context, uri = uri, crashLogger = crashLogger))
        }
    }
    return remember(launcher) {
        {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}

private suspend fun decodeCode(
    context: Context,
    uri: Uri,
    crashLogger: CrashLogger,
): ScannedCode? {
    val scanner = BarcodeScanning.getClient()
    return try {
        // fromFilePath applies the EXIF rotation, so photos taken sideways are read correctly.
        val image = withContext(Dispatchers.IO) { InputImage.fromFilePath(context, uri) }
        val codes = scanner.process(image).await().mapNotNull { it.toScannedCode() }
        codes.firstOrNull { it.format != null } ?: codes.firstOrNull()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        crashLogger.recordError(context = "Reading a code from an image failed", error = error)
        null
    } finally {
        scanner.close()
    }
}

private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            when {
                task.isCanceled -> continuation.cancel()
                task.isSuccessful -> continuation.resume(task.result)
                else -> continuation.resumeWithException(task.exception ?: IllegalStateException("Task failed without an exception"))
            }
        }
    }
