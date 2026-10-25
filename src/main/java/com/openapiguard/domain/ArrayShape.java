package com.openapiguard.domain;

/** Array homogéneo: items nulo permite cualquier elemento; min/max cuentan elementos, no bytes.
 * @param items restricciones de elementos; null representa ausencia de restricción normalizada
 * @param minItems límite inclusivo de longitud de array
 * @param maxItems límite inclusivo de longitud de array
 */
public record ArrayShape(SchemaValueContract items, int minItems, int maxItems) {}
