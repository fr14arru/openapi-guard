package com.openapiguard.domain;

import java.util.Objects;

/**
 * Parámetro efectivo tras herencia/override, sin AST del parser.
 * Conserva definición/uso y firma; presenceSupported solo describe soporte de presencia.
 * Path debe ser obligatorio; identidad, pointers y firma deben ser no nulos.
 * @param id identidad semántica estable
 * @param required obligatoriedad efectiva declarada
 * @param definitionPointer JSON Pointer original de la definición normalizada
 * @param usagePointer JSON Pointer del uso, incluso si la definición está en components
 * @param serialization firma de serialización conservada para detectar incertidumbre
 * @param presenceSupported soporte de la faceta de presencia; no implica soporte completo del parámetro
 */
public record EffectiveParameter(ParameterId id, boolean required, String definitionPointer,
                                 String usagePointer, String serialization, boolean presenceSupported) {
    /** Valida identidad y procedencia; los parámetros path deben ser obligatorios.
     * @param id identidad semántica estable
     * @param required obligatoriedad efectiva declarada
     * @param definitionPointer JSON Pointer original de la definición normalizada
     * @param usagePointer JSON Pointer del uso, incluso si la definición está en components
     * @param serialization firma de serialización conservada para detectar incertidumbre
     * @param presenceSupported soporte de la faceta de presencia; no implica soporte completo del parámetro
     */
    public EffectiveParameter {
        Objects.requireNonNull(id);
        Objects.requireNonNull(definitionPointer);
        Objects.requireNonNull(usagePointer);
        Objects.requireNonNull(serialization);
        if (id.in().equals("path") && !required) {
            throw new IllegalArgumentException("Path parameters must be required");
        }
    }
}
