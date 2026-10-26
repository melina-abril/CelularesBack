package com.ventacelulares.modules.checkout.application.dto;

import com.ventacelulares.shared.domain.Sucursal;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record OnlineOrderResponse(String id, String variantId, String status,
                                  String invoiceType, String deliveryMethod, Sucursal branch,
                                  String paymentMethod, BigDecimal devicePrice,
                                  BigDecimal accessoriesTotal, BigDecimal shippingCost,
                                  BigDecimal tradeInDiscount, BigDecimal total,
                                  ZonedDateTime createdAt, ZonedDateTime transferDeadline) {}