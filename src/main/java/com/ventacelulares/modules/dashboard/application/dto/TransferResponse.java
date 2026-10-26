package com.ventacelulares.modules.dashboard.application.dto;

import com.ventacelulares.shared.domain.Sucursal;
import java.time.ZonedDateTime;

public record TransferResponse(String id, String model, String imei, Sucursal origin,
                               Sucursal destination, ZonedDateTime dispatchedAt, String status) {
}
