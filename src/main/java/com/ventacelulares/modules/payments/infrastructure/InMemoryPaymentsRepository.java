package com.ventacelulares.modules.payments.infrastructure;

import com.ventacelulares.modules.payments.domain.PaymentMethod;
import com.ventacelulares.modules.payments.domain.PaymentPlan;
import com.ventacelulares.modules.payments.domain.PaymentVariant;
import com.ventacelulares.modules.payments.domain.PaymentsRepository;
import com.ventacelulares.shared.domain.Money;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryPaymentsRepository implements PaymentsRepository {
    private static final LocalDate PRICE_FROM = LocalDate.of(2025, 1, 1);
    private static final LocalDate PRICE_TO = LocalDate.of(2099, 12, 31);
    private static final Map<String, PaymentVariant> VARIANTS = Map.of(
            "samsung-galaxy-a55-256-black", new PaymentVariant(
                    "samsung-galaxy-a55-256-black", "Samsung Galaxy A55", 256, "Negro", "NUEVO"),
            "apple-iphone-15-128-black", new PaymentVariant(
                    "apple-iphone-15-128-black", "Apple iPhone 15", 128, "Negro", "NUEVO"),
            "motorola-edge-50-256-blue", new PaymentVariant(
                    "motorola-edge-50-256-blue", "Motorola Edge 50", 256, "Azul", "NUEVO"),
            "xiaomi-redmi-note-13-256-black", new PaymentVariant(
                    "xiaomi-redmi-note-13-256-black", "Xiaomi Redmi Note 13", 256, "Negro", "REACONDICIONADO_A"),
            "samsung-galaxy-a35-128-blue", new PaymentVariant(
                    "samsung-galaxy-a35-128-blue", "Samsung Galaxy A35", 128, "Azul", "NUEVO"));
    private static final Map<String, Money> PRICES = Map.of(
            "samsung-galaxy-a55-256-black", new Money(new BigDecimal("1080000.00")),
            "apple-iphone-15-128-black", new Money(new BigDecimal("2160000.00")),
            "motorola-edge-50-256-blue", new Money(new BigDecimal("900000.00")),
            "xiaomi-redmi-note-13-256-black", new Money(new BigDecimal("540000.00")),
            "samsung-galaxy-a35-128-blue", new Money(new BigDecimal("720000.00")));
    private static final List<PaymentPlan> STANDARD_PLANS = createPlans();

    @Override
    public Optional<PaymentVariant> findVariant(String variantId) {
        return Optional.ofNullable(VARIANTS.get(variantId));
    }

    @Override
    public Optional<Money> findListPrice(String variantId, LocalDate date) {
        if (date.isBefore(PRICE_FROM) || date.isAfter(PRICE_TO)) {
            return Optional.empty();
        }
        return Optional.ofNullable(PRICES.get(variantId));
    }

    @Override
    public List<PaymentPlan> findPlans(String variantId) {
        // Datos dummy en memoria: reemplazar por PostgreSQL y promociones configurables.
        return "samsung-galaxy-a35-128-blue".equals(variantId) ? List.of() : STANDARD_PLANS;
    }

    private static List<PaymentPlan> createPlans() {
        List<PaymentPlan> plans = new ArrayList<>();
        plans.add(plan(PaymentMethod.CASH, null, 1, "-8", "0"));
        plans.add(plan(PaymentMethod.BANK_TRANSFER, null, 1, "-6", "0"));
        plans.add(plan(PaymentMethod.DEBIT_CARD, null, 1, "0", "0"));
        plans.add(plan(PaymentMethod.CREDIT_CARD, null, 1, "0", "0"));
        int[] counts = {3, 6, 9, 12, 18};
        String[] markups = {"12", "20", "29", "42", "67"};
        String[] costs = {"56", "66", "77", "88", "103"};
        for (int i = 0; i < counts.length; i++) {
            plans.add(plan(PaymentMethod.CREDIT_CARD, "Banco Galicia", counts[i],
                    markups[i], costs[i]));
        }
        plans.add(new PaymentPlan(PaymentMethod.CREDIT_CARD, "Banco Nación", 6,
                BigDecimal.ZERO, BigDecimal.ZERO, true,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY),
                PRICE_FROM, PRICE_TO));
        plans.add(new PaymentPlan(PaymentMethod.CREDIT_CARD, "Banco Galicia", 12,
                BigDecimal.ZERO, BigDecimal.ZERO, true, Set.of(DayOfWeek.SATURDAY),
                PRICE_FROM, PRICE_TO));
        return List.copyOf(plans);
    }

    private static PaymentPlan plan(PaymentMethod method, String bank, int installments,
                                    String adjustment, String cft) {
        return new PaymentPlan(method, bank, installments, new BigDecimal(adjustment),
                new BigDecimal(cft), false, Set.of(), PRICE_FROM, PRICE_TO);
    }
}
