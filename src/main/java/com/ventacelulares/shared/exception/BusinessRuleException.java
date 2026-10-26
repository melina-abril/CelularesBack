package com.ventacelulares.shared.exception;

import java.util.Objects;

public class BusinessRuleException extends RuntimeException {
    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "El código de regla es obligatorio");
    }

    public String code() {
        return code;
    }

    public String getCode() {
        return code;
    }
}
