package com.ventacelulares.modules.warranties.domain;

import java.time.ZonedDateTime;

public record WarrantySale(String imei, String dni, String invoiceNumber, String model,
                           String condition, ZonedDateTime deliveredAt) {
    public int warrantyMonths() {
        return "NUEVO".equals(condition) ? 12 : 6;
    }

    public ZonedDateTime expiresAt() {
        return deliveredAt.plusMonths(warrantyMonths());
    }
}
