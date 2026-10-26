package com.ventacelulares.shared.domain;

public enum Sucursal {
    PALERMO("PAL", "Palermo"),
    BELGRANO("BEL", "Belgrano"),
    CABALLITO("CAB", "Caballito"),
    RECOLETA("REC", "Recoleta"),
    MICROCENTRO("MIC", "Microcentro"),
    SAN_ISIDRO("SID", "San Isidro"),
    LA_PLATA("LPL", "La Plata");

    private final String code;
    private final String displayName;

    Sucursal(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }
}
