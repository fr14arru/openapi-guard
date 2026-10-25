package com.openapiguard.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** Identidad escalar JSON; canonización BigDecimal hace equivalentes 1 y 1.0. No representa objetos/arrays.
 * @param kind tipo escalar normalizado; números se conservan en representación decimal
 * @param value valor escalar normalizado
 */
public record ScalarValue(String kind, String value) {
    /** Valida tipo escalar y canoniza números con BigDecimal.
     * @param kind tipo escalar normalizado; números se conservan en representación decimal
     * @param value valor escalar normalizado
     */
    public ScalarValue {
        Objects.requireNonNull(kind);
        Objects.requireNonNull(value);
        if (!java.util.Set.of("string", "number", "boolean", "null").contains(kind))
            throw new IllegalArgumentException("Unsupported scalar kind: " + kind);
        if (kind.equals("number")) value = new BigDecimal(value).stripTrailingZeros().toString();
    }
    /** Comprueba si la representación numérica tiene valor entero.
     * @return true si el número tiene valor entero, con independencia de su notación
     */
    public boolean integer() { return kind.equals("number") && new BigDecimal(value).stripTrailingZeros().scale() <= 0; }
}
