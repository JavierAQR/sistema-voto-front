package com.tuusuario.votoelectronico

data class PartidoResponse(
    val id: Int,
    val nombre: String,
    val siglas: String,
    val foto_url: String
)

data class VotoRequest(
    val votante_id: Int,
    val partido_id: Int
)

data class HuellaRequest(
    val votante_id: Int,
    val huella_exitosa: Boolean
)