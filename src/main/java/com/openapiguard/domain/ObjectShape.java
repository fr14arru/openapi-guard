package com.openapiguard.domain;

import java.util.Map;
import java.util.Set;

/**
 * Propiedades proyectadas por dirección, required y apertura a nombres adicionales.
 * accessModes conserva flags originales; las colecciones se copian sin probar satisfacibilidad.
 * @param properties esquemas de propiedades tras proyección de acceso
 * @param required nombres obligatorios tras proyectar dirección
 * @param additionalProperties si se admiten nombres no declarados; solo faceta booleana
 * @param accessModes modos de acceso originales para conservar evidencia de proyección
 */
public record ObjectShape(Map<String, SchemaValueContract> properties, Set<String> required, boolean additionalProperties,
                          Map<String, String> accessModes) {
    /** Copia propiedades, required y modos de acceso sin probar satisfacibilidad.
     * @param properties esquemas de propiedades tras proyección de acceso
     * @param required nombres obligatorios tras proyectar dirección
     * @param additionalProperties si se admiten nombres no declarados; solo faceta booleana
     */
    public ObjectShape(Map<String, SchemaValueContract> properties, Set<String> required, boolean additionalProperties) {
        this(properties, required, additionalProperties, Map.of());
    }
    /** Copia propiedades, required y modos de acceso sin probar satisfacibilidad.
     * @param properties esquemas de propiedades tras proyección de acceso
     * @param required nombres obligatorios tras proyectar dirección
     * @param additionalProperties si se admiten nombres no declarados; solo faceta booleana
     * @param accessModes modos de acceso originales para conservar evidencia de proyección
     */
    public ObjectShape {
        properties = Map.copyOf(properties);
        required = Set.copyOf(required);
        accessModes = Map.copyOf(accessModes);
    }
}
