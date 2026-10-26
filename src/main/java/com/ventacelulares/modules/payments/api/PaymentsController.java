package com.ventacelulares.modules.payments.api;

import com.ventacelulares.modules.payments.application.PaymentsService;
import com.ventacelulares.modules.payments.application.dto.PaymentOptionsResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentsController {
    private final PaymentsService service;

    public PaymentsController(PaymentsService service) {
        this.service = service;
    }

    @GetMapping("/variants/{variantId}/options")
    @PreAuthorize("hasAnyRole('CLIENTE', 'VENDEDOR', 'SUPERVISOR', 'BACKOFFICE', 'GERENCIA')")
    public PaymentOptionsResponse getPaymentOptions(
            @PathVariable String variantId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.getPaymentOptions(variantId, date);
    }
}
