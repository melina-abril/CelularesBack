package com.ventacelulares.shared.domain;

public record Imei(String value) {
    public Imei {
        if (value == null || !value.matches("[0-9]{15}") || !hasValidCheckDigit(value)) {
            throw new IllegalArgumentException("El IMEI debe tener 15 dígitos y un dígito verificador válido");
        }
    }

    private static boolean hasValidCheckDigit(String digits) {
        int sum = 0;
        for (int index = digits.length() - 1; index >= 0; index--) {
            int digit = digits.charAt(index) - '0';
            if ((digits.length() - 1 - index) % 2 == 1) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
        }
        return sum % 10 == 0;
    }
}
