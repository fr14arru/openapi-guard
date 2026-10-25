package com.openapiguard.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/** Ruptura probada con identidad semántica estable, regla, severidad, procedencia y explicación.
 * @param id identidad semántica estable
 * @param ruleId identificador público de la regla aplicada
 * @param severity severidad del hallazgo; ERROR para rupturas demostradas
 * @param location procedencia del hallazgo en definición y uso
 * @param description explicación de la conclusión para revisión humana
 */
public record DetectedChange(String id, String ruleId, Severity severity,
                             Location location, String description) {
    /** Severidad publicada actualmente para rupturas. */
    public enum Severity { /** Ruptura demostrada que bloquea la validación. */ ERROR }

    /** Definición y uso por documento; campos no aplicables nulos, pointer vacío para raíz.
     * @param path ruta literal del contrato, sin inferir equivalencia de plantillas
     * @param method método HTTP; null cuando el hallazgo afecta a la ruta completa
     * @param baselinePointer JSON Pointer de la definición en el origen; null si no aplica
     * @param candidatePointer JSON Pointer de la definición en el destino; null si no aplica
     * @param parameter identidad del parámetro asociado; null para otros usos
     * @param baselineUsagePointer JSON Pointer del uso en el contrato anterior; null si no aplica
     * @param candidateUsagePointer JSON Pointer del uso en el contrato propuesto; null si no aplica
     * @param direction REQUEST o RESPONSE; determina la proyección y sentido de inclusión
     * @param responseCode código de respuesta asociado; null para peticiones
     * @param mediaType representación asociada al uso; null si no aplica
     */
    public record Location(String path, HttpMethod method, String baselinePointer,
                           String candidatePointer, ParameterId parameter,
                           String baselineUsagePointer, String candidateUsagePointer,
                           String direction, String responseCode, String mediaType) {
        /** Conserva procedencia; las sobrecargas dejan null en los campos no aplicables.
         * @param path ruta literal del contrato, sin inferir equivalencia de plantillas
         * @param method método HTTP; null cuando el hallazgo afecta a la ruta completa
         * @param baselinePointer JSON Pointer de la definición en el origen; null si no aplica
         * @param candidatePointer JSON Pointer de la definición en el destino; null si no aplica
         * @param parameter identidad del parámetro asociado; null para otros usos
         * @param baselineUsagePointer JSON Pointer del uso en el contrato anterior; null si no aplica
         * @param candidateUsagePointer JSON Pointer del uso en el contrato propuesto; null si no aplica
         */
        public Location(String path, HttpMethod method, String baselinePointer, String candidatePointer,
                        ParameterId parameter, String baselineUsagePointer, String candidateUsagePointer) {
            this(path, method, baselinePointer, candidatePointer, parameter, baselineUsagePointer, candidateUsagePointer, null, null, null);
        }
        /** Conserva procedencia; las sobrecargas dejan null en los campos no aplicables.
         * @param path ruta literal del contrato, sin inferir equivalencia de plantillas
         * @param method método HTTP; null cuando el hallazgo afecta a la ruta completa
         * @param baselinePointer JSON Pointer de la definición en el origen; null si no aplica
         * @param candidatePointer JSON Pointer de la definición en el destino; null si no aplica
         */
        public Location(String path, HttpMethod method, String baselinePointer, String candidatePointer) {
            this(path, method, baselinePointer, candidatePointer, null, null, null);
        }
    }

    /** Identidad independiente del índice físico del parámetro y de su componente. */
    static DetectedChange parameterRequired(OperationId operation, EffectiveParameter before,
                                            EffectiveParameter after) {
        String rule = before == null ? "REQUIRED_PARAMETER_ADDED" : "PARAMETER_BECAME_REQUIRED";
        String identity = ComparisonProfile.PARAMETER_PRESENCE.id() + "\n" + rule + "\nREQUEST\n"
                + operation.path().length() + ":" + operation.path() + "\n" + operation.method() + "\n"
                + after.id().in() + "\n" + after.id().name().length() + ":" + after.id().name();
        String description = before == null
                ? "A required parameter was added: " + after.id().name() + " in " + after.id().in() + "."
                : "The parameter " + after.id().name() + " in " + after.id().in() + " becomes required.";
        return new DetectedChange(hash(identity), rule, Severity.ERROR,
                new Location(operation.path(), operation.method(), before == null ? null : before.definitionPointer(),
                        after.definitionPointer(), after.id(), before == null ? null : before.usagePointer(),
                        after.usagePointer()), description);
    }

    /** Mantiene identidad histórica; method nulo indica retirada del endpoint completo. */
    static DetectedChange removed(String path, HttpMethod method) {
        String ruleId = method == null ? "ENDPOINT_REMOVED" : "HTTP_METHOD_REMOVED";
        String pointer = "/paths/" + path.replace("~", "~0").replace("/", "~1");
        if (method != null) {
            pointer += "/" + method.name().toLowerCase(Locale.ROOT);
        }
        String identity = "operations-only-v1\n" + ruleId + "\n" + path + "\n" + method;
        String id = hash(identity);
        String description = method == null
                ? "The endpoint was removed: " + path + "."
                : "The HTTP method was removed: " + method + " from endpoint " + path + ".";
        return new DetectedChange(id, ruleId, Severity.ERROR,
                new Location(path, method, pointer, null), description);
    }

    /** Mantiene la versión histórica de presencia/cobertura incluso en perfiles ampliados. */
    static DetectedChange requestBody(OperationId operation, String rule, RequestBodyContract before,
                                     RequestBodyContract after, String mediaType) {
        String suffix = mediaType == null ? "" : "/content/" + mediaType.replace("~", "~0").replace("/", "~1");
        String identity = ComparisonProfile.REQUEST_BODY.id() + "\n" + rule + "\nREQUEST\n"
                + operation.path().length() + ":" + operation.path() + "\n" + operation.method() + "\n" + mediaType;
        String description = mediaType == null ? "The request body becomes required."
                : "The request no longer accepts media type " + mediaType + ".";
        return new DetectedChange(hash(identity), rule, Severity.ERROR,
                new Location(operation.path(), operation.method(), before == null ? null : before.definitionPointer() + suffix,
                        after == null ? null : after.definitionPointer() + (mediaType == null ? "" : "/content"), null,
                        before == null ? null : before.usagePointer(), after == null ? null : after.usagePointer()), description);
    }

    /** SHA-256 de identidad UTF-8, sin incorporar la descripción humana. */
    private static String hash(String identity) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(identity.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    /** Reetiqueta source/target como baseline/candidate al invertir la dirección de respuesta. */
    static DetectedChange schema(ComparisonProfile profile, SchemaUseId use, SchemaUse before, SchemaUse after, SchemaInclusion.Result result) {
        String rule = use.direction() == SchemaUseId.Direction.REQUEST ? "REQUEST_SCHEMA_VALUES_RESTRICTED" : "RESPONSE_SCHEMA_VALUES_EXPANDED";
        String identity = profile.id() + "\n" + rule + "\n"
                + token(use.operation().path()) + token(use.operation().method().name()) + token(use.direction().name())
                + token(use.parameter() == null ? null : use.parameter().in()) + token(use.parameter() == null ? null : use.parameter().name())
                + token(use.responseCode()) + token(use.mediaType());
        return new DetectedChange(hash(identity), rule, Severity.ERROR,
                new Location(use.operation().path(), use.operation().method(), use.direction() == SchemaUseId.Direction.REQUEST ? result.sourcePointer() : result.targetPointer(),
                        use.direction() == SchemaUseId.Direction.REQUEST ? result.targetPointer() : result.sourcePointer(),
                        use.parameter(), before.usagePointer(), after.usagePointer(), use.direction().name(), use.responseCode(), use.mediaType()),
                (use.direction() == SchemaUseId.Direction.REQUEST ? "The candidate request rejects values accepted by the baseline contract. "
                        : "The candidate response allows values rejected by the baseline contract. ") + result.reason()
                        + constraintChanges(before.schema(), after.schema(),
                        use.direction() == SchemaUseId.Direction.REQUEST ? result.sourcePointer() : result.targetPointer(),
                        use.direction() == SchemaUseId.Direction.REQUEST ? result.targetPointer() : result.sourcePointer()));
    }

    /**
     * Explica restricciones del nodo que ya refutó la inclusión; no infiere nuevas rupturas.
     * Las etiquetas siempre siguen baseline → candidate, incluso al invertir source/target.
     * Un nodo ausente/universal no se presenta como una definición escrita en el contrato.
     */
    private static String constraintChanges(SchemaValueContract baseline, SchemaValueContract candidate,
                                            String baselinePointer, String candidatePointer) {
        var before = schemaAt(baseline, baselinePointer);
        var after = schemaAt(candidate, candidatePointer);
        if (before == null || after == null) return "";
        var details = new java.util.ArrayList<String>();
        detail(details, "type", new java.util.TreeSet<>(before.types()), new java.util.TreeSet<>(after.types()));
        detail(details, "enum", enumText(before.enumValues()), enumText(after.enumValues()));
        detail(details, "minimum", boundText(before.minimum()), boundText(after.minimum()));
        detail(details, "maximum", boundText(before.maximum()), boundText(after.maximum()));
        detail(details, "minLength", before.minLength(), after.minLength());
        detail(details, "maxLength", lengthText(before.maxLength()), lengthText(after.maxLength()));
        if (before.arrayShape() != null && after.arrayShape() != null) {
            detail(details, "minItems", before.arrayShape().minItems(), after.arrayShape().minItems());
            detail(details, "maxItems", lengthText(before.arrayShape().maxItems()), lengthText(after.arrayShape().maxItems()));
        }
        return details.isEmpty() ? "" : " Changes (baseline -> candidate): " + String.join("; ", details) + ".";
    }

    /** Recorre el grafo normalizado por identidad: los componentes compartidos se visitan una vez. */
    private static SchemaValueContract schemaAt(SchemaValueContract root, String pointer) {
        var pending = new java.util.ArrayDeque<SchemaValueContract>();
        var seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<SchemaValueContract, Boolean>());
        pending.add(root);
        while (!pending.isEmpty()) {
            var node = pending.removeFirst();
            if (!seen.add(node)) continue;
            if (java.util.Objects.equals(node.definitionPointer(), pointer)) return node;
            if (node.objectShape() != null) pending.addAll(new java.util.TreeMap<>(node.objectShape().properties()).values());
            if (node.arrayShape() != null && node.arrayShape().items() != null) pending.add(node.arrayShape().items());
        }
        return null;
    }

    /** Solo añade diferencias observables; el motivo original conserva la prueba conjunta. */
    private static void detail(java.util.List<String> details, String keyword, Object before, Object after) {
        if (!java.util.Objects.equals(before, after)) details.add(keyword + ": " + before + " -> " + after);
    }

    /** Conserva exclusividad y decimales exactos sin expandir exponentes enormes. */
    private static String boundText(SchemaValueContract.Bound bound) {
        if (bound == null) return "unbounded";
        var value = bound.value().stripTrailingZeros();
        String text = Math.abs((long) value.scale()) <= 1000 ? value.toPlainString() : value.toString();
        return text + (bound.exclusive() ? " (exclusive)" : "");
    }

    /** Integer.MAX_VALUE es el límite normalizado para ausencia de máximo. */
    private static Object lengthText(int maximum) { return maximum == Integer.MAX_VALUE ? "unbounded" : maximum; }

    /** Enum canónico y acotado para lectura humana; el contrato original conserva los valores completos. */
    private static String enumText(java.util.List<ScalarValue> values) {
        if (values == null) return "unrestricted";
        var sorted = values.stream().map(ScalarValue::value).distinct().sorted().toList();
        return "[" + sorted.stream().limit(20).map(value -> value.codePointCount(0, value.length()) > 160
                        ? value.substring(0, value.offsetByCodePoints(0, 160)) + "..." : value)
                .collect(java.util.stream.Collectors.joining(", ")) + (sorted.size() > 20 ? ", ... (" + sorted.size() + " values)" : "") + "]";
    }

    /** Para un código ausente, el candidato apunta al contenedor existente de respuestas. */
    static DetectedChange responsePolicy(OperationId operation, String rule, String code, String media,
                                         ResponseContract before, ResponseContract after) {
        String identity = ComparisonProfile.RESPONSE_POLICY.id() + "\n" + rule + "\n"
                + token(operation.path()) + token(operation.method().name()) + token(code) + token(media);
        String baselinePointer = media == null ? before.definitionPointer() : before.representations().get(media);
        String candidatePointer = after == null ? before.usagePointer().substring(0, before.usagePointer().lastIndexOf('/')) : after.definitionPointer();
        return new DetectedChange(hash(identity), rule, Severity.ERROR,
                new Location(operation.path(), operation.method(), baselinePointer, candidatePointer, null,
                        before.usagePointer(), after == null ? null : after.usagePointer(), "RESPONSE", code, media),
                media == null ? "The documented response status was removed: " + code + " (profile availability policy)."
                        : "The documented media type was removed: " + media + " for response " + code + " (profile availability policy).");
    }

    /** Longitud y nulo explícitos evitan identidades ambiguas por concatenación. */
    private static String token(String value) { return value == null ? "-1:" : value.length() + ":" + value; }
}
