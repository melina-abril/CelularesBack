package com.ventacelulares.modules.catalogo.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CrearAvisoRequest(@NotBlank String modeloId, @NotBlank String sku, @NotBlank @Email String correo) {
}
