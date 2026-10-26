package com.ventacelulares.modules.warranties.application;

import com.ventacelulares.modules.warranties.application.dto.CreateWarrantyClaimRequest;
import com.ventacelulares.modules.warranties.application.dto.UpdateWarrantyClaimStatusRequest;
import com.ventacelulares.modules.warranties.application.dto.WarrantyClaimResponse;
import com.ventacelulares.modules.warranties.application.dto.WarrantyCoverageResponse;
import com.ventacelulares.modules.warranties.domain.ClaimStatus;
import com.ventacelulares.modules.warranties.domain.WarrantyClaim;
import com.ventacelulares.modules.warranties.domain.WarrantyRepository;
import com.ventacelulares.modules.warranties.domain.WarrantySale;
import com.ventacelulares.shared.domain.Imei;
import com.ventacelulares.shared.domain.Sucursal;
import com.ventacelulares.shared.exception.BusinessRuleException;
import com.ventacelulares.shared.exception.ConflictException;
import com.ventacelulares.shared.exception.NotFoundException;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import org.springframework.stereotype.Service;

@Service
public class WarrantyService {
    private final WarrantyRepository repository;
    private final Clock clock;

    public WarrantyService(WarrantyRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public WarrantyCoverageResponse findWarrantyCoverage(String imei, String dni, String invoiceNumber) {
        if (blank(imei) && blank(dni) && blank(invoiceNumber)) {
            throw new BusinessRuleException("BR-09", "Ingresá un IMEI, DNI o número de factura");
        }
        if (!blank(imei)) {
            validateImei(imei);
        }
        WarrantySale sale = findSale(imei, dni, invoiceNumber);
        // Respuesta basada en ventas dummy; se reemplaza al conectar el módulo de ventas.
        return new WarrantyCoverageResponse(sale.imei(), sale.model(), sale.condition(),
                sale.invoiceNumber(), sale.deliveredAt().toOffsetDateTime(),
                sale.expiresAt().toOffsetDateTime(),
                !ZonedDateTime.now(clock).isAfter(sale.expiresAt()));
    }

    public WarrantyClaimResponse createWarrantyClaim(CreateWarrantyClaimRequest request) {
        validateImei(request.imei());
        WarrantySale sale = findSale(request.imei(), null, null);
        ZonedDateTime now = ZonedDateTime.now(clock);
        if (now.isAfter(sale.expiresAt())) {
            throw new BusinessRuleException("BR-09", "La garantía del equipo está vencida");
        }
        String branch = parseBranch(request.receivingBranch()).name();
        WarrantyClaim claim = new WarrantyClaim(repository.nextClaimId(), sale.imei(),
                request.reportedFault(), request.photos(), branch, ClaimStatus.RECIBIDO,
                null, null, now, now);
        // Reclamo dummy persistido en memoria; se reemplaza por PostgreSQL.
        return toResponse(repository.save(claim));
    }

    public WarrantyClaimResponse getWarrantyClaim(String claimId) {
        return toResponse(findClaim(claimId));
    }

    public synchronized WarrantyClaimResponse updateWarrantyClaimStatus(
            String claimId, UpdateWarrantyClaimStatusRequest request) {
        WarrantyClaim claim = findClaim(claimId);
        ClaimStatus next = parseStatus(request.status());
        if (!allowedNext(claim.status()).contains(next)) {
            throw new ConflictException("No se puede avanzar el reclamo de " + claim.status() + " a " + next);
        }
        if (next == ClaimStatus.RECHAZADO && blank(request.rejectionReason())) {
            throw new BusinessRuleException("BR-09", "Indicá el motivo del rechazo");
        }
        WarrantyClaim updated = claim.withStatus(next, request.diagnosis(),
                next == ClaimStatus.RECHAZADO ? request.rejectionReason() : null,
                ZonedDateTime.now(clock));
        // Seguimiento dummy en memoria; se reemplaza por PostgreSQL.
        return toResponse(repository.save(updated));
    }

    private WarrantySale findSale(String imei, String dni, String invoiceNumber) {
        return repository.findSale(imei, dni, invoiceNumber)
                .orElseThrow(() -> new NotFoundException("No se encontró una venta para los datos indicados"));
    }

    private WarrantyClaim findClaim(String id) {
        return repository.findClaim(id)
                .orElseThrow(() -> new NotFoundException("No se encontró el reclamo " + id));
    }

    private WarrantyClaimResponse toResponse(WarrantyClaim claim) {
        return new WarrantyClaimResponse(claim.id(), claim.imei(), claim.reportedFault(),
                claim.photos(), claim.receivingBranch(), claim.status().name(),
                claim.diagnosis(), claim.rejectionReason(), claim.createdAt().toOffsetDateTime(),
                claim.updatedAt().toOffsetDateTime());
    }

    private EnumSet<ClaimStatus> allowedNext(ClaimStatus current) {
        return switch (current) {
            case RECIBIDO -> EnumSet.of(ClaimStatus.EN_DIAGNOSTICO, ClaimStatus.RECHAZADO);
            case EN_DIAGNOSTICO -> EnumSet.of(ClaimStatus.EN_REPARACION,
                    ClaimStatus.REEMPLAZO_APROBADO, ClaimStatus.RECHAZADO);
            case EN_REPARACION, REEMPLAZO_APROBADO -> EnumSet.of(ClaimStatus.LISTO_PARA_RETIRAR);
            case LISTO_PARA_RETIRAR -> EnumSet.of(ClaimStatus.ENTREGADO);
            case ENTREGADO, RECHAZADO -> EnumSet.noneOf(ClaimStatus.class);
        };
    }

    private ClaimStatus parseStatus(String value) {
        try {
            return ClaimStatus.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException("BR-09", "Estado de reclamo inválido");
        }
    }

    private Sucursal parseBranch(String value) {
        try {
            return Sucursal.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException("BR-09", "Sucursal de recepción inválida");
        }
    }

    private void validateImei(String value) {
        try {
            new Imei(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException("BR-09", "El IMEI indicado no es válido");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
