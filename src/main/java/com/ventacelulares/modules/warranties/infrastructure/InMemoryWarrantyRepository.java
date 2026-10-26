package com.ventacelulares.modules.warranties.infrastructure;

import com.ventacelulares.modules.warranties.domain.WarrantyClaim;
import com.ventacelulares.modules.warranties.domain.WarrantyRepository;
import com.ventacelulares.modules.warranties.domain.WarrantySale;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryWarrantyRepository implements WarrantyRepository {
    private final List<WarrantySale> sales;
    private final Map<String, WarrantyClaim> claims = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1001);

    public InMemoryWarrantyRepository(Clock clock) {
        ZonedDateTime now = ZonedDateTime.now(clock);
        // Ventas de ejemplo dummy; este adaptador se reemplaza por PostgreSQL.
        sales = List.of(
                new WarrantySale("490154203237518", "30123456", "B-0001-00004210",
                        "Samsung Galaxy S25 256 GB", "NUEVO", now.minusMonths(2)),
                new WarrantySale("356938035643809", "27456789", "B-0003-00001862",
                        "Motorola Edge 50 256 GB", "REACONDICIONADO", now.minusMonths(3)));
    }

    @Override
    public Optional<WarrantySale> findSale(String imei, String dni, String invoiceNumber) {
        return sales.stream().filter(sale ->
                (imei == null || sale.imei().equals(imei))
                        && (dni == null || sale.dni().equals(dni))
                        && (invoiceNumber == null || sale.invoiceNumber().equals(invoiceNumber)))
                .findFirst();
    }

    @Override
    public Optional<WarrantyClaim> findClaim(String claimId) {
        return Optional.ofNullable(claims.get(claimId));
    }

    @Override
    public String nextClaimId() {
        return "CLM-" + nextId.getAndIncrement();
    }

    @Override
    public WarrantyClaim save(WarrantyClaim claim) {
        claims.put(claim.id(), claim);
        return claim;
    }
}
