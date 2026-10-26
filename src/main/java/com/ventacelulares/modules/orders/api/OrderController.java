package com.ventacelulares.modules.orders.api;

import com.ventacelulares.modules.orders.application.OrderService;
import com.ventacelulares.modules.orders.application.dto.AdvanceOrderStatusRequest;
import com.ventacelulares.modules.orders.application.dto.OrderPageResponse;
import com.ventacelulares.modules.orders.application.dto.OrderResponse;
import com.ventacelulares.modules.orders.domain.OrderStatus;
import com.ventacelulares.modules.orders.domain.PaymentMethod;
import com.ventacelulares.shared.domain.Sucursal;
import jakarta.validation.Valid;
import java.time.ZonedDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VENDEDOR', 'SUPERVISOR', 'BACKOFFICE', 'GERENCIA')")
    public OrderPageResponse listOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Sucursal branch,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listOrders(status, branch, paymentMethod, from, to, page, size);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('VENDEDOR', 'SUPERVISOR', 'BACKOFFICE', 'GERENCIA')")
    public OrderResponse getOrder(@PathVariable String orderId) {
        return service.getOrder(orderId);
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('VENDEDOR', 'SUPERVISOR', 'BACKOFFICE')")
    public ResponseEntity<OrderResponse> advanceOrderStatus(
            @PathVariable String orderId, @Valid @RequestBody AdvanceOrderStatusRequest request) {
        return ResponseEntity.ok(service.advanceOrderStatus(orderId, request));
    }
}
