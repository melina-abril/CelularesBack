package com.ventacelulares.modules.tradein.application.dto;

import com.ventacelulares.modules.tradein.domain.TradeinVoucher;
import com.ventacelulares.shared.domain.Sucursal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

public final class TradeinDto {
    private TradeinDto() {}

    public record DeviceRequest(@NotBlank String brand, @NotBlank String model, @Positive int memoryGb) {
        public TradeinVoucher.Device toDomain() { return new TradeinVoucher.Device(brand, model, memoryGb); }
    }

    public record ChecklistRequest(@NotNull Boolean powersOn, @NotNull Boolean screenIntact,
                                   @NotNull Integer batteryPercent, @NotNull Boolean accountUnlocked,
                                   @NotNull Boolean boxAndCharger) {
        public TradeinVoucher.Checklist toDomain() {
            return new TradeinVoucher.Checklist(powersOn, screenIntact, batteryPercent, accountUnlocked, boxAndCharger);
        }
    }

    public record QuoteRequest(@NotNull Sucursal branch, @NotNull @Valid DeviceRequest device,
                               @NotNull @Valid ChecklistRequest checklist) {}
    public record UpdateQuoteRequest(@NotNull @Valid DeviceRequest device, @NotNull @Valid ChecklistRequest checklist) {}
    public record ApprovalRequest(@NotBlank String imei, @NotNull @DecimalMin("0.01") BigDecimal proposedValue,
                                  @NotBlank String reason) {}
    public record ResolutionRequest(@NotNull Boolean approved) {}
    public record ConfirmationRequest(@NotBlank String imei, @NotNull @DecimalMin("0.01") BigDecimal proposedValue) {}

    public record VoucherResponse(String code, Sucursal branch, TradeinVoucher.Device declaredDevice,
                                  TradeinVoucher.Checklist declaredChecklist, TradeinVoucher.Device verifiedDevice,
                                  TradeinVoucher.Checklist verifiedChecklist, BigDecimal estimate,
                                  ZonedDateTime issuedAt, ZonedDateTime expiresAt,
                                  TradeinVoucher.Status status, String confirmedImei, BigDecimal finalValue) {}
    public enum ReportStatus { CLEAR, REPORTED, INVALID, UNDETERMINED }
    public record ImeiReportResponse(String imei, ReportStatus status) {}
    public record ApprovalResponse(String id, String code, String seller, Sucursal branch, String imei,
                                   BigDecimal estimate, BigDecimal proposed, BigDecimal deviationPercent,
                                   String reason, Integer batteryPercent, String status) {}
}
