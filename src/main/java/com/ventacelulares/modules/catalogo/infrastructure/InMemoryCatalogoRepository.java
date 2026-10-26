package com.ventacelulares.modules.catalogo.infrastructure;

import com.ventacelulares.modules.catalogo.domain.CatalogoRepository;
import com.ventacelulares.modules.catalogo.domain.Condicion;
import com.ventacelulares.modules.catalogo.domain.Modelo;
import com.ventacelulares.modules.catalogo.domain.StockSucursal;
import com.ventacelulares.modules.catalogo.domain.Variante;
import com.ventacelulares.shared.domain.Money;
import com.ventacelulares.shared.domain.Sucursal;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryCatalogoRepository implements CatalogoRepository {
    // Datos dummy y almacenamiento en memoria: reemplazar por PostgreSQL.
    private final List<Modelo> modelos = List.of(
            new Modelo("galaxy-a55", "Samsung", "Galaxy A55", Condicion.NUEVO, null, "/images/galaxy-a55.jpg", List.of(
                    new Variante("SAM-A55-BLK-128", "Negro", 128, new Money(new BigDecimal("689999.00"))),
                    new Variante("SAM-A55-BLU-256", "Azul", 256, new Money(new BigDecimal("789999.00"))))),
            new Modelo("moto-g84", "Motorola", "Moto G84", Condicion.NUEVO, null, "/images/moto-g84.jpg", List.of(
                    new Variante("MOT-G84-BLU-256", "Azul", 256, new Money(new BigDecimal("459999.00"))))),
            new Modelo("iphone-13-reacondicionado", "Apple", "iPhone 13", Condicion.REACONDICIONADO, "A", "/images/iphone-13.jpg", List.of(
                    new Variante("APP-IP13-BLK-128-A", "Negro", 128, new Money(new BigDecimal("949999.00"))))),
            new Modelo("redmi-note-13", "Xiaomi", "Redmi Note 13", Condicion.NUEVO, null, "/images/redmi-note-13.jpg", List.of(
                    new Variante("XIA-RN13-GRN-128", "Verde", 128, new Money(new BigDecimal("359999.00"))))));
    private final Map<String, Map<Sucursal, StockSucursal>> existencias = new HashMap<>();
    private final Map<Long, AvisoStock> pendientes = new HashMap<>();
    private long siguienteId = 1;

    public InMemoryCatalogoRepository() {
        for (Modelo modelo : modelos) {
            for (Variante variante : modelo.variantes()) {
                Map<Sucursal, StockSucursal> porSucursal = new HashMap<>();
                for (Sucursal sucursal : Sucursal.values()) {
                    int unidades = variante.sku().equals("XIA-RN13-GRN-128") ? 0 : (sucursal == Sucursal.PALERMO ? 4 : sucursal == Sucursal.BELGRANO ? 2 : 0);
                    porSucursal.put(sucursal, new StockSucursal(sucursal, unidades, 0, 0));
                }
                existencias.put(variante.sku(), porSucursal);
            }
        }
    }

    @Override
    public List<Modelo> modelos() {
        return modelos;
    }

    @Override
    public Optional<Modelo> modelo(String id) {
        return modelos.stream().filter(modelo -> modelo.id().equals(id)).findFirst();
    }

    @Override
    public synchronized List<StockSucursal> stock(String sku) {
        Map<Sucursal, StockSucursal> porSucursal = existencias.get(sku);
        if (porSucursal == null) {
            return List.of();
        }
        List<StockSucursal> resultado = new ArrayList<>();
        for (Sucursal sucursal : Sucursal.values()) {
            resultado.add(porSucursal.get(sucursal));
        }
        return List.copyOf(resultado);
    }

    @Override
    public synchronized void registrarIngreso(String sku, Sucursal sucursal, int cantidad) {
        Map<Sucursal, StockSucursal> porSucursal = existencias.get(sku);
        if (porSucursal == null) {
            throw new IllegalArgumentException("SKU inexistente");
        }
        StockSucursal actual = porSucursal.get(sucursal);
        porSucursal.put(sucursal, new StockSucursal(sucursal, actual.unidades() + cantidad, actual.reservadas(), actual.enTransito()));
    }

    @Override
    public synchronized void guardarAviso(String sku, String correo) {
        pendientes.put(siguienteId, new AvisoStock(siguienteId, sku, correo));
        siguienteId++;
    }

    @Override
    public synchronized List<AvisoStock> avisosPendientes(String sku) {
        return pendientes.values().stream().filter(aviso -> aviso.sku().equals(sku)).sorted((a, b) -> Long.compare(a.id(), b.id())).toList();
    }

    @Override
    public synchronized void marcarAvisoEnviado(long avisoId) {
        pendientes.remove(avisoId);
    }
}
