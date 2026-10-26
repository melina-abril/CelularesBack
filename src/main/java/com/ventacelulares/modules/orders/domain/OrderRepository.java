package com.ventacelulares.modules.orders.domain;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    List<Order> findAll();
    Optional<Order> findById(String id);
    boolean isImeiAvailable(String imei, String orderId);
    void save(Order order);
}
