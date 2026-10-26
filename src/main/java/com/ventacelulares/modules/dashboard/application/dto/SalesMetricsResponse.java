package com.ventacelulares.modules.dashboard.application.dto;

import com.ventacelulares.shared.domain.Sucursal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SalesMetricsResponse(LocalDate from, LocalDate to, int saleCount, BigDecimal totalSales,
                                   BigDecimal averageTicket, BigDecimal tradeInPercentage,
                                   List<Breakdown> byBranch, List<Breakdown> byChannel,
                                   List<Breakdown> byPaymentMethod) {
    public record Breakdown(String name, int saleCount, BigDecimal totalSales) {
    }
}
