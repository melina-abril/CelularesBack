package com.ventacelulares.modules.catalogo.application.dto;

import java.util.List;

public record CatalogoPageResponse(List<ModeloResumenResponse> content, int page, int size, long totalElements, int totalPages) {
}
