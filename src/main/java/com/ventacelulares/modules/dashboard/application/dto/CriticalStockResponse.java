package com.ventacelulares.modules.dashboard.application.dto;

import com.ventacelulares.shared.domain.Sucursal;
import java.time.LocalDate;

public record CriticalStockResponse(Sucursal branch, String model, int availableUnits, LocalDate lastSaleDate) {
}
