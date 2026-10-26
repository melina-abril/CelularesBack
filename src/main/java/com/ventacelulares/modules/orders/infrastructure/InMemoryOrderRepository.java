package com.ventacelulares.modules.orders.infrastructure;

import com.ventacelulares.modules.orders.domain.DeliveryMethod;
import com.ventacelulares.modules.orders.domain.Order;
import com.ventacelulares.modules.orders.domain.OrderRepository;
import com.ventacelulares.modules.orders.domain.OrderStatus;
import com.ventacelulares.modules.orders.domain.PaymentMethod;
import com.ventacelulares.shared.domain.Sucursal;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryOrderRepository implements OrderRepository {
    private static final Set<String> AVAILABLE_IMEIS = Set.of("490154203237518", "356938035643809");
    private final Map<String, Order> orders = new HashMap<>();

    public InMemoryOrderRepository(Clock clock) {
        ZonedDateTime now = ZonedDateTime.now(clock);
        // Datos dummy; reemplazar este adaptador por PostgreSQL.
        save(new Order("ORD-1001", "Samsung Galaxy A55", "Azul · 128 GB",
                new BigDecimal("649999.00"), Sucursal.PALERMO, DeliveryMethod.RETIRO,
                PaymentMethod.TRANSFERENCIA, OrderStatus.PENDIENTE_PAGO,
                now.minusHours(44), null, null));
        save(new Order("ORD-1002", "Motorola Edge 50", "Negro · 256 GB",
                new BigDecimal("789999.00"), Sucursal.BELGRANO, DeliveryMethod.ENVIO,
                PaymentMethod.TARJETA, OrderStatus.PAGADO,
                now.minusHours(3), null, null));
        save(new Order("ORD-1003", "Apple iPhone 15", "Negro · 128 GB",
                new BigDecimal("1699999.00"), Sucursal.PALERMO, DeliveryMethod.RETIRO,
                PaymentMethod.TARJETA, OrderStatus.ENTREGADO,
                now.minusDays(5), now.minusDays(2), "353918057625408"));
    }

    @Override
    public synchronized List<Order> findAll() {
        return new ArrayList<>(orders.values());
    }

    @Override
    public synchronized Optional<Order> findById(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    @Override
    public synchronized boolean isImeiAvailable(String imei, String orderId) {
        return AVAILABLE_IMEIS.contains(imei) && orders.values().stream()
                .noneMatch(order -> !order.id().equals(orderId) && imei.equals(order.imei())
                        && order.status() != OrderStatus.DEVUELTO
                        && order.status() != OrderStatus.CANCELADO);
    }

    @Override
    public synchronized void save(Order order) {
        orders.put(order.id(), order);
    }
}
