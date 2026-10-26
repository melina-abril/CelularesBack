package com.ventacelulares.modules.checkout.application.dto;

import com.ventacelulares.shared.domain.Sucursal;
import java.time.ZonedDateTime;

public record ReservationResponse(String id, String variantId, Sucursal branch, ZonedDateTime expiresAt) {}