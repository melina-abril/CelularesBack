package com.ventacelulares.modules.catalogo.application.dto;

import com.ventacelulares.modules.catalogo.domain.Condicion;
import java.math.BigDecimal;

public record ModeloResumenResponse(String modeloId, String marca, String modelo, Condicion condicion, String grado, String fotoUrl, String sku, String color, int memoriaGb, BigDecimal precioContado, BigDecimal cuotaMasBaja, Integer cantidadCuotas, String estadoStock, int sucursalesConStock, Integer cantidadTotal) {
}
