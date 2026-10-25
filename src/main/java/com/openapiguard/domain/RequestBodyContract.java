package com.openapiguard.domain;

import java.util.Set;
import java.util.Objects;

/** Presencia y formatos del cuerpo con definición/uso; sus schemas son una faceta independiente.
 * @param required obligatoriedad efectiva declarada
 * @param mediaTypes formatos de cuerpo normalizados
 * @param definitionPointer JSON Pointer original de la definición normalizada
 * @param usagePointer JSON Pointer del uso, incluso si la definición está en components
 */
public record RequestBodyContract(boolean required, Set<String> mediaTypes, String definitionPointer,
                                  String usagePointer) {
    /** Copia formatos y exige procedencia de definición y uso no nula.
     * @param required obligatoriedad efectiva declarada
     * @param mediaTypes formatos de cuerpo normalizados
     * @param definitionPointer JSON Pointer original de la definición normalizada
     * @param usagePointer JSON Pointer del uso, incluso si la definición está en components
     */
    public RequestBodyContract {
        mediaTypes = Set.copyOf(mediaTypes);
        Objects.requireNonNull(definitionPointer);
        Objects.requireNonNull(usagePointer);
    }
}
