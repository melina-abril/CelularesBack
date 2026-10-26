package com.ventacelulares.modules.catalogo.domain;

import com.ventacelulares.shared.domain.Money;

public record Variante(String sku, String color, int memoriaGb, Money precioContado) {
}
