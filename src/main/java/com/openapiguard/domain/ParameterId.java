package com.openapiguard.domain;

import java.util.Locale;
import java.util.Set;

/** Identidad por nombre y ubicación; solo nombres de header se canonizan a minúsculas.
 * @param name nombre, normalizado sin distinguir mayúsculas solo para headers
 * @param in ubicación OpenAPI del parámetro
 */
public record ParameterId(String name, String in) implements Comparable<ParameterId> {
    /** Valida ubicación y nombre; normaliza headers para identidad y orden estables.
     * @param name nombre, normalizado sin distinguir mayúsculas solo para headers
     * @param in ubicación OpenAPI del parámetro
     */
    public ParameterId {
        if (name == null || name.isBlank() || !Set.of("path", "query", "header", "cookie").contains(in)) {
            throw new IllegalArgumentException("Invalid parameter identity");
        }
        if (in.equals("header")) {
            name = name.toLowerCase(Locale.ROOT);
        }
    }

    /** Orden estable por ubicación y nombre normalizado.
     * @param other identidad con la que establecer el orden determinista
     * @return orden por ubicación y nombre normalizados
     */
    @Override
    public int compareTo(ParameterId other) {
        int locationOrder = in.compareTo(other.in);
        return locationOrder == 0 ? name.compareTo(other.name) : locationOrder;
    }
}
