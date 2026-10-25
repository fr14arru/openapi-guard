package com.openapiguard.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Restricciones conjuntas normalizadas del perfil y lagunas explícitas.
 * Enum nulo significa sin enum; vacío puede resultar de una intersección contradictoria.
 * No valida OpenAPI: el adaptador valida entrada y el dominio decide inclusión.
 * @param types tipos permitidos conjuntamente con el resto de restricciones
 * @param enumValues valores escalares permitidos; null si no hay enum, vacío si es contradictorio
 * @param minimum extremo numérico exacto; null cuando no está acotado
 * @param maximum extremo numérico exacto; null cuando no está acotado
 * @param minLength límite de longitud en puntos de código Unicode
 * @param maxLength límite de longitud en puntos de código Unicode
 * @param unsupported construcciones opacas conservadas como lagunas de cobertura
 * @param definitionPointer JSON Pointer original de la definición normalizada
 * @param objectShape restricciones de objeto proyectadas; null cuando no se normalizan
 * @param readOnly anotación original de acceso, interpretada según dirección
 * @param writeOnly anotación original de acceso, interpretada según dirección
 * @param arrayShape restricciones de array; null cuando no se normalizan
 */
public record SchemaValueContract(Set<String> types, List<ScalarValue> enumValues, Bound minimum, Bound maximum,
                                  int minLength, int maxLength, List<String> unsupported, String definitionPointer,
                                  ObjectShape objectShape, boolean readOnly, boolean writeOnly, ArrayShape arrayShape) {
    /** Copia tipos y enums, ordena lagunas y conserva restricciones exactas.
     * @param types tipos permitidos conjuntamente con el resto de restricciones
     * @param enumValues valores escalares permitidos; null si no hay enum, vacío si es contradictorio
     * @param minimum extremo numérico exacto; null cuando no está acotado
     * @param maximum extremo numérico exacto; null cuando no está acotado
     * @param minLength límite de longitud en puntos de código Unicode
     * @param maxLength límite de longitud en puntos de código Unicode
     * @param unsupported construcciones opacas conservadas como lagunas de cobertura
     * @param definitionPointer JSON Pointer original de la definición normalizada
     * @param objectShape restricciones de objeto proyectadas; null cuando no se normalizan
     * @param readOnly anotación original de acceso, interpretada según dirección
     * @param writeOnly anotación original de acceso, interpretada según dirección
     */
    public SchemaValueContract(Set<String> types, List<ScalarValue> enumValues, Bound minimum, Bound maximum,
                               int minLength, int maxLength, List<String> unsupported, String definitionPointer,
                               ObjectShape objectShape, boolean readOnly, boolean writeOnly) {
        this(types, enumValues, minimum, maximum, minLength, maxLength, unsupported, definitionPointer,
                objectShape, readOnly, writeOnly, null);
    }
    /** Copia tipos y enums, ordena lagunas y conserva restricciones exactas.
     * @param types tipos permitidos conjuntamente con el resto de restricciones
     * @param enumValues valores escalares permitidos; null si no hay enum, vacío si es contradictorio
     * @param minimum extremo numérico exacto; null cuando no está acotado
     * @param maximum extremo numérico exacto; null cuando no está acotado
     * @param minLength límite de longitud en puntos de código Unicode
     * @param maxLength límite de longitud en puntos de código Unicode
     * @param unsupported construcciones opacas conservadas como lagunas de cobertura
     * @param definitionPointer JSON Pointer original de la definición normalizada
     */
    public SchemaValueContract(Set<String> types, List<ScalarValue> enumValues, Bound minimum, Bound maximum,
                               int minLength, int maxLength, List<String> unsupported, String definitionPointer) {
        this(types, enumValues, minimum, maximum, minLength, maxLength, unsupported, definitionPointer, null, false, false);
    }
    /** Extremo decimal exacto y exclusividad; un Bound ausente significa extremo no acotado.
     * @param value valor decimal exacto del extremo
     * @param exclusive si el extremo excluye su propio valor
     */
    public record Bound(BigDecimal value, boolean exclusive) {}
    /** Copia tipos y enums, ordena lagunas y conserva restricciones exactas.
     * @param types tipos permitidos conjuntamente con el resto de restricciones
     * @param enumValues valores escalares permitidos; null si no hay enum, vacío si es contradictorio
     * @param minimum extremo numérico exacto; null cuando no está acotado
     * @param maximum extremo numérico exacto; null cuando no está acotado
     * @param minLength límite de longitud en puntos de código Unicode
     * @param maxLength límite de longitud en puntos de código Unicode
     * @param unsupported construcciones opacas conservadas como lagunas de cobertura
     * @param definitionPointer JSON Pointer original de la definición normalizada
     * @param objectShape restricciones de objeto proyectadas; null cuando no se normalizan
     * @param readOnly anotación original de acceso, interpretada según dirección
     * @param writeOnly anotación original de acceso, interpretada según dirección
     * @param arrayShape restricciones de array; null cuando no se normalizan
     */
    public SchemaValueContract {
        types = Set.copyOf(types);
        enumValues = enumValues == null ? null : List.copyOf(enumValues);
        unsupported = unsupported.stream().distinct().sorted().toList();
    }

    /** Comprueba enum y restricciones escalares; no valida objetos ni arrays JSON.
     * @param value valor escalar normalizado
     * @return true si cumple enum y todas las restricciones escalares soportadas
     */
    public boolean accepts(ScalarValue value) {
        return (enumValues == null || enumValues.contains(value)) && acceptsConstraints(value);
    }

    /** Comprueba tipo/límites conjuntamente, contando puntos de código Unicode para strings.
     * @param value valor escalar normalizado
     * @return true si cumple tipo y límites escalares, sin consultar el enum
     */
    public boolean acceptsConstraints(ScalarValue value) {
        boolean typed = types.contains(value.kind()) || value.integer() && types.contains("integer");
        if (!typed) return false;
        if (value.kind().equals("number")) {
            BigDecimal number = new BigDecimal(value.value());
            if (minimum != null && (number.compareTo(minimum.value()) < 0 || minimum.exclusive() && number.compareTo(minimum.value()) == 0)) return false;
            if (maximum != null && (number.compareTo(maximum.value()) > 0 || maximum.exclusive() && number.compareTo(maximum.value()) == 0)) return false;
        }
        if (value.kind().equals("string")) {
            int length = value.value().codePointCount(0, value.value().length());
            return length >= minLength && length <= maxLength;
        }
        return true;
    }
}
