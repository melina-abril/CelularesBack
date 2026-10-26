package com.ventacelulares.modules.catalogo.application.dto;

import com.ventacelulares.shared.domain.Sucursal;

public record DisponibilidadResponse(Sucursal sucursal, boolean disponible, Integer cantidad) {
}
