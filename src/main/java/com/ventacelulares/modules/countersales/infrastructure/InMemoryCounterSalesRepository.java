package com.ventacelulares.modules.countersales.infrastructure;

import com.ventacelulares.modules.countersales.domain.CounterSalesRepository;
import com.ventacelulares.shared.domain.Sucursal;
import com.ventacelulares.shared.exception.ConflictException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryCounterSalesRepository implements CounterSalesRepository {
    // Datos dummy en memoria: reemplazar este adaptador por PostgreSQL.
    private final Map<String, Customer> customers = new HashMap<>();
    private final Map<String, Unit> units = new HashMap<>();
    private final Map<String, Approval> approvals = new HashMap<>();
    private final Map<String, Sale> sales = new HashMap<>();
    private final Map<String, Voucher> vouchers = new HashMap<>();
    private final Map<String, BigDecimal> accessories = Map.of(
            "CASE-001", new BigDecimal("18999.00"),
            "CHARGER-001", new BigDecimal("34999.00"));

    public InMemoryCounterSalesRepository(Clock clock) {
        customers.put("30123456", new Customer("30123456", "María López", "1155550123", false, null));
        units.put("490154203237518", new Unit("490154203237518", "Samsung Galaxy A55 128 GB", Sucursal.PALERMO, new BigDecimal("699999.00"), true, false));
        units.put("356938035643809", new Unit("356938035643809", "Motorola Edge 50 256 GB", Sucursal.PALERMO, new BigDecimal("899999.00"), true, true));
        vouchers.put("CANJE-2025-001", new Voucher("CANJE-2025-001", new BigDecimal("120000.00"), ZonedDateTime.now(clock).plusHours(72)));
    }

    public synchronized Optional<Customer> findCustomer(String dni) { return Optional.ofNullable(customers.get(dni)); }

    public synchronized Customer saveCustomer(Customer customer) {
        if (customers.containsKey(customer.dni())) throw new ConflictException("Ya existe un cliente con ese DNI");
        customers.put(customer.dni(), customer);
        return customer;
    }

    public synchronized Optional<Unit> findUnit(String imei) { return Optional.ofNullable(units.get(imei)); }
    public synchronized Optional<Approval> findApproval(String id) { return Optional.ofNullable(approvals.get(id)); }
    public synchronized Optional<Voucher> findVoucher(String code) { return Optional.ofNullable(vouchers.get(code)); }
    public Optional<BigDecimal> accessoryPrice(String sku) { return Optional.ofNullable(accessories.get(sku)); }

    public synchronized Approval saveApproval(Approval approval) {
        approvals.put(approval.id(), approval);
        return approval;
    }

    public synchronized Sale saveSale(Sale sale, BigDecimal discountPercent, String approvalId) {
        Unit unit = units.get(sale.imei());
        if (unit == null || !unit.available() || unit.branch() != sale.branch()) {
            throw new ConflictException("La unidad ya no está disponible en la sucursal");
        }
        if (discountPercent.compareTo(new BigDecimal("5.00")) > 0) {
            Approval approval = approvals.get(approvalId);
            if (approval == null || !approval.approved() || !approval.imei().equals(sale.imei())
                    || approval.branch() != sale.branch() || approval.percent().compareTo(discountPercent) != 0) {
                throw new ConflictException("El descuento no tiene aprobación vigente en la sucursal");
            }
        }
        if (unit.launch() && sales.values().stream().filter(previous -> previous.dni().equals(sale.dni()))
                .map(previous -> units.get(previous.imei()))
                .filter(previousUnit -> previousUnit != null && previousUnit.model().equals(unit.model())).count() >= 2) {
            throw new ConflictException("Límite de dos unidades del lanzamiento por DNI");
        }
        units.put(unit.imei(), new Unit(unit.imei(), unit.model(), unit.branch(), unit.cashPrice(), false, unit.launch()));
        sales.put(sale.id(), sale);
        return sale;
    }
}
