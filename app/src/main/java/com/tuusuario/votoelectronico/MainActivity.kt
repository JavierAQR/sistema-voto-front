package com.tuusuario.votoelectronico

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.safeDrawingPadding
import retrofit2.HttpException

// ── Paleta de colores ──────────────────────────────────────────────────────────
private val AzulPatria   = Color(0xFF0D2E6E)
private val RojoPatria   = Color(0xFFBF0A2E)
private val DoradoAccent = Color(0xFFF0C040)
private val FondoClaro   = Color(0xFFF5F7FB)
private val TextoPrinc   = Color(0xFF0D1B3E)
private val TextoSecund  = Color(0xFF5A6A8A)
private val VerdeOK      = Color(0xFF1B8A5A)
private val BlancoPuro   = Color.White

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary        = AzulPatria,
                    secondary      = RojoPatria,
                    background     = FondoClaro,
                    surface        = BlancoPuro,
                    onPrimary      = BlancoPuro,
                    onSecondary    = BlancoPuro,
                    onBackground   = TextoPrinc,
                    onSurface      = TextoPrinc,
                )
            ) {
                Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(), // ← respeta wifi/notificaciones/barra inferior
                color = FondoClaro
            ) {
                AppNavigation(this)
            }
            }
        }
    }
}

// ── Cabecera reutilizable ──────────────────────────────────────────────────────
@Composable
fun HeaderBanner(titulo: String, subtitulo: String? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(listOf(AzulPatria, Color(0xFF1A4BAA)))
            )
            .padding(vertical = 28.dp, horizontal = 24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.HowToVote, contentDescription = null, tint = DoradoAccent, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(6.dp))
            Text(titulo, color = BlancoPuro, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (subtitulo != null) {
                Spacer(Modifier.height(4.dp))
                Text(subtitulo, color = BlancoPuro.copy(alpha = 0.75f), fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

// ── Navegación principal ───────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(activity: AppCompatActivity) {
    val baseUrl          = "http://192.168.1.36:8000"
    var currentScreen    by remember { mutableStateOf("LOGIN") }
    var fotoUrl          by remember { mutableStateOf("") }
    var votanteId        by remember { mutableStateOf(0) }
    var selectedPartidoId by remember { mutableStateOf(0) }
    var partidos         by remember { mutableStateOf(listOf<PartidoResponse>()) }
    var errorCarga       by remember { mutableStateOf("") }
    var recargarPartidos by remember { mutableStateOf(0) }

    val context       = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    when (currentScreen) {

        // ── LOGIN ──────────────────────────────────────────────────────────────
        "LOGIN" -> LoginScreen(
            activity = activity,
            onDniRegistrado = { url, id ->
                fotoUrl   = url
                votanteId = id
                currentScreen = "FACE"
            }
        )

        // ── CÁMARA ────────────────────────────────────────────────────────────
        "FACE" -> FaceRecognitionScreen(
            fotoOficialUrl = fotoUrl,
            onValidated    = { fotoBase64 ->
                coroutineScope.launch {
                    try {
                        Toast.makeText(context, "Analizando biometría…", Toast.LENGTH_SHORT).show()
                        val resp = RetrofitClient.instance.verificarRostro(
                            RostroRequest(votanteId, true, fotoBase64)
                        )
                        Toast.makeText(context, resp.mensaje, Toast.LENGTH_SHORT).show()
                        showBiometricPrompt(activity,
                            onSuccess = {
                                coroutineScope.launch {
                                    try {
                                        RetrofitClient.instance.verificarHuella(HuellaRequest(votanteId, true))
                                        currentScreen = "SUCCESS"
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error validación final", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onFail = {
                                Toast.makeText(context, "Huella no válida", Toast.LENGTH_LONG).show()
                            }
                        )
                    } catch (e: Exception) {
                        Toast.makeText(context, "Rostro no coincide con DNI", Toast.LENGTH_LONG).show()
                        currentScreen = "LOGIN"
                    }
                }
            }
        )

        // ── CARGANDO PARTIDOS ──────────────────────────────────────────────────
        "SUCCESS" -> {
            // Cambia LaunchedEffect(Unit) por LaunchedEffect(recargarPartidos)
            LaunchedEffect(recargarPartidos) {
                errorCarga = ""
                try {
                    val resultado = RetrofitClient.instance.getPartidos()
                    if (resultado.isEmpty()) {
                        errorCarga = "No hay candidatos registrados en el servidor."
                    } else {
                        partidos = resultado
                        currentScreen = "VOTING"
                    }
                } catch (e: Exception) {
                    errorCarga = "Error de red: ${e.localizedMessage}"
                }
            }

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (errorCarga.isNotEmpty()) {
                        Text("⚠ $errorCarga", color = RojoPatria, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp))
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            // Incrementar la clave fuerza que LaunchedEffect corra de nuevo
                            recargarPartidos++
                        }) {
                            Text("Reintentar")
                        }
                    } else {
                        CircularProgressIndicator(color = AzulPatria)
                        Spacer(Modifier.height(16.dp))
                        Text("Cargando candidatos…", color = TextoSecund)
                    }
                }
            }
        }

        // ── VOTACIÓN ──────────────────────────────────────────────────────────
        "VOTING" -> VotingScreen(
            baseUrl          = baseUrl,
            partidos         = partidos,
            selectedPartidoId = selectedPartidoId,
            onSelectPartido  = { selectedPartidoId = it },
            onConfirmar      = {
                coroutineScope.launch {
                    try {
                        RetrofitClient.instance.votar(VotoRequest(votanteId, selectedPartidoId))
                        currentScreen = "RESULT"
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error al registrar voto", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )

        // ── RESULTADO ─────────────────────────────────────────────────────────
        "RESULT" -> ResultScreen(onSalir = {
            selectedPartidoId = 0
            currentScreen = "LOGIN"
        })
    }
}

// ── PANTALLA DE LOGIN ──────────────────────────────────────────────────────────
@Composable
fun LoginScreen(activity: AppCompatActivity, onDniRegistrado: (String, Int) -> Unit) {
    val context       = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading     by remember { mutableStateOf(false) }
    val scanner       = remember { BarcodeScanning.getClient() }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            isLoading = true
            val image = InputImage.fromFilePath(context, uri)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val raw = barcodes.firstOrNull()?.rawValue ?: ""
                    val dni = Regex("\\d{8}").find(raw)?.value
                    if (dni != null) {
                        Toast.makeText(context, "Procesando identidad…", Toast.LENGTH_SHORT).show()
                        coroutineScope.launch {
                            try {
                                val resp = RetrofitClient.instance.registrarDni(DniRequest(dni))
                                onDniRegistrado(resp.datos_oficiales.foto_oficial_url, resp.votante_id)
                            } catch (e: HttpException) {
                                val msg = when (e.code()) {
                                    403 -> "⛔ Este DNI ya emitió su voto en esta elección."
                                    400 -> "DNI inválido."
                                    else -> "Error del servidor (${e.code()})"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error de red: revisa el servidor", Toast.LENGTH_LONG).show()
                            } finally { isLoading = false }
                        }
                    } else {
                        Toast.makeText(context, "DNI no encontrado en el código", Toast.LENGTH_SHORT).show()
                        isLoading = false
                    }
                }
                .addOnFailureListener { isLoading = false }
        }
    }

    Column(Modifier.fillMaxSize().background(FondoClaro)) {
        HeaderBanner("Sistema de Votación Electrónica", "República del Perú · ONPE")

        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ícono decorativo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(AzulPatria.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, contentDescription = null,
                    tint = AzulPatria, modifier = Modifier.size(56.dp))
            }

            Spacer(Modifier.height(28.dp))

            Text("Identificación del Ciudadano",
                fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = TextoPrinc, textAlign = TextAlign.Center)

            Spacer(Modifier.height(8.dp))

            Text("Escanee el código de barras de su DNI para iniciar el proceso de votación.",
                fontSize = 14.sp, color = TextoSecund, textAlign = TextAlign.Center,
                lineHeight = 20.sp)

            Spacer(Modifier.height(36.dp))

            Button(
                onClick = { galleryLauncher.launch("image/*") },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AzulPatria)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = BlancoPuro, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Procesando…", fontSize = 16.sp)
                } else {
                    Text("📷  Escanear DNI", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(12.dp))

            Text("Solo fotografías nítidas del código de barras posterior del DNI.",
                fontSize = 12.sp, color = TextoSecund, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.weight(1f))

        // Pie de página
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AzulPatria.copy(alpha = 0.06f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Voto Electrónico Seguro · Sistema Biométrico Activo",
                fontSize = 11.sp, color = TextoSecund)
        }
    }
}

