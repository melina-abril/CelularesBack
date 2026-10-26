package com.ventacelulares.modules.warranties.application.dto;

import java.time.OffsetDateTime;

public record WarrantyCoverageResponse(String imei, String model, String condition,
                                       String invoiceNumber, OffsetDateTime deliveredAt,
                                       OffsetDateTime expiresAt, boolean covered) {
}
