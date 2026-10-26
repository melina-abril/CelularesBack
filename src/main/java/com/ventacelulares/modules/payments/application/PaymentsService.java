package com.ventacelulares.modules.payments.application;

import com.ventacelulares.modules.payments.application.dto.PaymentOptionResponse;
import com.ventacelulares.modules.payments.application.dto.PaymentOptionStatus;
import com.ventacelulares.modules.payments.application.dto.PaymentOptionsResponse;
import com.ventacelulares.modules.payments.domain.PaymentPlan;
import com.ventacelulares.modules.payments.domain.PaymentVariant;
import com.ventacelulares.modules.payments.domain.PaymentsRepository;
import com.ventacelulares.shared.domain.Money;
import com.ventacelulares.shared.exception.BusinessRuleException;
import com.ventacelulares.shared.exception.NotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentsService {
    private final PaymentsRepository repository;
    private final Clock clock;

    public PaymentsService(PaymentsRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public PaymentOptionsResponse getPaymentOptions(String variantId, LocalDate date) {
        if (variantId == null || variantId.isBlank()) {
            throw new BusinessRuleException("BR-03", "No se pudo identificar la variante elegida");
        }
        if (date == null) {
            throw new BusinessRuleException("BR-03", "La fecha de consulta es obligatoria");
        }

        PaymentVariant variant = repository.findVariant(variantId)
                .orElseThrow(() -> new NotFoundException("No se pudo identificar la variante elegida"));
        Money price = repository.findListPrice(variantId, date)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "No se pudieron consultar las opciones de pago. Intentá nuevamente"));
        if (price.amount().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudieron consultar las opciones de pago. Intentá nuevamente");
        }

        List<PaymentOptionResponse> options = repository.findPlans(variantId).stream()
                .map(plan -> calculate(plan, price, date))
                .toList();
        int promotions = (int) options.stream()
                .filter(option -> option.status() == PaymentOptionStatus.INTEREST_FREE_TODAY)
                .count();

        // Respuesta dummy basada en planes en memoria; reemplazar por listas y promociones reales.
        return new PaymentOptionsResponse(variant.id(), variant.model(), variant.memoryGb(),
                variant.color(), variant.condition(), price.amount(), price.currency(), date,
                promotions, options);
    }

    private PaymentOptionResponse calculate(PaymentPlan plan, Money price, LocalDate date) {
        boolean applicable = plan.appliesOn(date);
        BigDecimal adjustment = plan.adjustmentPercent();
        BigDecimal cft = plan.cftAnnualPercent();
        BigDecimal quotedTotal = price.amount()
                .multiply(BigDecimal.ONE.add(adjustment.movePointLeft(2)));
        BigDecimal installmentAmount = quotedTotal.divide(
                BigDecimal.valueOf(plan.installments()), 2, RoundingMode.HALF_EVEN);
        BigDecimal total = installmentAmount.multiply(BigDecimal.valueOf(plan.installments()))
                .setScale(2, RoundingMode.HALF_EVEN);

        if (applicable && plan.installments() > 1 && total.compareTo(cashPrice(price)) < 0) {
            throw new BusinessRuleException("BR-03", "El total en cuotas no puede ser menor al precio contado");
        }
        BigDecimal actualAdjustment = total.subtract(price.amount())
                .multiply(BigDecimal.valueOf(100))
                .divide(price.amount(), 4, RoundingMode.HALF_EVEN);
        boolean interestFreeToday = applicable && plan.interestFreePromotion()
                && plan.bank() != null && !plan.bank().isBlank()
                && plan.installments() > 1 && actualAdjustment.signum() == 0
                && cft.signum() == 0 && !plan.promotionDays().isEmpty();
        PaymentOptionStatus status = !applicable ? PaymentOptionStatus.UNAVAILABLE_TODAY
                : interestFreeToday ? PaymentOptionStatus.INTEREST_FREE_TODAY
                : PaymentOptionStatus.AVAILABLE;
        String label = switch (status) {
            case AVAILABLE -> "Disponible";
            case INTEREST_FREE_TODAY -> "Sin interés hoy";
            case UNAVAILABLE_TODAY -> "No disponible hoy";
        };
        String reason = applicable ? null : unavailableReason(plan, date);
        return new PaymentOptionResponse(plan.method(), plan.bank(), plan.installments(),
                installmentAmount, total, actualAdjustment, cft, status, label, reason,
                plan.promotionDays(), plan.validFrom(), plan.validTo());
    }

    private BigDecimal cashPrice(Money price) {
        return price.amount().multiply(new BigDecimal("0.92"))
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    private String unavailableReason(PaymentPlan plan, LocalDate date) {
        if (date.isBefore(plan.validFrom()) || date.isAfter(plan.validTo())) {
            return "Promoción fuera de vigencia";
        }
        return "Solo " + plan.promotionDays().stream()
                .sorted()
                .map(day -> switch (day) {
                    case MONDAY -> "lunes";
                    case TUESDAY -> "martes";
                    case WEDNESDAY -> "miércoles";
                    case THURSDAY -> "jueves";
                    case FRIDAY -> "viernes";
                    case SATURDAY -> "sábado";
                    case SUNDAY -> "domingo";
                })
                .collect(java.util.stream.Collectors.joining(", "));
    }
}