// ── PANTALLA DE VOTACIÓN ───────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VotingScreen(
    baseUrl: String,
    partidos: List<PartidoResponse>,
    selectedPartidoId: Int,
    onSelectPartido: (Int) -> Unit,
    onConfirmar: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(FondoClaro)) {
        HeaderBanner("Emita su Voto", "Seleccione un candidato y confirme")

        if (partidos.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No hay candidatos disponibles.", color = TextoSecund)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(partidos) { partido ->
                    val seleccionado = partido.id == selectedPartidoId
                    Card(
                        onClick = { onSelectPartido(partido.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (seleccionado) Modifier.border(2.dp, AzulPatria, RoundedCornerShape(16.dp))
                                else Modifier
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (seleccionado) AzulPatria.copy(alpha = 0.07f) else BlancoPuro
                        ),
                        elevation = CardDefaults.cardElevation(if (seleccionado) 6.dp else 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Logo del partido
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(FondoClaro),
                                contentAlignment = Alignment.Center
                            ) {
                                SubcomposeAsyncImage(
                                    model = "$baseUrl${partido.foto_url}",
                                    contentDescription = partido.nombre,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
                                    loading = {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = AzulPatria,
                                            strokeWidth = 2.dp
                                        )
                                    },
                                    error = {
                                        Icon(Icons.Filled.HowToVote, contentDescription = null,
                                            tint = TextoSecund, modifier = Modifier.size(36.dp))
                                    }
                                )
                            }

                            Spacer(Modifier.width(16.dp))

                            Column(Modifier.weight(1f)) {
                                Text(partido.nombre,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (seleccionado) AzulPatria else TextoPrinc)
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (seleccionado) AzulPatria else TextoSecund.copy(alpha = 0.12f)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(partido.siglas,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (seleccionado) BlancoPuro else TextoSecund)
                                }
                            }

                            if (seleccionado) {
                                Icon(Icons.Filled.CheckCircle,
                                    contentDescription = "Seleccionado",
                                    tint = AzulPatria,
                                    modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }
        }

        // Botón de confirmación
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BlancoPuro)
                .padding(16.dp)
        ) {
            Button(
                onClick = onConfirmar,
                enabled = selectedPartidoId != 0,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RojoPatria,
                    disabledContainerColor = TextoSecund.copy(alpha = 0.3f)
                )
            ) {
                Icon(Icons.Filled.HowToVote, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Confirmar Voto", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── PANTALLA DE RESULTADO ──────────────────────────────────────────────────────
@Composable
fun ResultScreen(onSalir: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AzulPatria, Color(0xFF1A4BAA)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {

            Text("✔", fontSize = (72 * scale).sp, color = DoradoAccent)

            Spacer(Modifier.height(24.dp))

            Text("VOTO REGISTRADO", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                color = BlancoPuro, letterSpacing = 2.sp, textAlign = TextAlign.Center)

            Spacer(Modifier.height(12.dp))

            Text("Su participación ha sido registrada de forma segura y anónima.",
                fontSize = 14.sp, color = BlancoPuro.copy(alpha = 0.8f),
                textAlign = TextAlign.Center, lineHeight = 20.sp)

            Spacer(Modifier.height(48.dp))

            // Separador decorativo
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DoradoAccent)
            )

            Spacer(Modifier.height(48.dp))

            Text("Gracias por ejercer su derecho cívico", fontSize = 13.sp,
                color = BlancoPuro.copy(alpha = 0.7f), textAlign = TextAlign.Center)

            Spacer(Modifier.height(32.dp))

            OutlinedButton(
                onClick = onSalir,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, DoradoAccent),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DoradoAccent)
            ) {
                Text("Finalizar sesión", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── BIOMETRÍA (sin cambios) ────────────────────────────────────────────────────
fun showBiometricPrompt(
    activity: AppCompatActivity,
    onSuccess: () -> Unit,
    onFail: () -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            onSuccess()
        }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            onFail()
        }
        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            onFail()
        }
    })
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Firma Digital del Voto")
        .setSubtitle("Coloque su dedo en el sensor")
        .setNegativeButtonText("Cancelar")
        .build()
    prompt.authenticate(info)
}