package com.ventacelulares.modules.dashboard.domain;

import com.ventacelulares.shared.domain.Sucursal;
import java.time.LocalDate;

public record InventoryRecord(Sucursal branch, String model, int availableUnits, LocalDate lastSaleDate) {
}
