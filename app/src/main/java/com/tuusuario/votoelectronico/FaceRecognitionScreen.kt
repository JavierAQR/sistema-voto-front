package com.tuusuario.votoelectronico // Tu paquete

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background // Agregado para el fondo oscuro
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.ByteArrayOutputStream

// Función auxiliar de ingeniería para convertir el frame a Base64
fun convertirFrameABase64(imageProxy: ImageProxy): String {
    val bitmap = imageProxy.toBitmap()
    val outputStream = ByteArrayOutputStream()
    // Comprimimos al 70% en JPEG para envío rápido por red
    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
    val byteArray = outputStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.NO_WRAP)
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun FaceRecognitionScreen(
    fotoOficialUrl: String,
    onValidated: (String) -> Unit // AHORA EXPORTAMOS UN STRING (La foto en Base64)
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val baseUrl = "http://192.168.1.36:8000"

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Se requiere permiso de cámara para la validación facial.")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text("Otorgar Permiso")
            }
        }
        return
    }

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            // Cambiamos FRONT por BACK para que el emulador no colapse
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        }
    }

    val faceDetector = remember {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
        FaceDetection.getClient(options)
    }

    // --- VARIABLES DE ESTADO DE INGENIERÍA PARA EL PESTAÑEO ---
    var isValidated by remember { mutableStateOf(false) }
    var isBlinkStarted by remember { mutableStateOf(false) } // NUEVO: ¿Ya cerró los ojos?

    // Agregamos background(Color.Black) para aislar la vista y resaltar textos
    Box(
    modifier = Modifier
        .fillMaxSize()
        .background(Color.Black),
    contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    controller = cameraController
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            },
            update = { previewView ->
                // Vinculamos la cámara aquí, cuando la vista ya existe
                cameraController.bindToLifecycle(lifecycleOwner)
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
            // verticalArrangement = Arrangement.Top
        ) {
            Text("Verificación de Identidad", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(modifier = Modifier.height(20.dp))
            val timestamp = System.currentTimeMillis()
            AsyncImage(
                model = "$baseUrl$fotoOficialUrl?v=$timestamp",
                contentDescription = "Foto RENIEC",
                modifier = Modifier.size(140.dp).clip(CircleShape).border(3.dp, Color.Green, CircleShape)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text("Foto oficial de referencia", style = MaterialTheme.typography.bodySmall, color = Color.Green)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (isValidated) "¡Identidad Confirmada!" else "Pestañee para confirmar que es usted",
                style = MaterialTheme.typography.titleLarge,
                color = if (isValidated) Color.Green else Color.White,
                modifier = Modifier.padding(bottom = 80.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

           Button(
                onClick = {
                    cameraController.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                val base64 = convertirFrameABase64(imageProxy)
                                onValidated(base64)
                                imageProxy.close()
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e("FaceRecognition", "Error al capturar imagen: ${exception.message}")
                            }
                        }
                    )
                }
            ) {
                Text("Validar identidad")
            }

        }
    }

}