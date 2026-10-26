package com.ventacelulares.modules.tradein.infrastructure;

import com.ventacelulares.modules.tradein.domain.TradeinRepository;
import com.ventacelulares.modules.tradein.domain.TradeinVoucher;
import com.ventacelulares.shared.domain.Sucursal;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryTradeinRepository implements TradeinRepository {
    private final Map<String, TradeinVoucher> vouchers = new ConcurrentHashMap<>();

    public InMemoryTradeinRepository(Clock clock) {
        // Datos dummy; reemplazar este adaptador por PostgreSQL.
        TradeinVoucher example = new TradeinVoucher("CANJE-DEMO-001", Sucursal.PALERMO,
                new TradeinVoucher.Device("Samsung", "Galaxy S23", 256),
                new TradeinVoucher.Checklist(true, true, 86, true, true),
                new BigDecimal("310000.00"), ZonedDateTime.now(clock).minusHours(2));
        save(example);
    }

    @Override
    public Optional<TradeinVoucher> findByCode(String code) {
        return Optional.ofNullable(vouchers.get(code));
    }

    @Override
    public void save(TradeinVoucher voucher) {
        vouchers.put(voucher.code(), voucher);
    }
}
