package com.ventacelulares.modules.checkout.api;

import com.ventacelulares.modules.checkout.application.CheckoutService;
import com.ventacelulares.modules.checkout.application.dto.CartResponse;
import com.ventacelulares.modules.checkout.application.dto.DeliveryOptionsResponse;
import com.ventacelulares.modules.checkout.application.dto.OnlineOrderResponse;
import com.ventacelulares.modules.checkout.application.dto.PlaceOrderRequest;
import com.ventacelulares.modules.checkout.application.dto.ReservationResponse;
import com.ventacelulares.modules.checkout.application.dto.ReserveUnitRequest;
import com.ventacelulares.modules.checkout.application.dto.UpdateCartRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkout")
public class CheckoutController {
    private final CheckoutService service;

    public CheckoutController(CheckoutService service) {
        this.service = service;
    }

    @GetMapping("/cart")
    @PreAuthorize("hasRole('CLIENTE')")
    public CartResponse getCart() {
        return service.getCart();
    }

    @PutMapping("/cart")
    @PreAuthorize("hasRole('CLIENTE')")
    public CartResponse updateCart(@Valid @RequestBody UpdateCartRequest request) {
        return service.updateCart(request);
    }

    @GetMapping("/delivery-options")
    @PreAuthorize("hasRole('CLIENTE')")
    public DeliveryOptionsResponse getDeliveryOptions(@RequestParam(required = false) String postalCode) {
        return service.getDeliveryOptions(postalCode);
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CLIENTE')")
    public ReservationResponse reserveUnit(@Valid @RequestBody ReserveUnitRequest request) {
        return service.reserveUnit(request);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CLIENTE')")
    public OnlineOrderResponse placeOnlineOrder(@Valid @RequestBody PlaceOrderRequest request) {
        return service.placeOnlineOrder(request);
    }
}