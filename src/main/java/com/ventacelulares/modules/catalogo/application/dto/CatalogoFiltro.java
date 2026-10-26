package com.ventacelulares.modules.catalogo.application.dto;

import com.ventacelulares.modules.catalogo.domain.Condicion;
import com.ventacelulares.shared.domain.Sucursal;
import java.math.BigDecimal;

public record CatalogoFiltro(String marca, BigDecimal precioMinimo, BigDecimal precioMaximo, Integer memoriaGb, Condicion condicion, Sucursal sucursal, int page, int size) {
}
