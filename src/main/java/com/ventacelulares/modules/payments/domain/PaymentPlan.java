package com.ventacelulares.modules.payments.domain;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

public record PaymentPlan(
        PaymentMethod method,
        String bank,
        int installments,
        BigDecimal adjustmentPercent,
        BigDecimal cftAnnualPercent,
        boolean interestFreePromotion,
        Set<DayOfWeek> promotionDays,
        LocalDate validFrom,
        LocalDate validTo
) {
    public PaymentPlan {
        promotionDays = Set.copyOf(promotionDays);
    }

    public boolean appliesOn(LocalDate date) {
        return !date.isBefore(validFrom)
                && !date.isAfter(validTo)
                && (promotionDays.isEmpty() || promotionDays.contains(date.getDayOfWeek()));
    }
}
