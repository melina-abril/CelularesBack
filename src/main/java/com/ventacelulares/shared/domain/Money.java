package com.ventacelulares.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount, String currency) {
    public Money {
        amount = Objects.requireNonNull(amount, "El importe es obligatorio").setScale(2, RoundingMode.HALF_EVEN);
        currency = currency == null || currency.isBlank() ? "ARS" : currency;
    }

    public Money(BigDecimal amount) {
        this(amount, "ARS");
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money percentage(BigDecimal percent) {
        Objects.requireNonNull(percent, "El porcentaje es obligatorio");
        return new Money(amount.multiply(percent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN), currency);
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "El importe es obligatorio");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("No se pueden operar importes de distintas monedas");
        }
    }
}
