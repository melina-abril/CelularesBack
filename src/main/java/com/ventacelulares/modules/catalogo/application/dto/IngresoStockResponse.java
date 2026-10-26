package com.ventacelulares.modules.catalogo.application.dto;

public record IngresoStockResponse(String sku, int cantidadIngresada, int avisosEnviados, int avisosRechazados, int erroresProcesamiento) {
}
