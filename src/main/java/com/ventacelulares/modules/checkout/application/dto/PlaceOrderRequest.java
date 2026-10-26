package com.ventacelulares.modules.checkout.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PlaceOrderRequest(
        @NotBlank String reservationId,
        @NotBlank String fullName,
        @NotBlank @Pattern(regexp = "[0-9]{7,8}") String dni,
        @NotBlank @Email String email,
        @NotBlank String invoiceType,
        String cuit,
        @NotBlank String deliveryMethod,
        String address,
        String postalCode,
        @NotBlank String paymentMethod,
        String paymentToken,
        @NotNull Integer installments) {}