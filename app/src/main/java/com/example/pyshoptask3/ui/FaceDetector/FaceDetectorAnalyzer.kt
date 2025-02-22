package com.example.pyshoptask3.ui.FaceDetector

import android.graphics.PointF
import android.media.Image
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs
import kotlin.reflect.KFunction3

class FaceDetectorAnalyzer(
    private val   onDetectedFaceUpdated: KFunction3<FaceContour, Float, PointF, Unit>,
    val onSizeUpdated: (Int, Int) -> Unit,
) : ImageAnalysis.Analyzer {

    companion object {
        const val THROTTLE_TIMEOUT_MS = 1_000L
    }

    private val detectorOptions: FaceDetectorOptions? = null

    private val options = detectorOptions
        ?: FaceDetectorOptions.Builder()
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .enableTracking()
            .build()
    private val detector = FaceDetection.getClient(options)
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        scope.launch {
            val mediaImage: Image = imageProxy.image ?: run { imageProxy.close(); return@launch }
            val inputImage: InputImage =
                InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )
            suspendCoroutine { continuation ->
                detector.process(inputImage)
                    .addOnSuccessListener { faces ->
                        faces.forEach { face ->
                           // val degree = face.headEulerAngleZ// не смог разобраться как отображать корректно при ротации, времени много надо...
                            val lEar = face.getLandmark(FaceLandmark.LEFT_EAR)
                            val rEar = face.getLandmark(FaceLandmark.RIGHT_EAR)
                            var faceWidth = 0F
                            if (lEar!=null&&rEar!=null)
                                faceWidth = abs(lEar.position.x - rEar.position.x)
                            onSizeUpdated(inputImage.width, inputImage.height)
                            val detectedFace = face.getContour(FaceContour.FACE)?: return@forEach
                            val noseLandmark = face.getLandmark(FaceLandmark.NOSE_BASE)
                            onDetectedFaceUpdated(detectedFace,faceWidth,noseLandmark?.position?: PointF())
                        }
                    }
                    .addOnCompleteListener {
                        continuation.resume(Unit)
                    }
            }

            delay(THROTTLE_TIMEOUT_MS)
        }.invokeOnCompletion { exception ->
            exception?.printStackTrace()
            imageProxy.close()
        }
    }
}