package com.openapiguard.domain;

/** Identidad semántica del uso, independiente de índices de parámetros y nombres físicos de components.
 * @param operation identidad por ruta y método de la operación
 * @param direction REQUEST o RESPONSE; determina la proyección y sentido de inclusión
 * @param parameter identidad del parámetro asociado; null para otros usos
 * @param responseCode código de respuesta asociado; null para peticiones
 * @param mediaType representación asociada al uso; null si no aplica
 */
public record SchemaUseId(OperationId operation, Direction direction, ParameterId parameter,
                          String responseCode, String mediaType) {
    /** Perspectiva del cliente del contrato anterior. */
    public enum Direction {
        /** El servidor nuevo debe admitir peticiones antiguas: old ⊆ new. */ REQUEST,
        /** El cliente antiguo debe admitir respuestas nuevas: new ⊆ old. */ RESPONSE
    }
}
