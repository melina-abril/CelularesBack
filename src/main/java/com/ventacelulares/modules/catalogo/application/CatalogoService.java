package com.ventacelulares.modules.catalogo.application;

import com.ventacelulares.modules.catalogo.application.dto.AvisoResponse;
import com.ventacelulares.modules.catalogo.application.dto.CatalogoFiltro;
import com.ventacelulares.modules.catalogo.application.dto.CatalogoPageResponse;
import com.ventacelulares.modules.catalogo.application.dto.CrearAvisoRequest;
import com.ventacelulares.modules.catalogo.application.dto.DisponibilidadResponse;
import com.ventacelulares.modules.catalogo.application.dto.IngresoStockRequest;
import com.ventacelulares.modules.catalogo.application.dto.IngresoStockResponse;
import com.ventacelulares.modules.catalogo.application.dto.ModeloDetalleResponse;
import com.ventacelulares.modules.catalogo.application.dto.ModeloResumenResponse;
import com.ventacelulares.modules.catalogo.domain.CatalogoRepository;
import com.ventacelulares.modules.catalogo.domain.CorreoAvisoPort;
import com.ventacelulares.modules.catalogo.domain.Modelo;
import com.ventacelulares.modules.catalogo.domain.StockSucursal;
import com.ventacelulares.modules.catalogo.domain.Variante;
import com.ventacelulares.shared.domain.Sucursal;
import com.ventacelulares.shared.exception.BusinessRuleException;
import com.ventacelulares.shared.exception.ConflictException;
import com.ventacelulares.shared.exception.NotFoundException;
import com.ventacelulares.shared.security.CurrentUser;
import com.ventacelulares.shared.security.Roles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class CatalogoService {
    private final CatalogoRepository repository;
    private final CorreoAvisoPort correo;
    private final CurrentUser currentUser;
    private final Clock clock;

    public CatalogoService(CatalogoRepository repository, CorreoAvisoPort correo, CurrentUser currentUser, Clock clock) {
        this.repository = repository;
        this.correo = correo;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    public CatalogoPageResponse listModels(CatalogoFiltro filtro) {
        if (filtro.page() < 0 || filtro.size() < 1 || filtro.size() > 50) {
            throw new BusinessRuleException("BR-CATALOG-01", "La página o el tamaño solicitado no es válido");
        }
        if ((filtro.precioMinimo() != null && filtro.precioMinimo().signum() < 0)
                || (filtro.precioMaximo() != null && filtro.precioMaximo().signum() < 0)
                || (filtro.precioMinimo() != null && filtro.precioMaximo() != null && filtro.precioMinimo().compareTo(filtro.precioMaximo()) > 0)) {
            throw new BusinessRuleException("BR-CATALOG-02", "Ingresá un rango de precios válido");
        }
        verificarSucursal(filtro.sucursal());
        List<ModeloResumenResponse> resultados = new ArrayList<>();
        for (Modelo modelo : repository.modelos()) {
            if (filtro.marca() != null && !filtro.marca().isBlank() && !modelo.marca().equalsIgnoreCase(filtro.marca().trim())) {
                continue;
            }
            if (filtro.condicion() != null && filtro.condicion() != modelo.condicion()) {
                continue;
            }
            for (Variante variante : modelo.variantes()) {
                BigDecimal precio = variante.precioContado().amount();
                if ((filtro.memoriaGb() != null && variante.memoriaGb() != filtro.memoriaGb())
                        || (filtro.precioMinimo() != null && precio.compareTo(filtro.precioMinimo()) < 0)
                        || (filtro.precioMaximo() != null && precio.compareTo(filtro.precioMaximo()) > 0)) {
                    continue;
                }
                List<StockSucursal> stock = stockVisible(variante.sku());
                if (filtro.sucursal() != null && stock.stream().noneMatch(s -> s.sucursal() == filtro.sucursal() && s.disponibles() > 0)) {
                    continue;
                }
                int cantidad = stock.stream().mapToInt(StockSucursal::disponibles).sum();
                resultados.add(new ModeloResumenResponse(modelo.id(), modelo.marca(), modelo.nombre(), modelo.condicion(), modelo.grado(), modelo.fotoUrl(), variante.sku(), variante.color(), variante.memoriaGb(), precio, cuota(precio), cuotas(), estado(cantidad), (int) stock.stream().filter(s -> s.disponibles() > 0).count(), mostrarCantidades() ? cantidad : null));
            }
        }
        long inicio = (long) filtro.page() * filtro.size();
        List<ModeloResumenResponse> contenido = inicio >= resultados.size() ? List.of() : resultados.subList((int) inicio, (int) Math.min(resultados.size(), inicio + filtro.size()));
        return new CatalogoPageResponse(contenido, filtro.page(), filtro.size(), resultados.size(), (int) ((resultados.size() + (long) filtro.size() - 1) / filtro.size()));
    }

    public ModeloDetalleResponse getModelDetail(String modeloId) {
        Modelo modelo = repository.modelo(modeloId).orElseThrow(() -> new NotFoundException("Modelo inexistente"));
        List<ModeloDetalleResponse.VarianteDetalle> variantes = modelo.variantes().stream().map(variante -> {
            List<StockSucursal> stock = stockVisible(variante.sku());
            int total = stock.stream().mapToInt(StockSucursal::disponibles).sum();
            List<DisponibilidadResponse> sucursales = stock.stream().map(s -> new DisponibilidadResponse(s.sucursal(), s.disponibles() > 0, mostrarCantidades() ? s.disponibles() : null)).toList();
            return new ModeloDetalleResponse.VarianteDetalle(variante.sku(), variante.color(), variante.memoriaGb(), variante.precioContado().amount(), cuota(variante.precioContado().amount()), cuotas(), estado(total), sucursales);
        }).toList();
        return new ModeloDetalleResponse(modelo.id(), modelo.marca(), modelo.nombre(), modelo.condicion(), modelo.grado(), modelo.fotoUrl(), variantes);
    }

    public AvisoResponse createStockAlert(CrearAvisoRequest request) {
        Modelo modelo = repository.modelo(request.modeloId()).orElseThrow(() -> new NotFoundException("Modelo inexistente"));
        Variante variante = modelo.variantes().stream().filter(v -> v.sku().equals(request.sku())).findFirst()
                .orElseThrow(() -> new NotFoundException("La variante no pertenece al modelo"));
        synchronized (repository) {
            if (repository.stock(variante.sku()).stream().anyMatch(s -> s.disponibles() > 0)) {
                throw new ConflictException("Esta variante ya tiene stock disponible");
            }
            repository.guardarAviso(variante.sku(), request.correo());
        }
        return new AvisoResponse(variante.sku(), request.correo(), "Aviso creado para esta variante");
    }

    public IngresoStockResponse registerStockArrival(IngresoStockRequest request) {
        verificarSucursal(request.sucursal());
        Modelo modelo = repository.modelos().stream().filter(m -> m.variantes().stream().anyMatch(v -> v.sku().equals(request.sku())))
                .findFirst().orElseThrow(() -> new NotFoundException("Variante inexistente"));
        Variante variante = modelo.variantes().stream().filter(v -> v.sku().equals(request.sku())).findFirst().orElseThrow();
        int enviados = 0;
        int rechazados = 0;
        int errores = 0;
        synchronized (repository) {
            repository.registrarIngreso(request.sku(), request.sucursal(), request.cantidad());
            for (CatalogoRepository.AvisoStock aviso : repository.avisosPendientes(request.sku())) {
                try {
                    CorreoAvisoPort.ResultadoEnvio resultado = correo.enviar(aviso.correo(), modelo.nombre(), modelo.condicion(), variante.color(), variante.memoriaGb());
                    if (resultado == CorreoAvisoPort.ResultadoEnvio.ACEPTADO) {
                        repository.marcarAvisoEnviado(aviso.id());
                        enviados++;
                    } else if (resultado == CorreoAvisoPort.ResultadoEnvio.RECHAZADO) {
                        rechazados++;
                    } else {
                        errores++;
                    }
                } catch (RuntimeException ex) {
                    errores++;
                }
            }
        }
        return new IngresoStockResponse(request.sku(), request.cantidad(), enviados, rechazados, errores);
    }

    private List<StockSucursal> stockVisible(String sku) {
        List<StockSucursal> stock = repository.stock(sku);
        if (Roles.VENDEDOR.equals(currentUser.role()) || Roles.SUPERVISOR.equals(currentUser.role())) {
            Sucursal sucursal = currentUser.sucursal().orElseThrow(() -> new AccessDeniedException("Sucursal no asignada"));
            return stock.stream().filter(s -> s.sucursal() == sucursal).toList();
        }
        return stock;
    }

    private void verificarSucursal(Sucursal sucursal) {
        if (sucursal != null && (Roles.VENDEDOR.equals(currentUser.role()) || Roles.SUPERVISOR.equals(currentUser.role()))
                && currentUser.sucursal().filter(sucursal::equals).isEmpty()) {
            throw new AccessDeniedException("No puede operar sobre otra sucursal");
        }
    }

    private boolean mostrarCantidades() {
        return !Roles.CLIENTE.equals(currentUser.role());
    }

    private String estado(int cantidad) {
        return cantidad == 0 ? "Sin stock" : cantidad <= 3 ? "Últimas unidades" : "En stock";
    }

    private int cuotas() {
        // Promoción dummy vigente los lunes para el banco de ejemplo; reemplazar por promociones reales.
        return LocalDate.now(clock).getDayOfWeek() == DayOfWeek.MONDAY ? 6 : 3;
    }

    private BigDecimal cuota(BigDecimal contado) {
        BigDecimal total = cuotas() == 6 ? contado : contado.multiply(new BigDecimal("1.15"));
        return total.divide(BigDecimal.valueOf(cuotas()), 2, RoundingMode.HALF_EVEN);
    }
}
