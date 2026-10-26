package com.ventacelulares.modules.orders.application.dto;

import com.ventacelulares.modules.orders.domain.DeliveryMethod;
import com.ventacelulares.modules.orders.domain.OrderStatus;
import com.ventacelulares.modules.orders.domain.PaymentMethod;
import com.ventacelulares.shared.domain.Sucursal;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

public record OrderResponse(
        String id,
        String model,
        String variant,
        BigDecimal total,
        Sucursal branch,
        DeliveryMethod deliveryMethod,
        PaymentMethod paymentMethod,
        OrderStatus status,
        ZonedDateTime createdAt,
        ZonedDateTime deliveredAt,
        String imei,
        ZonedDateTime transferExpiresAt,
        boolean transferExpiringSoon,
        ZonedDateTime returnDeadline,
        boolean withinReturnPeriod,
        List<OrderStatus> nextStatuses) {
}
