package com.ventacelulares.modules.catalogo.application.dto;

import com.ventacelulares.shared.domain.Sucursal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IngresoStockRequest(@NotBlank String sku, @NotNull Sucursal sucursal, @Min(1) int cantidad) {
}
