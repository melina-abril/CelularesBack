package com.ventacelulares.modules.catalogo.domain;

public interface CorreoAvisoPort {
    ResultadoEnvio enviar(String destinatario, String modelo, Condicion condicion, String color, int memoriaGb);

    enum ResultadoEnvio {
        ACEPTADO, RECHAZADO, ERROR_PROCESAMIENTO
    }
}
