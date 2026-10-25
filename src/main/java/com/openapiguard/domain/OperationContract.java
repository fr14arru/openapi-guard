package com.openapiguard.domain;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Snapshot de facetas del perfil, con colecciones y mapas anidados copiados.
 * Las sobrecargas rellenan facetas posteriores con mapas vacíos.
 * La validez OpenAPI procede del adaptador, no de construir este record a mano.
 * @param openApiVersion dialecto declarado en la entrada
 * @param paths métodos disponibles por ruta literal
 * @param parameters parámetros efectivos por operación, incluyendo herencia y overrides
 * @param requestBodies presencia y formatos de petición por operación
 * @param schemas restricciones por uso semántico y dirección
 * @param responses respuestas documentadas por operación y código
 */
public record OperationContract(String openApiVersion, Map<String, Set<HttpMethod>> paths,
                                Map<OperationId, Map<ParameterId, EffectiveParameter>> parameters,
                                Map<OperationId, RequestBodyContract> requestBodies,
                                Map<SchemaUseId, SchemaUse> schemas,
                                Map<OperationId, Map<String, ResponseContract>> responses) {
    /** Copia facetas disponibles; las sobrecargas rellenan facetas posteriores con mapas vacíos.
     * @param openApiVersion dialecto declarado en la entrada
     * @param paths métodos disponibles por ruta literal
     * @param parameters parámetros efectivos por operación, incluyendo herencia y overrides
     * @param requestBodies presencia y formatos de petición por operación
     * @param schemas restricciones por uso semántico y dirección
     */
    public OperationContract(String openApiVersion, Map<String, Set<HttpMethod>> paths,
                             Map<OperationId, Map<ParameterId, EffectiveParameter>> parameters,
                             Map<OperationId, RequestBodyContract> requestBodies,
                             Map<SchemaUseId, SchemaUse> schemas) {
        this(openApiVersion, paths, parameters, requestBodies, schemas, Map.of());
    }
    /** Copia facetas disponibles; las sobrecargas rellenan facetas posteriores con mapas vacíos.
     * @param openApiVersion dialecto declarado en la entrada
     * @param paths métodos disponibles por ruta literal
     * @param parameters parámetros efectivos por operación, incluyendo herencia y overrides
     * @param requestBodies presencia y formatos de petición por operación
     */
    public OperationContract(String openApiVersion, Map<String, Set<HttpMethod>> paths,
                             Map<OperationId, Map<ParameterId, EffectiveParameter>> parameters,
                             Map<OperationId, RequestBodyContract> requestBodies) {
        this(openApiVersion, paths, parameters, requestBodies, Map.of());
    }
    /** Copia facetas disponibles; las sobrecargas rellenan facetas posteriores con mapas vacíos.
     * @param openApiVersion dialecto declarado en la entrada
     * @param paths métodos disponibles por ruta literal
     * @param parameters parámetros efectivos por operación, incluyendo herencia y overrides
     */
    public OperationContract(String openApiVersion, Map<String, Set<HttpMethod>> paths,
                             Map<OperationId, Map<ParameterId, EffectiveParameter>> parameters) {
        this(openApiVersion, paths, parameters, Map.of());
    }
    /** Copia facetas disponibles; las sobrecargas rellenan facetas posteriores con mapas vacíos.
     * @param openApiVersion dialecto declarado en la entrada
     * @param paths métodos disponibles por ruta literal
     */
    public OperationContract(String openApiVersion, Map<String, Set<HttpMethod>> paths) {
        this(openApiVersion, paths, Map.of());
    }

    /** Copia facetas disponibles; las sobrecargas rellenan facetas posteriores con mapas vacíos.
     * @param openApiVersion dialecto declarado en la entrada
     * @param paths métodos disponibles por ruta literal
     * @param parameters parámetros efectivos por operación, incluyendo herencia y overrides
     * @param requestBodies presencia y formatos de petición por operación
     * @param schemas restricciones por uso semántico y dirección
     * @param responses respuestas documentadas por operación y código
     */
    public OperationContract {
        paths = paths.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> Set.copyOf(entry.getValue())));
        parameters = parameters.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> Map.copyOf(entry.getValue())));
        requestBodies = Map.copyOf(requestBodies);
        schemas = Map.copyOf(schemas);
        responses = responses.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> Map.copyOf(entry.getValue())));
    }
}
