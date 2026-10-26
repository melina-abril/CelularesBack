package com.ventacelulares.modules.orders.application.dto;

import com.ventacelulares.modules.orders.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record AdvanceOrderStatusRequest(@NotNull OrderStatus status, String imei) {
}
