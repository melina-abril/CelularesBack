package com.ventacelulares.modules.dashboard.application.dto;

import java.math.BigDecimal;

public record TopModelResponse(int rank, String model, int unitsSold, BigDecimal totalSales) {
}
