package com.ventacelulares.modules.dashboard.infrastructure;

import com.ventacelulares.modules.dashboard.domain.DashboardRepository;
import com.ventacelulares.modules.dashboard.domain.InventoryRecord;
import com.ventacelulares.modules.dashboard.domain.SaleRecord;
import com.ventacelulares.modules.dashboard.domain.TransferRecord;
import com.ventacelulares.shared.domain.Money;
import com.ventacelulares.shared.domain.Sucursal;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryDashboardRepository implements DashboardRepository {
    private final Clock clock;

    public InMemoryDashboardRepository(Clock clock) {
        this.clock = clock;
    }

    @Override
    public List<SaleRecord> sales() {
        // Datos dummy fijos para cada día relativo al reloj; reemplazar por PostgreSQL.
        LocalDate today = LocalDate.now(clock);
        return List.of(
                new SaleRecord(today, Sucursal.PALERMO, "MOSTRADOR", "TARJETA_CREDITO", "Samsung Galaxy A55 128 GB", pesos("729999.00"), false),
                new SaleRecord(today.minusDays(1), Sucursal.PALERMO, "MOSTRADOR", "TRANSFERENCIA", "Motorola Edge 50 Fusion 256 GB", pesos("599999.00"), true),
                new SaleRecord(today.minusDays(2), Sucursal.BELGRANO, "ONLINE", "TARJETA_CREDITO", "iPhone 15 128 GB", pesos("1699999.00"), false),
                new SaleRecord(today.minusDays(3), Sucursal.CABALLITO, "MOSTRADOR", "DEBITO", "Samsung Galaxy A55 128 GB", pesos("729999.00"), true),
                new SaleRecord(today.minusDays(4), Sucursal.PALERMO, "ONLINE", "TRANSFERENCIA", "Xiaomi Redmi Note 13 256 GB", pesos("449999.00"), false),
                new SaleRecord(today.minusDays(6), Sucursal.RECOLETA, "MOSTRADOR", "TARJETA_CREDITO", "iPhone 15 128 GB", pesos("1699999.00"), false),
                new SaleRecord(today.minusDays(9), Sucursal.PALERMO, "MOSTRADOR", "EFECTIVO", "Samsung Galaxy A55 128 GB", pesos("729999.00"), false),
                new SaleRecord(today.minusDays(20), Sucursal.SAN_ISIDRO, "ONLINE", "TARJETA_CREDITO", "Motorola Edge 50 Fusion 256 GB", pesos("599999.00"), true));
    }

    @Override
    public List<InventoryRecord> inventory() {
        LocalDate today = LocalDate.now(clock);
        return List.of(
                new InventoryRecord(Sucursal.PALERMO, "Samsung Galaxy A55 128 GB", 1, today),
                new InventoryRecord(Sucursal.PALERMO, "Motorola Edge 50 Fusion 256 GB", 0, today.minusDays(1)),
                new InventoryRecord(Sucursal.BELGRANO, "iPhone 15 128 GB", 1, today.minusDays(2)),
                new InventoryRecord(Sucursal.CABALLITO, "Xiaomi Redmi Note 13 256 GB", 4, today.minusDays(3)),
                new InventoryRecord(Sucursal.RECOLETA, "Samsung Galaxy A55 128 GB", 1, today.minusDays(12)));
    }

    @Override
    public List<TransferRecord> transfersInTransit() {
        ZonedDateTime now = ZonedDateTime.now(clock);
        return List.of(
                new TransferRecord("TR-1042", "Samsung Galaxy A55 128 GB", "490154203237518", Sucursal.BELGRANO, Sucursal.PALERMO, now.minusHours(5)),
                new TransferRecord("TR-1043", "iPhone 15 128 GB", "490154203237518", Sucursal.RECOLETA, Sucursal.CABALLITO, now.minusDays(1)),
                new TransferRecord("TR-1044", "Motorola Edge 50 Fusion 256 GB", "490154203237518", Sucursal.PALERMO, Sucursal.SAN_ISIDRO, now.minusHours(2)));
    }

    private Money pesos(String value) {
        return new Money(new BigDecimal(value));
    }
}
