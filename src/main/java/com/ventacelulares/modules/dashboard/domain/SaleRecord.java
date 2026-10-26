package com.ventacelulares.modules.dashboard.domain;

import com.ventacelulares.shared.domain.Money;
import com.ventacelulares.shared.domain.Sucursal;
import java.time.LocalDate;

public record SaleRecord(LocalDate date, Sucursal branch, String channel, String paymentMethod,
                         String model, Money amount, boolean tradeIn) {
}
