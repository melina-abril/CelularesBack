package com.ventacelulares.modules.orders.application.dto;

import java.util.List;

public record OrderPageResponse(List<OrderResponse> items, int page, int size, long total) {
}
