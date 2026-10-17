package dev.sk2andy.materialbrowser.browser.transfer

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

/**
 * The camera for the pairing screen's viewfinder slot (V26): the back camera's preview, with
 * frames read for a Vola transfer code until one is found ([onCode], once). The camera runs only
 * while this is on screen and the screen is started. Without the camera permission or a camera,
 * [onNoCamera] is called and nothing opens; asking for the permission is the screen's job.
 */
@Composable
internal fun LocalTransferViewfinder(
    onCode: (String) -> Unit,
    onNoCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCode = rememberUpdatedState(onCode)
    val currentOnNoCamera = rememberUpdatedState(onNoCamera)
    val previewView = remember(context) {
        PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
    }
    AndroidView(factory = { previewView }, modifier = modifier)

    DisposableEffect(lifecycleOwner, previewView) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            currentOnNoCamera.value()
            return@DisposableEffect onDispose {}
        }
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val analysisExecutor = Executors.newSingleThreadExecutor { task -> Thread(task, "vola-transfer-scan") }
        val analyzer = LocalTransferQrAnalyzer { code -> mainExecutor.execute { currentOnCode.value(code) } }
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        var disposed = false
        providerFuture.addListener({
            if (disposed) return@addListener
            val cameraProvider = runCatching { providerFuture.get() }.getOrNull()
            if (cameraProvider == null || !runCatching { cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) }.getOrDefault(false)) {
                currentOnNoCamera.value()
                return@addListener
            }
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(analysisExecutor, analyzer) }
            runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                provider = cameraProvider
            }.onFailure { currentOnNoCamera.value() }
        }, mainExecutor)

        onDispose {
            disposed = true
            provider?.unbindAll()
            analysisExecutor.shutdown()
        }
    }
}
