package com.ventacelulares.modules.tradein.api;

import com.ventacelulares.modules.tradein.application.TradeinService;
import com.ventacelulares.modules.tradein.application.dto.TradeinDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tradein")
public class TradeinController {
    private final TradeinService service;

    public TradeinController(TradeinService service) {
        this.service = service;
    }

    @PostMapping("/quotes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CLIENTE','VENDEDOR','SUPERVISOR')")
    public TradeinDto.VoucherResponse createQuote(@Valid @RequestBody TradeinDto.QuoteRequest request) {
        return service.createQuote(request);
    }

    @GetMapping("/vouchers/{code}")
    @PreAuthorize("hasAnyRole('CLIENTE','VENDEDOR','SUPERVISOR')")
    public TradeinDto.VoucherResponse getVoucher(@PathVariable String code) {
        return service.getVoucher(code);
    }

    @PatchMapping("/vouchers/{code}/quote")
    @PreAuthorize("hasRole('VENDEDOR')")
    public TradeinDto.VoucherResponse updateVerifiedQuote(@PathVariable String code,
            @Valid @RequestBody TradeinDto.UpdateQuoteRequest request) {
        return service.updateVerifiedQuote(code, request);
    }

    @GetMapping("/imei/{imei}/report")
    @PreAuthorize("hasRole('VENDEDOR')")
    public TradeinDto.ImeiReportResponse checkImeiReport(@PathVariable String imei) {
        return service.checkImeiReport(imei);
    }

    @PostMapping("/vouchers/{code}/approvals")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('VENDEDOR')")
    public TradeinDto.ApprovalResponse requestValueApproval(@PathVariable String code,
            @Valid @RequestBody TradeinDto.ApprovalRequest request) {
        return service.requestValueApproval(code, request);
    }

    @GetMapping("/vouchers/{code}/approvals")
    @PreAuthorize("hasAnyRole('VENDEDOR','SUPERVISOR')")
    public TradeinDto.ApprovalResponse getValueApproval(@PathVariable String code) {
        return service.getValueApproval(code);
    }

    @PatchMapping("/approvals/{approvalId}")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public TradeinDto.ApprovalResponse resolveValueApproval(@PathVariable String approvalId,
            @Valid @RequestBody TradeinDto.ResolutionRequest request) {
        return service.resolveValueApproval(approvalId, request);
    }

    @PostMapping("/vouchers/{code}/confirmation")
    @PreAuthorize("hasRole('VENDEDOR')")
    public TradeinDto.VoucherResponse confirmFinalValue(@PathVariable String code,
            @Valid @RequestBody TradeinDto.ConfirmationRequest request) {
        return service.confirmFinalValue(code, request);
    }
}
