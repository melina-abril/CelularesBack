package com.ventacelulares.modules.countersales.application;

import com.ventacelulares.modules.countersales.application.dto.CounterSalesDtos.*;
import com.ventacelulares.modules.countersales.domain.CounterSalesRepository;
import com.ventacelulares.modules.countersales.domain.CounterSalesRepository.*;
import com.ventacelulares.shared.domain.Imei;
import com.ventacelulares.shared.domain.Sucursal;
import com.ventacelulares.shared.exception.BusinessRuleException;
import com.ventacelulares.shared.exception.ConflictException;
import com.ventacelulares.shared.exception.NotFoundException;
import com.ventacelulares.shared.security.CurrentUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class CounterSalesService {
    private final CounterSalesRepository repository;
    private final CurrentUser currentUser;
    private final Clock clock;

    public CounterSalesService(CounterSalesRepository repository, CurrentUser currentUser, Clock clock) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    private Sucursal branch() {
        return currentUser.sucursal().orElseThrow(() -> new AccessDeniedException("El usuario no tiene sucursal asignada"));
    }

    private String validImei(String value) {
        try { return new Imei(value).value(); }
        catch (IllegalArgumentException exception) { throw new BusinessRuleException("BR-01", exception.getMessage()); }
    }

    private CustomerResponse customerResponse(Customer customer) {
        return new CustomerResponse(customer.dni(), customer.name(), customer.phone(), customer.registeredTaxpayer(), customer.cuit());
    }

    public CustomerResponse findCustomerByDni(String dni) {
        return customerResponse(repository.findCustomer(dni).orElseThrow(() -> new NotFoundException("Cliente no encontrado")));
    }

    public CustomerResponse createCustomer(CustomerRequest request) {
        if (request.registeredTaxpayer() && (request.cuit() == null || !request.cuit().matches("[0-9]{11}"))) {
            throw new BusinessRuleException("BR-08", "Factura A requiere CUIT de 11 dígitos");
        }
        return customerResponse(repository.saveCustomer(new Customer(request.dni(), request.name(), request.phone(),
                request.registeredTaxpayer(), request.registeredTaxpayer() ? request.cuit() : null)));
    }

    private Unit availableUnit(String imei) {
        Unit unit = repository.findUnit(validImei(imei)).orElseThrow(() -> new NotFoundException("IMEI no encontrado"));
        if (unit.branch() != branch() || !unit.available()) {
            throw new ConflictException("IMEI vendido, reservado o fuera de la sucursal");
        }
        return unit;
    }

    public ImeiResponse verifySaleImei(ImeiRequest request) {
        Unit unit = availableUnit(request.imei());
        return new ImeiResponse(unit.imei(), unit.model(), unit.branch(), unit.cashPrice(), true);
    }

    private ApprovalResponse approvalResponse(Approval approval) {
        return new ApprovalResponse(approval.id(), approval.imei(), approval.branch(), approval.percent(),
                approval.approved(), approval.rejected());
    }

    public ApprovalResponse requestDiscountApproval(ApprovalRequest request) {
        Unit unit = availableUnit(request.imei());
        Approval approval = new Approval(UUID.randomUUID().toString(), unit.imei(), branch(), request.percent(), false, false);
        return approvalResponse(repository.saveApproval(approval));
    }

    public ApprovalResponse resolveDiscountApproval(String id, ApprovalResolution request) {
        Approval approval = repository.findApproval(id).orElseThrow(() -> new NotFoundException("Solicitud no encontrada"));
        if (approval.branch() != branch()) throw new AccessDeniedException("Solicitud de otra sucursal");
        if (approval.approved() || approval.rejected()) throw new ConflictException("Solicitud ya resuelta");
        Approval resolved = new Approval(id, approval.imei(), approval.branch(), approval.percent(),
                request.approved(), !request.approved());
        return approvalResponse(repository.saveApproval(resolved));
    }

    public SaleResponse createCounterSale(SaleRequest request) {
        Unit unit = availableUnit(request.imei());
        Customer customer = repository.findCustomer(request.dni()).orElseThrow(() -> new NotFoundException("Cliente no encontrado"));
        if (customer.registeredTaxpayer() && (customer.cuit() == null || !customer.cuit().matches("[0-9]{11}"))) {
            throw new BusinessRuleException("BR-08", "Factura A requiere CUIT válido");
        }
        List<String> skus = request.accessorySkus() == null ? List.of() : request.accessorySkus();
        if (skus.stream().anyMatch(sku -> sku == null || sku.isBlank())) {
            throw new BusinessRuleException("BR-01", "SKU de accesorio inválido");
        }
        BigDecimal accessories = BigDecimal.ZERO;
        for (String sku : skus) {
            accessories = accessories.add(repository.accessoryPrice(sku)
                    .orElseThrow(() -> new NotFoundException("Accesorio no encontrado: " + sku)));
        }
        BigDecimal discount = unit.cashPrice().multiply(request.discountPercent())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN);
        if (request.discountPercent().compareTo(new BigDecimal("5.00")) > 0) {
            Approval approval = repository.findApproval(request.approvalId() == null ? "" : request.approvalId())
                    .orElseThrow(() -> new ConflictException("Falta aprobación del supervisor"));
            if (!approval.approved() || approval.branch() != branch() || !approval.imei().equals(unit.imei())
                    || approval.percent().compareTo(request.discountPercent()) != 0) {
                throw new ConflictException("Aprobación inválida para la venta");
            }
        }
        BigDecimal tradeIn = BigDecimal.ZERO;
        if (request.voucherCode() != null && !request.voucherCode().isBlank()) {
            Voucher voucher = repository.findVoucher(request.voucherCode())
                    .orElseThrow(() -> new NotFoundException("Vale de canje no encontrado"));
            if (!voucher.expiresAt().isAfter(ZonedDateTime.now(clock))) {
                throw new BusinessRuleException("BR-04", "El vale de canje venció");
            }
            tradeIn = voucher.value();
            if (tradeIn.compareTo(unit.cashPrice().multiply(new BigDecimal("0.40"))) > 0) {
                throw new BusinessRuleException("BR-04", "El canje supera el 40 % del precio contado");
            }
        }
        BigDecimal cashTotal = unit.cashPrice().add(accessories).subtract(discount).subtract(tradeIn).setScale(2, RoundingMode.HALF_EVEN);
        if (cashTotal.signum() < 0) throw new BusinessRuleException("BR-04", "El total no puede ser negativo");
        if (request.paymentMethod() == PaymentMethod.CREDIT) {
            if (request.installments() == null || !List.of(1, 3, 6, 9, 12, 18).contains(request.installments())
                    || request.bank() == null || request.bank().isBlank()) {
                throw new BusinessRuleException("BR-03", "Indique banco y cantidad de cuotas válida");
            }
            if (request.paymentTotal().compareTo(cashTotal) < 0) {
                throw new BusinessRuleException("BR-03", "El total en cuotas no puede ser menor al contado");
            }
        } else if (request.paymentTotal().compareTo(cashTotal) != 0) {
            throw new BusinessRuleException("BR-03", "El total del pago no coincide con el precio contado");
        }
        String id = UUID.randomUUID().toString();
        Sale sale = new Sale(id, "FC-" + id.substring(0, 8).toUpperCase(),
                customer.registeredTaxpayer() ? "A" : "B", unit.imei(), customer.dni(), branch(),
                request.paymentTotal().setScale(2, RoundingMode.HALF_EVEN),
                cashTotal.multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_EVEN), ZonedDateTime.now(clock));
        Sale saved = repository.saveSale(sale, request.discountPercent(), request.approvalId());
        // Respuesta dummy en memoria: reemplazar al conectar facturación y pagos.
        return new SaleResponse(saved.id(), saved.invoiceNumber(), saved.invoiceType(), saved.imei(), saved.dni(),
                saved.branch(), saved.total(), saved.commission(), saved.createdAt());
    }
}
