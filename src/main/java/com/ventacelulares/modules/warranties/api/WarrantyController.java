package com.ventacelulares.modules.warranties.api;

import com.ventacelulares.modules.warranties.application.WarrantyService;
import com.ventacelulares.modules.warranties.application.dto.CreateWarrantyClaimRequest;
import com.ventacelulares.modules.warranties.application.dto.UpdateWarrantyClaimStatusRequest;
import com.ventacelulares.modules.warranties.application.dto.WarrantyClaimResponse;
import com.ventacelulares.modules.warranties.application.dto.WarrantyCoverageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/warranties")
public class WarrantyController {
    private final WarrantyService service;

    public WarrantyController(WarrantyService service) {
        this.service = service;
    }

    @GetMapping("/coverage")
    @PreAuthorize("hasRole('BACKOFFICE')")
    public WarrantyCoverageResponse findWarrantyCoverage(
            @RequestParam(required = false) String imei,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String invoiceNumber) {
        return service.findWarrantyCoverage(imei, dni, invoiceNumber);
    }

    @PostMapping("/claims")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BACKOFFICE')")
    public WarrantyClaimResponse createWarrantyClaim(@Valid @RequestBody CreateWarrantyClaimRequest request) {
        return service.createWarrantyClaim(request);
    }

    @GetMapping("/claims/{claimId}")
    @PreAuthorize("hasRole('BACKOFFICE')")
    public WarrantyClaimResponse getWarrantyClaim(@PathVariable String claimId) {
        return service.getWarrantyClaim(claimId);
    }

    @PatchMapping("/claims/{claimId}/status")
    @PreAuthorize("hasRole('BACKOFFICE')")
    public WarrantyClaimResponse updateWarrantyClaimStatus(@PathVariable String claimId,
            @Valid @RequestBody UpdateWarrantyClaimStatusRequest request) {
        return service.updateWarrantyClaimStatus(claimId, request);
    }
}
