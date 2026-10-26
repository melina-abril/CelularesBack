package com.ventacelulares.modules.countersales.domain;

import com.ventacelulares.shared.domain.Sucursal;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Optional;

public interface CounterSalesRepository {
    record Customer(String dni, String name, String phone, boolean registeredTaxpayer, String cuit) {}
    record Unit(String imei, String model, Sucursal branch, BigDecimal cashPrice, boolean available, boolean launch) {}
    record Approval(String id, String imei, Sucursal branch, BigDecimal percent, boolean approved, boolean rejected) {}
    record Voucher(String code, BigDecimal value, ZonedDateTime expiresAt) {}
    record Sale(String id, String invoiceNumber, String invoiceType, String imei, String dni,
                Sucursal branch, BigDecimal total, BigDecimal commission, ZonedDateTime createdAt) {}

    Optional<Customer> findCustomer(String dni);
    Customer saveCustomer(Customer customer);
    Optional<Unit> findUnit(String imei);
    Optional<Approval> findApproval(String id);
    Approval saveApproval(Approval approval);
    Optional<Voucher> findVoucher(String code);
    Optional<BigDecimal> accessoryPrice(String sku);
    Sale saveSale(Sale sale, BigDecimal discountPercent, String approvalId);
}
