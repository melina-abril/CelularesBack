package com.ventacelulares.modules.payments.domain;

import com.ventacelulares.shared.domain.Money;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentsRepository {
    Optional<PaymentVariant> findVariant(String variantId);

    Optional<Money> findListPrice(String variantId, LocalDate date);

    List<PaymentPlan> findPlans(String variantId);
}
