package com.ventacelulares.modules.orders.application;

import com.ventacelulares.modules.orders.application.dto.AdvanceOrderStatusRequest;
import com.ventacelulares.modules.orders.application.dto.OrderPageResponse;
import com.ventacelulares.modules.orders.application.dto.OrderResponse;
import com.ventacelulares.modules.orders.domain.DeliveryMethod;
import com.ventacelulares.modules.orders.domain.Order;
import com.ventacelulares.modules.orders.domain.OrderRepository;
import com.ventacelulares.modules.orders.domain.OrderStatus;
import com.ventacelulares.modules.orders.domain.PaymentMethod;
import com.ventacelulares.shared.domain.Imei;
import com.ventacelulares.shared.domain.Sucursal;
import com.ventacelulares.shared.exception.BusinessRuleException;
import com.ventacelulares.shared.exception.ConflictException;
import com.ventacelulares.shared.exception.NotFoundException;
import com.ventacelulares.shared.security.CurrentUser;
import com.ventacelulares.shared.security.Roles;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    private final OrderRepository repository;
    private final Clock clock;
    private final CurrentUser currentUser;

    public OrderService(OrderRepository repository, Clock clock, CurrentUser currentUser) {
        this.repository = repository;
        this.clock = clock;
        this.currentUser = currentUser;
    }

    public OrderPageResponse listOrders(OrderStatus status, Sucursal branch, PaymentMethod paymentMethod,
                                        ZonedDateTime from, ZonedDateTime to, int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new IllegalArgumentException("page debe ser no negativo y size debe estar entre 1 y 50");
        }
        Sucursal restrictedBranch = restrictedBranch();
        if (restrictedBranch != null && branch != null && branch != restrictedBranch) {
            throw new AccessDeniedException("No puede consultar otra sucursal");
        }
        ZonedDateTime now = ZonedDateTime.now(clock);
        List<Order> filtered = repository.findAll().stream()
                .filter(order -> restrictedBranch == null || order.branch() == restrictedBranch)
                .filter(order -> branch == null || order.branch() == branch)
                .filter(order -> status == null || effectiveStatus(order, now) == status)
                .filter(order -> paymentMethod == null || order.paymentMethod() == paymentMethod)
                .filter(order -> from == null || !order.createdAt().isBefore(from))
                .filter(order -> to == null || !order.createdAt().isAfter(to))
                .sorted(Comparator.comparing(Order::createdAt).reversed())
                .toList();
        long offset = (long) page * size;
        List<OrderResponse> items = offset >= filtered.size() ? List.of()
                : filtered.subList((int) offset, (int) Math.min(offset + size, filtered.size()))
                        .stream().map(order -> response(order, now)).toList();
        // Respuesta dummy respaldada por datos en memoria; reemplazar al conectar persistencia.
        return new OrderPageResponse(items, page, size, filtered.size());
    }

    public OrderResponse getOrder(String id) {
        return response(visibleOrder(id), ZonedDateTime.now(clock));
    }

    public synchronized OrderResponse advanceOrderStatus(String id, AdvanceOrderStatusRequest request) {
        Order order = visibleOrder(id);
        ZonedDateTime now = ZonedDateTime.now(clock);
        OrderStatus current = effectiveStatus(order, now);
        if (current == OrderStatus.CANCELADO && order.status() != OrderStatus.CANCELADO) {
            throw new ConflictException("La transferencia venció a las 48 horas; el pedido está cancelado");
        }
        if (!nextStatuses(order, now).contains(request.status())) {
            throw new ConflictException("La transición de estado solicitada no está permitida");
        }
        String imei = order.imei();
        if (request.status() == OrderStatus.PREPARADO) {
            try {
                imei = new Imei(request.imei()).value();
            } catch (IllegalArgumentException exception) {
                throw new BusinessRuleException("BR-01", "Se requiere un IMEI válido para preparar el pedido");
            }
            if (!repository.isImeiAvailable(imei, id)) {
                throw new ConflictException("El IMEI no está disponible para preparar el pedido");
            }
        }
        ZonedDateTime deliveredAt = request.status() == OrderStatus.ENTREGADO ? now : order.deliveredAt();
        Order updated = order.withStatus(request.status(), imei, deliveredAt);
        repository.save(updated);
        // Respuesta dummy; reemplazar al conectar el backend de stock y persistencia.
        return response(updated, now);
    }

    private Order visibleOrder(String id) {
        Order order = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado: " + id));
        Sucursal restrictedBranch = restrictedBranch();
        if (restrictedBranch != null && order.branch() != restrictedBranch) {
            throw new AccessDeniedException("No puede operar sobre otra sucursal");
        }
        return order;
    }

    private Sucursal restrictedBranch() {
        String role = currentUser.role();
        if (Roles.VENDEDOR.equals(role) || Roles.SUPERVISOR.equals(role)) {
            return currentUser.sucursal().orElseThrow(
                    () -> new AccessDeniedException("El usuario no tiene sucursal asignada"));
        }
        return null;
    }

    private OrderStatus effectiveStatus(Order order, ZonedDateTime now) {
        if (order.status() == OrderStatus.PENDIENTE_PAGO
                && order.paymentMethod() == PaymentMethod.TRANSFERENCIA
                && !now.isBefore(order.createdAt().plusHours(48))) {
            return OrderStatus.CANCELADO;
        }
        return order.status();
    }

    private List<OrderStatus> nextStatuses(Order order, ZonedDateTime now) {
        return switch (effectiveStatus(order, now)) {
            case PENDIENTE_PAGO -> List.of(OrderStatus.PAGADO, OrderStatus.CANCELADO);
            case PAGADO -> List.of(OrderStatus.PREPARADO, OrderStatus.CANCELADO);
            case PREPARADO -> List.of(order.deliveryMethod() == DeliveryMethod.ENVIO
                    ? OrderStatus.DESPACHADO : OrderStatus.LISTO_PARA_RETIRAR);
            case DESPACHADO, LISTO_PARA_RETIRAR -> List.of(OrderStatus.ENTREGADO);
            case ENTREGADO -> order.deliveredAt() != null
                    && !now.isAfter(order.deliveredAt().plusDays(10))
                    ? List.of(OrderStatus.DEVUELTO) : List.of();
            case CANCELADO, DEVUELTO -> List.of();
        };
    }

    private OrderResponse response(Order order, ZonedDateTime now) {
        ZonedDateTime transferExpiry = order.paymentMethod() == PaymentMethod.TRANSFERENCIA
                && order.status() == OrderStatus.PENDIENTE_PAGO ? order.createdAt().plusHours(48) : null;
        ZonedDateTime returnDeadline = order.deliveredAt() == null
                ? null : order.deliveredAt().plusDays(10);
        return new OrderResponse(order.id(), order.model(), order.variant(), order.total(),
                order.branch(), order.deliveryMethod(), order.paymentMethod(), effectiveStatus(order, now),
                order.createdAt(), order.deliveredAt(), order.imei(), transferExpiry,
                transferExpiry != null && !now.isBefore(transferExpiry.minusHours(6))
                        && now.isBefore(transferExpiry),
                returnDeadline, order.status() == OrderStatus.ENTREGADO
                        && returnDeadline != null && !now.isAfter(returnDeadline),
                nextStatuses(order, now));
    }
}
