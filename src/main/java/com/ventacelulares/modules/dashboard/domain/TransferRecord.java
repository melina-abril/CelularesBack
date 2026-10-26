package com.ventacelulares.modules.dashboard.domain;

import com.ventacelulares.shared.domain.Sucursal;
import java.time.ZonedDateTime;

public record TransferRecord(String id, String model, String imei, Sucursal origin,
                             Sucursal destination, ZonedDateTime dispatchedAt) {
}
