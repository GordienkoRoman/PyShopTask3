package com.example.pyshoptask3.ui.FaceDetector

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PointF
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.camera.core.AspectRatio
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.pyshoptask3.R
import com.google.mlkit.vision.face.FaceContour
import kotlin.math.ceil
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3

@SuppressLint("SuspiciousIndentation")
@Composable
fun FaceDetectorScreen(modifier: Modifier = Modifier) {
    val context: Context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val cameraController: LifecycleCameraController =
        remember { LifecycleCameraController(context) }
    var counturs: FaceContour by remember { mutableStateOf(FaceContour(0, listOf(PointF()))) }
    val config = LocalConfiguration.current
    val portraitMode by remember {
        mutableIntStateOf(config.orientation)
    }
    var width by remember {
        mutableIntStateOf(0)
    }
    var height by remember {
        mutableIntStateOf(0)
    }
    var moustacheWidth by remember {
        mutableFloatStateOf(0F)
    }
    val nosePosition by remember {
        mutableStateOf(PointF())
    }

    fun onFaceUpdated(updatedFace: FaceContour,faceWidth:Float,point:PointF) {
        counturs = updatedFace
        moustacheWidth = faceWidth/3
        nosePosition.x = point.x
        nosePosition.y = point.y
    }

    fun onSizeUpdated(imageWidth: Int, imageHeight: Int) {
        if (portraitMode == Configuration.ORIENTATION_PORTRAIT) {
            width = imageHeight
            height = imageWidth
        } else {
            height = imageHeight
            width = imageWidth
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Face detector") }) },
    ) { paddingValues: PaddingValues ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.BottomCenter
        ) {

            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                factory = { context ->
                    PreviewView(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(Color.BLACK)
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_START
                    }.also { previewView ->
                        startFaceDetection(
                            context = context,
                            cameraController = cameraController,
                            lifecycleOwner = lifecycleOwner,
                            previewView = previewView,
                            onDetectedFaceUpdated = ::onFaceUpdated,
                            onSizeUpdated = ::onSizeUpdated
                        )
                    }
                }
            )
            val vector = ImageVector.vectorResource(id = R.drawable.moustache)
            val painter = rememberVectorPainter(image = vector)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                val scaleX = size.width / width
                val scaleY = size.height / height
                val scale = scaleX.coerceAtLeast(scaleY)
                val offsetX = (size.width - ceil(width * scaleX)) / 2.0f
                val offsetY = (size.height - ceil(height * scaleY)) / 2.0f
                val points = counturs.points
                if(moustacheWidth>0F&& nosePosition!=PointF())
                    translate(     //рисуем усы
                        nosePosition.x * scale + offsetX - moustacheWidth / 2,
                        nosePosition.y * scale + offsetX + moustacheWidth/4
                    ) {
                        with(painter)
                        {
                            draw(
                                Size(moustacheWidth, moustacheWidth/2)
                            )
                        }
                    }
                    drawPath(    //рисуем контур
                        path = Path().apply {
                            moveTo(points[0].x * scale + offsetX, points[0].y * scale + offsetY)
                            for (i in 1..<points.size) {
                                lineTo(points[i].x * scale + offsetX, points[i].y * scale + offsetY)
                            }
                            lineTo(points[0].x * scale + offsetX, points[0].y * scale + offsetY)
                        },
                        androidx.compose.ui.graphics.Color.Red,
                        style = Stroke(3.dp.toPx())
                    )
                }
        }
    }
}

private fun startFaceDetection(
    context: Context,
    cameraController: LifecycleCameraController,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onDetectedFaceUpdated: KFunction3<FaceContour, Float, PointF, Unit>,
    onSizeUpdated: KFunction2<Int, Int, Unit>
) {

    cameraController.imageAnalysisTargetSize = CameraController.OutputSize(AspectRatio.RATIO_16_9)
    cameraController.setImageAnalysisAnalyzer(
        ContextCompat.getMainExecutor(context),
        FaceDetectorAnalyzer(
            onDetectedFaceUpdated = onDetectedFaceUpdated,
            onSizeUpdated = onSizeUpdated
        )
    )

    cameraController.bindToLifecycle(lifecycleOwner)
    previewView.controller = cameraController
}
