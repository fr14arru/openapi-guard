package com.openapiguard.domain;

import java.util.List;

/** Cobertura pública versionada; capacidades explícitas conservan la semántica de perfiles históricos. */
public enum ComparisonProfile {
    /** Disponibilidad de operaciones. */
    OPERATIONS_ONLY("operations-only-v1", List.of("parameters", "requestSchemas", "responseSchemas",
            "responseCodes", "mediaTypes", "security", "servers", "callbacks", "webhooks")),
    /** Operaciones y presencia de parámetros efectivos. */
    PARAMETER_PRESENCE("operations-parameter-presence-v1", List.of("parameterSchemas", "parameterSerialization",
            "requestSchemas", "responseSchemas", "responseCodes", "mediaTypes", "security", "servers", "callbacks", "webhooks")),
    /** Añade presencia y media types de petición. */
    REQUEST_BODY("operations-parameters-request-body-v1", List.of("parameterSchemas", "parameterSerialization",
            "requestSchemas", "responseSchemas", "responseCodes", "responseMediaTypes", "security", "servers", "callbacks", "webhooks")),
    /** Añade tipos, enums y restricciones escalares por dirección. */
    SCHEMA_VALUES("operations-parameters-schema-values-v1", List.of("parameterSerialization", "formatAssertions",
            "requestEncoding", "responseCodes", "responseMediaNegotiation", "security", "servers", "callbacks", "webhooks")),
    /** Añade propiedades, obligatoriedad y apertura de objetos. */
    OBJECT_SCHEMAS("operations-parameters-object-schemas-v1", List.of("parameterSerialization", "formatAssertions",
            "requestEncoding", "responseCodes", "responseMediaNegotiation", "security", "servers", "callbacks", "webhooks")),
    /** Añade items y límites de arrays. */
    ARRAY_SCHEMAS("operations-parameters-array-schemas-v1", List.of("parameterSerialization", "formatAssertions",
            "requestEncoding", "responseCodes", "responseMediaNegotiation", "security", "servers", "callbacks", "webhooks")),
    /** Cobertura full: añade disponibilidad de respuestas. */
    RESPONSE_POLICY("operations-data-response-policy-v1", List.of("parameterSerialization", "formatAssertions",
            "requestEncoding", "responseNegotiation", "responseHeaders", "responseLinks", "security", "servers", "callbacks", "webhooks"));

    private final String id;
    private final List<String> excludedFacets;

    ComparisonProfile(String id, List<String> excludedFacets) {
        this.id = id;
        this.excludedFacets = excludedFacets;
    }

    /** Expone una capacidad o identificador de este perfil versionado.
     * @return identificador estable del perfil
     */
    public String id() { return id; }
    /** Expone una capacidad o identificador de este perfil versionado.
     * @return facetas excluidas, sin confundirlas con errores de entrada
     */
    public List<String> excludedFacets() { return excludedFacets; }
    /** Expone una capacidad o identificador de este perfil versionado.
     * @return true si el perfil compara presencia y formatos de petición
     */
    public boolean includesRequestBodies() { return this != OPERATIONS_ONLY && this != PARAMETER_PRESENCE; }
    /** Expone una capacidad o identificador de este perfil versionado.
     * @return true si incluye restricciones de valores
     */
    public boolean includesSchemas() { return this == SCHEMA_VALUES || includesObjects(); }
    /** Expone una capacidad o identificador de este perfil versionado.
     * @return true si incluye propiedades y apertura de objetos
     */
    public boolean includesObjects() { return this == OBJECT_SCHEMAS || includesArrays(); }
    /** Expone una capacidad o identificador de este perfil versionado.
     * @return true si incluye items y límites de arrays
     */
    public boolean includesArrays() { return this == ARRAY_SCHEMAS || includesResponsePolicy(); }
    /** Expone una capacidad o identificador de este perfil versionado.
     * @return true si incluye disponibilidad de códigos y formatos de respuesta
     */
    public boolean includesResponsePolicy() { return this == RESPONSE_POLICY; }
}
