package com.ventacelulares.modules.checkout.application.dto;

import com.ventacelulares.shared.domain.Sucursal;
import java.math.BigDecimal;
import java.util.List;

public record DeliveryOptionsResponse(List<PickupOption> pickupBranches, ShippingOption shipping) {
    public record PickupOption(Sucursal branch, String name) {}
    public record ShippingOption(String postalCode, boolean available, BigDecimal cost) {}
}