package com.tuusuario.votoelectronico // Mantén tu paquete

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.Response

// --- Modelos de Petición (Lo que enviamos a Python) ---
data class DniRequest(val dni: String)

// ApiService.kt
data class RostroRequest(
    val votante_id: Int,
    val rostro_exitoso: Boolean,
    val foto_base64: String // ¡El nuevo campo vital!
)


// --- Modelos de Respuesta (Lo que recibimos de Python) ---

data class DatosOficiales(
    val dni: String,
    val foto_oficial_url: String,
    val nombre_simulado: String
)

// Este es el modelo clave que arregla tu error
data class DniResponse(
    val mensaje: String,
    val votante_id: Int,
    val datos_oficiales: DatosOficiales
)

// Respuesta genérica para huella y rostro
data class MensajeResponse(
    val mensaje: String
)


// --- Interfaz de Comunicación ---
interface ApiService {

    @GET("/partidos")
    suspend fun getPartidos(): List<PartidoResponse>

    @POST("/voting/cast")
    suspend fun votar(@Body request: VotoRequest): Response<Unit>

    // CORREGIDO: Ahora devuelve DniResponse en lugar de AuthResponse
    @POST("/auth/register-dni")
    suspend fun registrarDni(@Body request: DniRequest): DniResponse

    @POST("/auth/verify-face")
    suspend fun verificarRostro(@Body request: RostroRequest): MensajeResponse

    @POST("/auth/verify-fingerprint")
    suspend fun verificarHuella(@Body request: HuellaRequest): MensajeResponse
}


// --- Cliente Retrofit ---
object RetrofitClient {
    // Tu dirección IPv4 correcta
    private const val BASE_URL = "http://192.168.1.36:8000"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}