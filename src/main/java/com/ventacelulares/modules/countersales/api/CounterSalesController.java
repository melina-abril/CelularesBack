package com.ventacelulares.modules.countersales.api;

import com.ventacelulares.modules.countersales.application.CounterSalesService;
import com.ventacelulares.modules.countersales.application.dto.CounterSalesDtos.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/counter-sales")
@Validated
public class CounterSalesController {
    private final CounterSalesService service;

    public CounterSalesController(CounterSalesService service) { this.service = service; }

    @GetMapping("/customers")
    @PreAuthorize("hasAnyRole('VENDEDOR','SUPERVISOR')")
    public CustomerResponse findCustomerByDni(@RequestParam @Pattern(regexp = "[0-9]{7,8}") String dni) {
        return service.findCustomerByDni(dni);
    }

    @PostMapping("/customers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('VENDEDOR','SUPERVISOR')")
    public CustomerResponse createCustomer(@Valid @RequestBody CustomerRequest request) {
        return service.createCustomer(request);
    }

    @PostMapping("/imei-verifications")
    @PreAuthorize("hasAnyRole('VENDEDOR','SUPERVISOR')")
    public ImeiResponse verifySaleImei(@Valid @RequestBody ImeiRequest request) {
        return service.verifySaleImei(request);
    }

    @PostMapping("/discount-approvals")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('VENDEDOR')")
    public ApprovalResponse requestDiscountApproval(@Valid @RequestBody ApprovalRequest request) {
        return service.requestDiscountApproval(request);
    }

    @PatchMapping("/discount-approvals/{approvalId}")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ApprovalResponse resolveDiscountApproval(@PathVariable String approvalId,
                                                      @Valid @RequestBody ApprovalResolution request) {
        return service.resolveDiscountApproval(approvalId, request);
    }

    @PostMapping("/sales")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('VENDEDOR','SUPERVISOR')")
    public SaleResponse createCounterSale(@Valid @RequestBody SaleRequest request) {
        return service.createCounterSale(request);
    }
}
