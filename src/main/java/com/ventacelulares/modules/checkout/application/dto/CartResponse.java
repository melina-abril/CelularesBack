package com.ventacelulares.modules.checkout.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(String variantId, String model, String color, String memory,
                           BigDecimal cashPrice, List<AccessoryResponse> accessories,
                           List<AccessoryResponse> suggestedAccessories, String voucherCode,
                           BigDecimal subtotal) {
    public record AccessoryResponse(String id, String name, BigDecimal price) {}
}