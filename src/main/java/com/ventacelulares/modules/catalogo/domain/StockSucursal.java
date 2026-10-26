package com.ventacelulares.modules.catalogo.domain;

import com.ventacelulares.shared.domain.Sucursal;

public record StockSucursal(Sucursal sucursal, int unidades, int reservadas, int enTransito) {
    public int disponibles() {
        return Math.max(0, unidades - reservadas - enTransito);
    }
}
