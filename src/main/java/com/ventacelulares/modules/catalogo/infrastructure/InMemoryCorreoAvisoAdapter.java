package com.ventacelulares.modules.catalogo.infrastructure;

import com.ventacelulares.modules.catalogo.domain.Condicion;
import com.ventacelulares.modules.catalogo.domain.CorreoAvisoPort;
import org.springframework.stereotype.Component;

@Component
public class InMemoryCorreoAvisoAdapter implements CorreoAvisoPort {
    @Override
    public ResultadoEnvio enviar(String destinatario, String modelo, Condicion condicion, String color, int memoriaGb) {
        // Respuesta dummy: reemplazar por el servicio de correo al conectarlo.
        if (destinatario == null || !destinatario.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
                || modelo == null || modelo.isBlank() || condicion == null || color == null || color.isBlank() || memoriaGb <= 0) {
            return ResultadoEnvio.RECHAZADO;
        }
        return ResultadoEnvio.ACEPTADO;
    }
}
