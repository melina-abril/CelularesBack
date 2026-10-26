package com.ventacelulares.modules.checkout.application.dto;

import com.ventacelulares.shared.domain.Sucursal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReserveUnitRequest(@NotBlank String variantId, @NotNull Sucursal branch) {}