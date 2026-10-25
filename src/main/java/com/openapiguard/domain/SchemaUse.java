package com.openapiguard.domain;

/** Uso de schema y procedencia; representationIssue vacío indica ausencia de esa laguna.
 * @param schema restricciones normalizadas del fragmento soportado
 * @param usagePointer JSON Pointer del uso, incluso si la definición está en components
 * @param representationIssue laguna de representación; cadena vacía si no se detectó
 */
public record SchemaUse(SchemaValueContract schema, String usagePointer, String representationIssue) {}
