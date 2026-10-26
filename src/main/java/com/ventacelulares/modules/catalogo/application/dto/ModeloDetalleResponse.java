package com.ventacelulares.modules.catalogo.application.dto;

import com.ventacelulares.modules.catalogo.domain.Condicion;
import java.math.BigDecimal;
import java.util.List;

public record ModeloDetalleResponse(String modeloId, String marca, String modelo, Condicion condicion, String grado, String fotoUrl, List<VarianteDetalle> variantes) {
    public record VarianteDetalle(String sku, String color, int memoriaGb, BigDecimal precioContado, BigDecimal cuotaMasBaja, Integer cantidadCuotas, String estadoStock, List<DisponibilidadResponse> sucursales) {
    }
}
