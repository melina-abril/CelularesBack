package com.ventacelulares.modules.payments.application.dto;

import com.ventacelulares.modules.payments.domain.PaymentMethod;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

public record PaymentOptionResponse(
        PaymentMethod method,
        String bank,
        int installments,
        BigDecimal installmentAmount,
        BigDecimal total,
        BigDecimal adjustmentPercent,
        BigDecimal cftAnnualPercent,
        PaymentOptionStatus status,
        String statusLabel,
        String unavailableReason,
        Set<DayOfWeek> promotionDays,
        LocalDate validFrom,
        LocalDate validTo
) {
}
