package com.openapiguard.domain;

import java.util.Objects;

/** Identidad literal: ruta con slash inicial y método, ambos no nulos.
 * @param path ruta literal del contrato, sin inferir equivalencia de plantillas
 * @param method método HTTP no nulo que completa la identidad de operación
 */
public record OperationId(String path, HttpMethod method) {
    /** Exige método no nulo y ruta literal con slash inicial.
     * @param path ruta literal del contrato, sin inferir equivalencia de plantillas
     * @param method método HTTP no nulo que completa la identidad de operación
     */
    public OperationId {
        Objects.requireNonNull(path);
        Objects.requireNonNull(method);
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Operation path must start with '/'");
        }
    }
}
