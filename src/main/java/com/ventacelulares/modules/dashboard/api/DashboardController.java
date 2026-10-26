package com.ventacelulares.modules.dashboard.api;

import com.ventacelulares.modules.dashboard.application.DashboardService;
import com.ventacelulares.modules.dashboard.application.dto.CriticalStockResponse;
import com.ventacelulares.modules.dashboard.application.dto.PagedResponse;
import com.ventacelulares.modules.dashboard.application.dto.SalesMetricsResponse;
import com.ventacelulares.modules.dashboard.application.dto.TopModelResponse;
import com.ventacelulares.modules.dashboard.application.dto.TransferResponse;
import com.ventacelulares.shared.domain.Sucursal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/sales")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'GERENCIA')")
    public SalesMetricsResponse getSalesMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Sucursal branch) {
        return service.getSalesMetrics(from, to, branch);
    }

    @GetMapping("/top-models")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'GERENCIA')")
    public PagedResponse<TopModelResponse> getTopModels(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Sucursal branch,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return service.getTopModels(from, to, branch, page, size);
    }

    @GetMapping("/critical-stock")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'GERENCIA')")
    public PagedResponse<CriticalStockResponse> getCriticalStock(
            @RequestParam(required = false) Sucursal branch,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return service.getCriticalStock(branch, page, size);
    }

    @GetMapping("/transfers")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'GERENCIA')")
    public PagedResponse<TransferResponse> getTransfersInTransit(
            @RequestParam(required = false) Sucursal branch,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return service.getTransfersInTransit(branch, page, size);
    }
}
