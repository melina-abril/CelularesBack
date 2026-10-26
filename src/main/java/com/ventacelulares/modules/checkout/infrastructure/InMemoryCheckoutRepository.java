package com.ventacelulares.modules.checkout.infrastructure;

import com.ventacelulares.modules.checkout.domain.CheckoutRepository;
import com.ventacelulares.shared.domain.Sucursal;
import com.ventacelulares.shared.exception.ConflictException;
import com.ventacelulares.shared.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryCheckoutRepository implements CheckoutRepository {
    // Datos dummy; reemplazar por un adaptador PostgreSQL con bloqueo transaccional de unidades.
    private final Map<String, Product> products = Map.of(
            "SAM-A55-128-BLK", new Product("SAM-A55-128-BLK", "Samsung Galaxy A55", "Negro", "128 GB", new BigDecimal("649999.00"), false),
            "MOTO-G85-256-BLU", new Product("MOTO-G85-256-BLU", "Motorola Moto G85", "Azul", "256 GB", new BigDecimal("479999.00"), false),
            "IPH-16-128-BLK", new Product("IPH-16-128-BLK", "Apple iPhone 16", "Negro", "128 GB", new BigDecimal("1599999.00"), true));
    private final List<Accessory> accessoryCatalog = List.of(
            new Accessory("CASE-A55", "Funda Galaxy A55", new BigDecimal("19999.00")),
            new Accessory("CHARGER-USBC", "Cargador USB-C", new BigDecimal("34999.00")));
    private final Map<String, String> units = Map.of(
            "UNIT-001", "SAM-A55-128-BLK|PALERMO",
            "UNIT-002", "SAM-A55-128-BLK|PALERMO",
            "UNIT-003", "SAM-A55-128-BLK|BELGRANO",
            "UNIT-004", "MOTO-G85-256-BLU|CABALLITO",
            "UNIT-005", "IPH-16-128-BLK|PALERMO");
    private final Map<String, Cart> carts = new HashMap<>();
    private final Map<String, Reservation> reservations = new HashMap<>();
    private final Map<String, String> reservedUnits = new HashMap<>();
    private final Map<String, String> soldUnits = new HashMap<>();
    private final List<Order> orders = new ArrayList<>();
    private long nextReservation = 1;
    private long nextOrder = 1;

    @Override
    public Product product(String variantId) {
        Product result = products.get(variantId);
        if (result == null) throw new NotFoundException("Variante no encontrada");
        return result;
    }

    @Override
    public List<Accessory> accessories() {
        return accessoryCatalog;
    }

    @Override
    public Accessory accessory(String id) {
        return accessoryCatalog.stream().filter(item -> item.id().equals(id)).findFirst()
                .orElseThrow(() -> new NotFoundException("Accesorio no encontrado: " + id));
    }

    @Override
    public synchronized Cart cart(String customer) {
        return carts.getOrDefault(customer, new Cart(null, List.of(), null));
    }

    @Override
    public synchronized Cart updateCart(String customer, Cart cart) {
        Cart snapshot = new Cart(cart.variantId(), List.copyOf(cart.accessoryIds()), cart.voucherCode());
        carts.put(customer, snapshot);
        releaseCustomerReservations(customer);
        return snapshot;
    }

    private void releaseCustomerReservations(String customer) {
        List<String> ids = reservations.values().stream().filter(r -> r.customer().equals(customer))
                .map(Reservation::id).toList();
        for (String id : ids) {
            reservations.remove(id);
            reservedUnits.remove(id);
        }
    }

    private void removeExpired(ZonedDateTime now) {
        List<String> ids = reservations.values().stream().filter(r -> !r.expiresAt().isAfter(now))
                .map(Reservation::id).toList();
        for (String id : ids) {
            reservations.remove(id);
            reservedUnits.remove(id);
        }
    }

    @Override
    public synchronized List<Availability> availability(String variantId, ZonedDateTime now) {
        product(variantId);
        removeExpired(now);
        List<Availability> result = new ArrayList<>();
        for (Sucursal branch : Sucursal.values()) {
            String key = variantId + "|" + branch.name();
            int count = (int) units.entrySet().stream()
                    .filter(entry -> key.equals(entry.getValue()))
                    .filter(entry -> !soldUnits.containsKey(entry.getKey()))
                    .filter(entry -> !reservedUnits.containsValue(entry.getKey())).count();
            if (count > 0) result.add(new Availability(branch, count));
        }
        return List.copyOf(result);
    }

    @Override
    public synchronized Reservation reserve(String customer, String variantId, Sucursal branch, ZonedDateTime now) {
        product(variantId);
        removeExpired(now);
        if (!variantId.equals(cart(customer).variantId())) throw new ConflictException("La variante no coincide con el carrito");
        releaseCustomerReservations(customer);
        String key = variantId + "|" + branch.name();
        String unit = units.entrySet().stream().filter(entry -> key.equals(entry.getValue()))
                .map(Map.Entry::getKey).sorted()
                .filter(id -> !soldUnits.containsKey(id) && !reservedUnits.containsValue(id))
                .findFirst().orElseThrow(() -> new ConflictException("No hay unidades disponibles en la sucursal"));
        String id = "RES-" + String.format("%05d", nextReservation++);
        Reservation reservation = new Reservation(id, customer, variantId, branch, now.plusMinutes(15));
        reservations.put(id, reservation);
        reservedUnits.put(id, unit);
        return reservation;
    }

    @Override
    public synchronized Reservation reservation(String id, String customer, ZonedDateTime now) {
        removeExpired(now);
        Reservation result = reservations.get(id);
        if (result == null || !result.customer().equals(customer)) {
            throw new ConflictException("La reserva no existe o venció; volvé a verificar el stock");
        }
        return result;
    }

    @Override
    public synchronized Order placeOrder(String customer, String reservationId, String dni,
                                         String invoiceType, String deliveryMethod, String postalCode,
                                         String paymentMethod, BigDecimal total, ZonedDateTime now) {
        Reservation reservation = reservation(reservationId, customer, now);
        Product selected = product(reservation.variantId());
        if (!reservation.variantId().equals(cart(customer).variantId())) {
            throw new ConflictException("El carrito cambió después de la reserva");
        }
        if (selected.launch() && orders.stream().filter(order -> order.dni().equals(dni))
                .filter(order -> order.variantId().equals(selected.variantId()))
                .filter(order -> !"CANCELADO".equals(order.status())).count() >= 2) {
            throw new com.ventacelulares.shared.exception.BusinessRuleException("BR-07", "Máximo dos unidades del lanzamiento por DNI");
        }
        String unit = reservedUnits.remove(reservationId);
        if (unit == null || soldUnits.containsKey(unit)) throw new ConflictException("La unidad ya no está disponible");
        String id = "ONL-" + now.getYear() + "-" + String.format("%05d", nextOrder++);
        soldUnits.put(unit, id);
        reservations.remove(reservationId);
        boolean transfer = "TRANSFERENCIA".equals(paymentMethod);
        Order order = new Order(id, customer, selected.variantId(), reservationId, dni,
                invoiceType, deliveryMethod, reservation.branch(), postalCode, paymentMethod,
                transfer ? "PENDIENTE_PAGO" : "PAGADO", total, now,
                transfer ? now.plusHours(48) : null);
        orders.add(order);
        carts.put(customer, new Cart(null, List.of(), null));
        return order;
    }
}