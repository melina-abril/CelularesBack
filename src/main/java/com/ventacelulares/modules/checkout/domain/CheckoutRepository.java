package com.ventacelulares.modules.checkout.domain;

import com.ventacelulares.shared.domain.Sucursal;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

public interface CheckoutRepository {
    record Accessory(String id, String name, BigDecimal price) {}
    record Product(String variantId, String model, String color, String memory, BigDecimal cashPrice, boolean launch) {}
    record Cart(String variantId, List<String> accessoryIds, String voucherCode) {}
    record Availability(Sucursal branch, int available) {}
    record Reservation(String id, String customer, String variantId, Sucursal branch, ZonedDateTime expiresAt) {}
    record Order(String id, String customer, String variantId, String reservationId, String dni,
                 String invoiceType, String deliveryMethod, Sucursal branch, String postalCode,
                 String paymentMethod, String status, BigDecimal total, ZonedDateTime createdAt,
                 ZonedDateTime transferDeadline) {}

    Product product(String variantId);
    List<Accessory> accessories();
    Accessory accessory(String id);
    Cart cart(String customer);
    Cart updateCart(String customer, Cart cart);
    List<Availability> availability(String variantId, ZonedDateTime now);
    Reservation reserve(String customer, String variantId, Sucursal branch, ZonedDateTime now);
    Reservation reservation(String id, String customer, ZonedDateTime now);
    Order placeOrder(String customer, String reservationId, String dni, String invoiceType,
                     String deliveryMethod, String postalCode, String paymentMethod,
                     BigDecimal total, ZonedDateTime now);
}