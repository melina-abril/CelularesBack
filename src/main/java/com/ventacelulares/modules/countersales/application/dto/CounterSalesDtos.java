package com.ventacelulares.modules.countersales.application.dto;

import com.ventacelulares.shared.domain.Sucursal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

public final class CounterSalesDtos {
    private CounterSalesDtos() {}

    public enum PaymentMethod { CASH, TRANSFER, DEBIT, CREDIT }

    public record CustomerRequest(
            @NotBlank @Pattern(regexp = "[0-9]{7,8}") String dni,
            @NotBlank String name,
            @NotBlank String phone,
            boolean registeredTaxpayer,
            String cuit) {}

    public record CustomerResponse(String dni, String name, String phone,
                                   boolean registeredTaxpayer, String cuit) {}

    public record ImeiRequest(@NotBlank String imei) {}
    public record ImeiResponse(String imei, String model, Sucursal branch,
                               BigDecimal cashPrice, boolean available) {}

    public record ApprovalRequest(@NotBlank String imei,
            @NotNull @DecimalMin("5.01") @DecimalMax("100.00") BigDecimal percent) {}
    public record ApprovalResolution(@NotNull Boolean approved) {}
    public record ApprovalResponse(String id, String imei, Sucursal branch,
                                   BigDecimal percent, boolean approved, boolean rejected) {}

    public record SaleRequest(
            @NotBlank String dni,
            @NotBlank String imei,
            List<String> accessorySkus,
            @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal discountPercent,
            String approvalId,
            String voucherCode,
            @NotNull PaymentMethod paymentMethod,
            Integer installments,
            String bank,
            @NotNull @DecimalMin("0.00") BigDecimal paymentTotal) {}

    public record SaleResponse(String id, String invoiceNumber, String invoiceType,
                               String imei, String dni, Sucursal branch, BigDecimal total,
                               BigDecimal commission, ZonedDateTime createdAt) {}
}
