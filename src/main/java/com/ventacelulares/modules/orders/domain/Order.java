package com.ventacelulares.modules.orders.domain;

import com.ventacelulares.shared.domain.Sucursal;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record Order(
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
        String imei) {

    public Order withStatus(OrderStatus next, String preparedImei, ZonedDateTime deliveryDate) {
        return new Order(id, model, variant, total, branch, deliveryMethod, paymentMethod,
                next, createdAt, deliveryDate, preparedImei);
    }
}
