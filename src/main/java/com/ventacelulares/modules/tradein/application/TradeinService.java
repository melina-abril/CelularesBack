package com.ventacelulares.modules.tradein.application;

import com.ventacelulares.modules.tradein.application.dto.TradeinDto;
import com.ventacelulares.modules.tradein.domain.TradeinRepository;
import com.ventacelulares.modules.tradein.domain.TradeinVoucher;
import com.ventacelulares.shared.domain.Imei;
import com.ventacelulares.shared.domain.Sucursal;
import com.ventacelulares.shared.exception.BusinessRuleException;
import com.ventacelulares.shared.exception.ConflictException;
import com.ventacelulares.shared.exception.NotFoundException;
import com.ventacelulares.shared.security.CurrentUser;
import com.ventacelulares.shared.security.Roles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class TradeinService {
    private static final BigDecimal TEN = new BigDecimal("10.0");
    private static final Map<String, BigDecimal> BASE_VALUES = Map.of(
            "Samsung|Galaxy S23|256", new BigDecimal("310000.00"),
            "Motorola|Edge 40|256", new BigDecimal("175000.00"),
            "Apple|iPhone 13|128", new BigDecimal("350000.00"),
            "Xiaomi|Redmi Note 12|128", new BigDecimal("120000.00"));
    private final TradeinRepository repository;
    private final Clock clock;
    private final CurrentUser currentUser;

    public TradeinService(TradeinRepository repository, Clock clock, CurrentUser currentUser) {
        this.repository = repository;
        this.clock = clock;
        this.currentUser = currentUser;
    }

    public TradeinDto.VoucherResponse createQuote(TradeinDto.QuoteRequest request) {
        TradeinVoucher.Device device = request.device().toDomain();
        TradeinVoucher.Checklist checklist = request.checklist().toDomain();
        BigDecimal estimate = calculate(device, checklist);
        // Respuesta dummy respaldada por el adaptador en memoria; reemplazar por persistencia real.
        TradeinVoucher voucher = new TradeinVoucher("CANJE-" + UUID.randomUUID(), request.branch(), device,
                checklist, estimate, ZonedDateTime.now(clock));
        repository.save(voucher);
        return response(voucher);
    }

    public TradeinDto.VoucherResponse getVoucher(String code) {
        TradeinVoucher voucher = find(code);
        if (Roles.VENDEDOR.equals(currentUser.role()) || Roles.SUPERVISOR.equals(currentUser.role())) {
            requireBranch(voucher);
        }
        return response(voucher);
    }

    public TradeinDto.VoucherResponse updateVerifiedQuote(String code, TradeinDto.UpdateQuoteRequest request) {
        TradeinVoucher voucher = findForSeller(code);
        synchronized (voucher) {
            requireAvailable(voucher);
            TradeinVoucher.Device device = request.device().toDomain();
            TradeinVoucher.Checklist checklist = request.checklist().toDomain();
            BigDecimal estimate = calculate(device, checklist);
            voucher.update(device, checklist, estimate);
            repository.save(voucher);
            return response(voucher);
        }
    }

    public TradeinDto.ImeiReportResponse checkImeiReport(String imei) {
        // Consulta dummy: reemplazar por el servicio de denuncias; no se envían datos del cliente.
        if (imei == null || !imei.matches("[0-9]{15}")) {
            return new TradeinDto.ImeiReportResponse(imei, TradeinDto.ReportStatus.INVALID);
        }
        try {
            new Imei(imei);
        } catch (IllegalArgumentException exception) {
            return new TradeinDto.ImeiReportResponse(imei, TradeinDto.ReportStatus.INVALID);
        }
        return new TradeinDto.ImeiReportResponse(imei,
                "356938035643809".equals(imei) ? TradeinDto.ReportStatus.REPORTED : TradeinDto.ReportStatus.CLEAR);
    }

    public TradeinDto.ApprovalResponse requestValueApproval(String code, TradeinDto.ApprovalRequest request) {
        TradeinVoucher voucher = findForSeller(code);
        synchronized (voucher) {
            requireAvailable(voucher);
            requireClearImei(request.imei());
            requirePositive(request.proposedValue());
            BigDecimal deviation = deviation(voucher.estimate(), request.proposedValue());
            if (deviation.abs().compareTo(TEN) <= 0) {
                throw new BusinessRuleException("BR-04", "Este valor no requiere aprobación");
            }
            TradeinVoucher.Approval approval = new TradeinVoucher.Approval(UUID.randomUUID().toString(),
                    currentUser.name(), voucher.branch(), request.imei(), voucher.estimate(),
                    request.proposedValue().setScale(2, RoundingMode.HALF_EVEN), deviation,
                    request.reason(), voucher.currentChecklist().batteryPercent(), TradeinVoucher.ApprovalStatus.PENDING);
            voucher.setApproval(approval);
            repository.save(voucher);
            return approvalResponse(voucher);
        }
    }

    public TradeinDto.ApprovalResponse getValueApproval(String code) {
        TradeinVoucher voucher = find(code);
        requireBranch(voucher);
        return approvalResponse(voucher);
    }

    public TradeinDto.ApprovalResponse resolveValueApproval(String approvalId, TradeinDto.ResolutionRequest request) {
        // Los datos dummy se recorren a través de los vales conocidos por este adaptador temporal.
        TradeinVoucher voucher = repository.findByCode("CANJE-DEMO-001")
                .filter(value -> value.approval() != null && value.approval().id().equals(approvalId))
                .orElseThrow(() -> new NotFoundException("Solicitud de aprobación inexistente"));
        requireBranch(voucher);
        synchronized (voucher) {
            requireAvailable(voucher);
            TradeinVoucher.Approval old = voucher.approval();
            if (old == null || !old.id().equals(approvalId)) {
                throw new NotFoundException("Solicitud de aprobación inexistente");
            }
            if (old.status() != TradeinVoucher.ApprovalStatus.PENDING) {
                throw new ConflictException("La solicitud ya fue resuelta");
            }
            voucher.setApproval(new TradeinVoucher.Approval(old.id(), old.seller(), old.branch(), old.imei(),
                    old.estimate(), old.proposed(), old.deviationPercent(), old.reason(), old.batteryPercent(),
                    request.approved() ? TradeinVoucher.ApprovalStatus.APPROVED : TradeinVoucher.ApprovalStatus.REJECTED));
            repository.save(voucher);
            return approvalResponse(voucher);
        }
    }

    public TradeinDto.VoucherResponse confirmFinalValue(String code, TradeinDto.ConfirmationRequest request) {
        TradeinVoucher voucher = findForSeller(code);
        synchronized (voucher) {
            requireAvailable(voucher);
            calculate(voucher.currentDevice(), voucher.currentChecklist());
            requireClearImei(request.imei());
            requirePositive(request.proposedValue());
            BigDecimal proposed = request.proposedValue().setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal difference = deviation(voucher.estimate(), proposed);
            if (difference.abs().compareTo(TEN) > 0) {
                TradeinVoucher.Approval approval = voucher.approval();
                if (approval != null && approval.status() == TradeinVoucher.ApprovalStatus.REJECTED
                        && approval.imei().equals(request.imei()) && approval.proposed().compareTo(proposed) == 0) {
                    throw new BusinessRuleException("BR-04", "El supervisor rechazó el valor propuesto");
                }
                if (approval == null || approval.status() != TradeinVoucher.ApprovalStatus.APPROVED
                        || !approval.imei().equals(request.imei()) || approval.proposed().compareTo(proposed) != 0
                        || approval.estimate().compareTo(voucher.estimate()) != 0) {
                    throw new BusinessRuleException("BR-04", "Se requiere aprobación del supervisor para este valor e IMEI");
                }
            }
            voucher.confirm(request.imei(), proposed);
            repository.save(voucher);
            return response(voucher);
        }
    }

    private BigDecimal calculate(TradeinVoucher.Device device, TradeinVoucher.Checklist checklist) {
        BigDecimal base = BASE_VALUES.get(device.brand() + "|" + device.model() + "|" + device.memoryGb());
        if (base == null) {
            throw new BusinessRuleException("BR-04", "Seleccioná un equipo válido");
        }
        if (!checklist.screenIntact()) {
            throw new BusinessRuleException("BR-04", "Canje rechazado: pantalla rota");
        }
        if (!checklist.accountUnlocked()) {
            throw new BusinessRuleException("BR-04", "Canje rechazado: bloqueo de cuenta");
        }
        if (checklist.batteryPercent() < 0 || checklist.batteryPercent() > 100) {
            throw new BusinessRuleException("BR-04", "Ingresá un porcentaje de batería válido");
        }
        if (!checklist.powersOn()) base = base.multiply(new BigDecimal("0.70"));
        if (checklist.batteryPercent() <= 80) base = base.multiply(new BigDecimal("0.85"));
        if (!checklist.boxAndCharger()) base = base.multiply(new BigDecimal("0.95"));
        return base.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal deviation(BigDecimal estimate, BigDecimal proposed) {
        return proposed.subtract(estimate).multiply(new BigDecimal("100"))
                .divide(estimate, 1, RoundingMode.HALF_EVEN);
    }

    private void requirePositive(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            throw new BusinessRuleException("BR-04", "Ingresá un valor propuesto mayor que cero");
        }
    }

    private void requireClearImei(String imei) {
        TradeinDto.ReportStatus status = checkImeiReport(imei).status();
        if (status == TradeinDto.ReportStatus.REPORTED) {
            throw new BusinessRuleException("BR-04", "Canje rechazado: equipo denunciado");
        }
        if (status != TradeinDto.ReportStatus.CLEAR) {
            throw new BusinessRuleException("BR-04", "Ingresá un IMEI válido");
        }
    }

    private TradeinVoucher find(String code) {
        return repository.findByCode(code).orElseThrow(() -> new NotFoundException("Vale inexistente"));
    }

    private TradeinVoucher findForSeller(String code) {
        TradeinVoucher voucher = find(code);
        requireBranch(voucher);
        return voucher;
    }

    private void requireBranch(TradeinVoucher voucher) {
        Sucursal branch = currentUser.sucursal().orElseThrow(() -> new AccessDeniedException("Se requiere una sucursal asignada"));
        if (branch != voucher.branch()) {
            throw new AccessDeniedException("No puede operar sobre otra sucursal");
        }
    }

    private void requireAvailable(TradeinVoucher voucher) {
        if (voucher.status() != TradeinVoucher.Status.AVAILABLE) {
            throw new ConflictException("El vale ya fue utilizado");
        }
        if (!ZonedDateTime.now(clock).isBefore(voucher.expiresAt())) {
            throw new BusinessRuleException("BR-04", "El vale está vencido");
        }
    }

    private TradeinDto.VoucherResponse response(TradeinVoucher voucher) {
        return new TradeinDto.VoucherResponse(voucher.code(), voucher.branch(), voucher.declaredDevice(),
                voucher.declaredChecklist(), voucher.verifiedDevice(), voucher.verifiedChecklist(),
                voucher.estimate(), voucher.issuedAt(), voucher.expiresAt(), voucher.status(),
                voucher.confirmedImei(), voucher.finalValue());
    }

    private TradeinDto.ApprovalResponse approvalResponse(TradeinVoucher voucher) {
        TradeinVoucher.Approval approval = voucher.approval();
        if (approval == null) {
            return new TradeinDto.ApprovalResponse(null, voucher.code(), null, voucher.branch(), null,
                    voucher.estimate(), null, null, null, null, "NONE");
        }
        return new TradeinDto.ApprovalResponse(approval.id(), voucher.code(), approval.seller(),
                approval.branch(), approval.imei(), approval.estimate(), approval.proposed(),
                approval.deviationPercent(), approval.reason(), approval.batteryPercent(), approval.status().name());
    }
}
