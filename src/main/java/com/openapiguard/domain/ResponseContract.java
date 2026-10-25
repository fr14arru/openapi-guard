package com.openapiguard.domain;

import java.util.Map;

/** Formato normalizado a pointer original; mapa vacío representa respuesta sin cuerpo.
 * @param representations media types normalizados y pointers originales; vacío si no hay cuerpo
 * @param definitionPointer JSON Pointer original de la definición normalizada
 * @param usagePointer JSON Pointer del uso, incluso si la definición está en components
 */
public record ResponseContract(Map<String, String> representations, String definitionPointer, String usagePointer) {
    /** Copia representaciones para aislar la instantánea de mutaciones externas.
     * @param representations media types normalizados y pointers originales; vacío si no hay cuerpo
     * @param definitionPointer JSON Pointer original de la definición normalizada
     * @param usagePointer JSON Pointer del uso, incluso si la definición está en components
     */
    public ResponseContract { representations = Map.copyOf(representations); }
}
