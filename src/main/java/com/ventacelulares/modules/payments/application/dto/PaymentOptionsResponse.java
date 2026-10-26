package com.ventacelulares.modules.payments.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PaymentOptionsResponse(
        String variantId,
        String model,
        int memoryGb,
        String color,
        String condition,
        BigDecimal listPrice,
        String currency,
        LocalDate queryDate,
        int interestFreePromotions,
        List<PaymentOptionResponse> options
) {
}
