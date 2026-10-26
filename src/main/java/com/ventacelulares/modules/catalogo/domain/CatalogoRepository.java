package com.ventacelulares.modules.catalogo.domain;

import com.ventacelulares.shared.domain.Sucursal;
import java.util.List;
import java.util.Optional;

public interface CatalogoRepository {
    List<Modelo> modelos();
    Optional<Modelo> modelo(String id);
    List<StockSucursal> stock(String sku);
    void registrarIngreso(String sku, Sucursal sucursal, int cantidad);
    void guardarAviso(String sku, String correo);
    List<AvisoStock> avisosPendientes(String sku);
    void marcarAvisoEnviado(long avisoId);

    record AvisoStock(long id, String sku, String correo) {
    }
}
