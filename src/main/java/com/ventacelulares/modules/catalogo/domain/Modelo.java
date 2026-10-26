package com.ventacelulares.modules.catalogo.domain;

import java.util.List;

public record Modelo(String id, String marca, String nombre, Condicion condicion, String grado, String fotoUrl, List<Variante> variantes) {
    public Modelo {
        variantes = List.copyOf(variantes);
    }
}
