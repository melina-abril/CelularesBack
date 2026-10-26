package com.ventacelulares.modules.catalogo.api;

import com.ventacelulares.modules.catalogo.application.CatalogoService;
import com.ventacelulares.modules.catalogo.application.dto.AvisoResponse;
import com.ventacelulares.modules.catalogo.application.dto.CatalogoFiltro;
import com.ventacelulares.modules.catalogo.application.dto.CatalogoPageResponse;
import com.ventacelulares.modules.catalogo.application.dto.CrearAvisoRequest;
import com.ventacelulares.modules.catalogo.application.dto.IngresoStockRequest;
import com.ventacelulares.modules.catalogo.application.dto.IngresoStockResponse;
import com.ventacelulares.modules.catalogo.application.dto.ModeloDetalleResponse;
import com.ventacelulares.modules.catalogo.domain.Condicion;
import com.ventacelulares.shared.domain.Sucursal;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalogo")
public class CatalogoController {
    private final CatalogoService service;

    public CatalogoController(CatalogoService service) {
        this.service = service;
    }

    @GetMapping("/modelos")
    @PreAuthorize("hasAnyRole('CLIENTE','VENDEDOR','SUPERVISOR','BACKOFFICE','GERENCIA')")
    public CatalogoPageResponse listModels(@RequestParam(required = false) String marca,
            @RequestParam(required = false) BigDecimal precioMinimo,
            @RequestParam(required = false) BigDecimal precioMaximo,
            @RequestParam(required = false) Integer memoriaGb,
            @RequestParam(required = false) Condicion condicion,
            @RequestParam(required = false) Sucursal sucursal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listModels(new CatalogoFiltro(marca, precioMinimo, precioMaximo, memoriaGb, condicion, sucursal, page, size));
    }

    @GetMapping("/modelos/{modeloId}")
    @PreAuthorize("hasAnyRole('CLIENTE','VENDEDOR','SUPERVISOR','BACKOFFICE','GERENCIA')")
    public ModeloDetalleResponse getModelDetail(@PathVariable String modeloId) {
        return service.getModelDetail(modeloId);
    }

    @PostMapping("/avisos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CLIENTE','VENDEDOR','SUPERVISOR','BACKOFFICE','GERENCIA')")
    public AvisoResponse createStockAlert(@Valid @RequestBody CrearAvisoRequest request) {
        return service.createStockAlert(request);
    }

    @PostMapping("/ingresos-stock")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('VENDEDOR','SUPERVISOR','BACKOFFICE')")
    public IngresoStockResponse registerStockArrival(@Valid @RequestBody IngresoStockRequest request) {
        return service.registerStockArrival(request);
    }
}
