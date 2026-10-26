package com.ventacelulares.modules.payments.domain;

public record PaymentVariant(
        String id,
        String model,
        int memoryGb,
        String color,
        String condition
) {
}
