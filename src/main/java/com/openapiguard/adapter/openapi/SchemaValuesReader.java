package com.openapiguard.adapter.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.openapiguard.domain.*;
import java.math.BigDecimal;
import java.util.*;

/** Normaliza restricciones soportadas por documento y conserva las desconocidas como lagunas explícitas. */
final class SchemaValuesReader {
    private static final String REFERENCE_GAP = "Cyclic or excessively deep reference.";
    private static final String NORMALIZATION_GAP = "Schema normalization budget exceeded (10000 nodes).";
    private static final Set<String> ALL_TYPES = Set.of("string", "boolean", "null", "integer", "number", "object", "array");
    private static final Set<String> KNOWN = Set.of("type", "enum", "const", "minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum",
            "minLength", "maxLength", "nullable", "title", "description", "default", "example", "examples", "deprecated", "format", "readOnly", "writeOnly", "xml", "externalDocs", "$schema");
    private final JsonNode root;
    private final boolean dialect31;
    private final boolean objects;
    private final boolean arrays;
    private final Map<String, SchemaValueContract> references = new HashMap<>();
    private int normalizedNodes;

    SchemaValuesReader(JsonNode root) {
        this(root, false);
    }

    SchemaValuesReader(JsonNode root, boolean objects) {
        this(root, objects, false);
    }

    SchemaValuesReader(JsonNode root, boolean objects, boolean arrays) {
        this.root = root;
        this.objects = objects;
        this.arrays = arrays;
        dialect31 = root.path("openapi").asText().startsWith("3.1.");
    }

    /** Extrae schemas por uso semántico, no solo por definición compartida, en orden estable. */
    Map<SchemaUseId, SchemaUse> read(Map<String, Set<HttpMethod>> paths,
                                   Map<OperationId, Map<ParameterId, EffectiveParameter>> parameters,
                                   Map<OperationId, RequestBodyContract> bodies) {
        Map<SchemaUseId, SchemaUse> result = new HashMap<>();
        paths.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(pathEntry -> pathEntry.getValue().stream()
                .sorted(Comparator.comparing(Enum::name)).forEach(method -> {
            String path = pathEntry.getKey();
            var operation = new OperationId(path, method);
            String pointer = "/paths/" + escape(path) + "/" + method.name().toLowerCase(Locale.ROOT);
            var body = bodies.get(operation);
            if (body != null) content(result, operation, SchemaUseId.Direction.REQUEST, null,
                    root.at(body.definitionPointer()).path("content"), body.definitionPointer() + "/content", body.usagePointer() + "/content");
            parameters.getOrDefault(operation, Map.of()).entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(parameterEntry -> {
                var id = parameterEntry.getKey();
                var parameter = parameterEntry.getValue();
                String schemaPointer = parameter.definitionPointer() + "/schema";
                result.put(new SchemaUseId(operation, SchemaUseId.Direction.REQUEST, id, null, null),
                        new SchemaUse(schema(root.at(schemaPointer), schemaPointer, new HashSet<>(), SchemaUseId.Direction.REQUEST), parameter.usagePointer() + "/schema",
                                parameter.presenceSupported() ? "" : "Unsupported parameter representation."));
            });
            root.at(pointer).path("responses").properties().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                if (entry.getKey().startsWith("x-")) return;
                String usage = pointer + "/responses/" + escape(entry.getKey());
                String definition = referenceObject(entry.getValue(), usage);
                content(result, operation, SchemaUseId.Direction.RESPONSE, entry.getKey(), root.at(definition).path("content"), definition + "/content", usage + "/content");
            });
        }));
        return Map.copyOf(result);
    }

    /** Conserva formatos originales y señala representación, encoding o fallback que requieren revisión. */
    private void content(Map<SchemaUseId, SchemaUse> result, OperationId operation, SchemaUseId.Direction direction,
                         String code, JsonNode content, String definition, String usage) {
        content.properties().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            boolean hasSchema = entry.getValue().has("schema");
            String pointer = definition + "/" + escape(entry.getKey()) + (hasSchema ? "/schema" : "");
            String media = entry.getKey().toLowerCase(Locale.ROOT);
            String issue = media.matches("application/(json|[a-z0-9!#$&^_.+\\-]+\\+json)") && !entry.getValue().has("encoding") ? "" : "Representation, wildcards or encoding require review.";
            if (code != null && !code.matches("[1-5][0-9]{2}")) issue = "Response with default or a status range is unsupported.";
            if (direction == SchemaUseId.Direction.REQUEST && !Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH).contains(operation.method()))
                issue = "Request body semantics for this method require review.";
            var id = new SchemaUseId(operation, direction, null, code, media);
            if (result.put(id, new SchemaUse(schema(entry.getValue().path("schema"), pointer, new HashSet<>(), direction),
                    usage + "/" + escape(entry.getKey()) + (hasSchema ? "/schema" : ""), issue)) != null)
                throw new IllegalArgumentException("Duplicate representation after case normalization: " + media);
        });
    }

    /** Localiza content de una respuesta referenciada, con ciclo/profundidad acotados. */
    private String referenceObject(JsonNode node, String pointer) {
        Set<String> visited = new HashSet<>();
        while (node.has("$ref")) {
            String ref = node.path("$ref").asText();
            if (!visited.add(ref) || visited.size() > 64) throw new IllegalArgumentException("Cyclic response reference: " + pointer);
            pointer = ref.substring(1);
            node = root.at(pointer);
        }
        return pointer;
    }

    /**
     * Normaliza recursivamente y proyecta acceso por dirección.
     * Ciclos o presupuesto excedido producen opacidad; la cache de referencias distingue direcciones.
     */
    private SchemaValueContract schema(JsonNode node, String pointer, Set<String> visited, SchemaUseId.Direction direction) {
        if (++normalizedNodes > 10_000) return opaque(pointer, NORMALIZATION_GAP);
        List<String> unsupported = new ArrayList<>();
        if (node.has("$ref")) {
            String ref = node.path("$ref").asText();
            if (!visited.add(ref) || visited.size() > 64) return opaque(pointer, REFERENCE_GAP);
            if (dialect31 && node.properties().stream().anyMatch(e -> !Set.of("$ref", "description", "summary").contains(e.getKey())))
                return opaque(pointer, "Constraints alongside $ref require composition.");
            // El mismo componente tiene distinta proyección readOnly/writeOnly en request y response.
            // Compartir cache por $ref solamente mezclaría required y propiedades de ambos usos.
            String key = direction.name() + ":" + ref;
            var cached = references.get(key);
            if (cached != null) return cached;
            var resolved = schema(root.at(ref.substring(1)), ref.substring(1), visited, direction);
            if (cacheable(resolved, Collections.newSetFromMap(new IdentityHashMap<>()))) references.put(key, resolved);
            return resolved;
        }
        if (node.isBoolean()) return new SchemaValueContract(node.asBoolean() ? ALL_TYPES : Set.of(), null, null, null, 0, Integer.MAX_VALUE, List.of(), pointer);
        if (!node.isObject() && !node.isMissingNode()) return opaque(pointer, "Schema is not an object.");
        node.fieldNames().forEachRemaining(key -> { if (!KNOWN.contains(key)
                && !(objects && Set.of("properties", "required", "additionalProperties").contains(key))
                && !(arrays && Set.of("items", "minItems", "maxItems").contains(key))) unsupported.add(key); });
        if (node.has("$schema") && !Set.of("https://json-schema.org/draft/2020-12/schema", "https://spec.openapis.org/oas/3.1/dialect/base")
                .contains(node.path("$schema").asText())) unsupported.add("Unrecognized schema dialect.");
        if (dialect31 && root.has("jsonSchemaDialect") && !Set.of("https://json-schema.org/draft/2020-12/schema", "https://spec.openapis.org/oas/3.1/dialect/base")
                .contains(root.path("jsonSchemaDialect").asText())) unsupported.add("Unrecognized global schema dialect.");
        Set<String> types = new HashSet<>();
        JsonNode type = node.path("type");
        if (type.isMissingNode()) types.addAll(ALL_TYPES);
        else if (type.isTextual()) types.add(type.asText());
        else if (dialect31 && type.isArray()) type.forEach(value -> types.add(value.asText()));
        else throw new IllegalArgumentException("Invalid schema type: " + pointer);
        if (!ALL_TYPES.containsAll(types) || types.isEmpty()) throw new IllegalArgumentException("Unknown type: " + pointer);
        if (node.has("nullable") && !node.path("nullable").isBoolean()) throw new IllegalArgumentException("Invalid nullable value: " + pointer);
        if (!dialect31 && type.isTextual() && node.path("nullable").asBoolean(false)) types.add("null");
        if (dialect31 && node.has("nullable")) unsupported.add("OpenAPI 3.0 nullable used in OpenAPI 3.1");
        if (type.isTextual() && (!arrays && type.asText().equals("array") || !objects && type.asText().equals("object"))
                || type.isArray() && (!arrays && types.contains("array") || !objects && types.contains("object")))
            unsupported.add("Compound type requires further analysis.");
        List<ScalarValue> values = null;
        if (node.has("enum")) {
            if (!node.path("enum").isArray() || node.path("enum").isEmpty()) throw new IllegalArgumentException("Invalid enum: " + pointer);
            values = new ArrayList<>();
            for (JsonNode value : node.path("enum")) {
                ScalarValue scalar = scalar(value);
                if (scalar == null) unsupported.add("Compound enum values are unsupported."); else values.add(scalar);
            }
        }
        if (node.has("const")) {
            if (!dialect31) unsupported.add("const is outside the OpenAPI 3.0 dialect");
            else {
                ScalarValue constant = scalar(node.get("const"));
                if (constant == null) unsupported.add("Compound const values are unsupported.");
                else if (values == null) values = List.of(constant);
                else values = values.contains(constant) ? List.of(constant) : List.of();
            }
        }
        boolean readOnly = flag(node, "readOnly");
        boolean writeOnly = flag(node, "writeOnly");
        if (readOnly && writeOnly) throw new IllegalArgumentException("A property cannot be both readOnly and writeOnly: " + pointer);
        ObjectShape shape = null;
        if (objects && types.contains("object")) {
            Map<String, SchemaValueContract> properties = new HashMap<>();
            Map<String, String> accessModes = new HashMap<>();
            Set<String> excluded = new HashSet<>();
            if (node.has("properties") && !node.path("properties").isObject()) throw new IllegalArgumentException("Invalid properties: " + pointer);
            node.path("properties").properties().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                var property = schema(entry.getValue(), pointer + "/properties/" + escape(entry.getKey()), new HashSet<>(visited), direction);
                accessModes.put(entry.getKey(), property.readOnly() ? "readOnly" : property.writeOnly() ? "writeOnly" : "readWrite");
                boolean projectedOut = direction == SchemaUseId.Direction.REQUEST ? property.readOnly() : property.writeOnly();
                if (projectedOut) excluded.add(entry.getKey()); else properties.put(entry.getKey(), property);
            });
            Set<String> required = new HashSet<>();
            if (node.has("required")) {
                if (!node.path("required").isArray()) throw new IllegalArgumentException("required must be an array: " + pointer);
                for (JsonNode name : node.path("required")) {
                    if (!name.isTextual() || !required.add(name.asText())) throw new IllegalArgumentException("Invalid or duplicate required property: " + pointer);
                }
            }
            required.removeAll(excluded);
            boolean additional = true;
            if (node.has("additionalProperties")) {
                if (node.path("additionalProperties").isBoolean()) additional = node.path("additionalProperties").asBoolean();
                else unsupported.add("Schema-valued additionalProperties is unsupported.");
            }
            shape = new ObjectShape(properties, required, additional, accessModes);
        }
        ArrayShape array = null;
        if (arrays && types.contains("array")) {
            if (!dialect31 && type.isTextual() && type.asText().equals("array") && !node.has("items"))
                throw new IllegalArgumentException("items is required for arrays in OpenAPI 3.0: " + pointer);
            JsonNode items = node.path("items");
            if (node.has("items") && !items.isObject() && !(dialect31 && items.isBoolean()))
                throw new IllegalArgumentException("items must be a schema: " + pointer);
            array = new ArrayShape(node.has("items") ? schema(items, pointer + "/items", new HashSet<>(visited), direction) : null,
                    length(node, "minItems", 0), length(node, "maxItems", Integer.MAX_VALUE));
        }
        return new SchemaValueContract(types, values, bound(node, "minimum", "exclusiveMinimum", true),
                bound(node, "maximum", "exclusiveMaximum", false), length(node, "minLength", 0),
                length(node, "maxLength", Integer.MAX_VALUE), unsupported, pointer, shape, readOnly, writeOnly, array);
    }

    /** Todos los tipos con una laguna no equivalen a un schema probado sin restricciones. */
    private SchemaValueContract opaque(String pointer, String reason) {
        return new SchemaValueContract(ALL_TYPES, null, null, null, 0, Integer.MAX_VALUE, List.of(reason), pointer);
    }

    /** Devuelve null para enum/const compuestos, cuya semántica aún no se implementa. */
    private ScalarValue scalar(JsonNode node) {
        if (node.isNull()) return new ScalarValue("null", "null");
        if (node.isTextual()) return new ScalarValue("string", node.asText());
        if (node.isBoolean()) return new ScalarValue("boolean", node.asText());
        if (node.isNumber()) return new ScalarValue("number", decimal(node).toString());
        return null;
    }

    /** Rechaza números fuera de precisión/escala 1024 antes de operar con BigDecimal. */
    private BigDecimal decimal(JsonNode node) {
        if (!node.isNumber()) throw new IllegalArgumentException("Invalid numeric bound.");
        BigDecimal value = node.decimalValue();
        if (Math.abs((long) value.scale()) > 1024 || value.precision() > 1024) throw new IllegalArgumentException("Number exceeds the precision budget.");
        return value;
    }

    /** Respeta exclusividad booleana 3.0 o numérica 3.1 y combina el extremo más fuerte. */
    private SchemaValueContract.Bound bound(JsonNode node, String inclusive, String exclusive, boolean lower) {
        var bound = node.has(inclusive) ? new SchemaValueContract.Bound(decimal(node.get(inclusive)), false) : null;
        if (!node.has(exclusive)) return bound;
        JsonNode value = node.get(exclusive);
        if (!dialect31) {
            if (!value.isBoolean()) throw new IllegalArgumentException("Exclusive bound must be a boolean in OpenAPI 3.0.");
            return bound == null ? null : new SchemaValueContract.Bound(bound.value(), value.asBoolean());
        }
        var strict = new SchemaValueContract.Bound(decimal(value), true);
        if (bound == null || (lower ? strict.value().compareTo(bound.value()) >= 0 : strict.value().compareTo(bound.value()) <= 0)) return strict;
        return bound;
    }

    /** Longitud entera exacta y no negativa; admite representación decimal como 1.0 si es exacta. */
    private int length(JsonNode node, String keyword, int defaultValue) {
        if (!node.has(keyword)) return defaultValue;
        JsonNode value = node.get(keyword);
        try {
            int length = decimal(value).intValueExact();
            if (length < 0) throw new IllegalArgumentException("Negative length bound: " + keyword);
            return length;
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Length bound is not an integer or exceeds the budget: " + keyword, exception);
        }
    }

    /** Valida flags presentes; la ausencia equivale a false. */
    private boolean flag(JsonNode node, String keyword) {
        if (node.has(keyword) && !node.path(keyword).isBoolean()) throw new IllegalArgumentException(keyword + " must be a boolean.");
        return node.path(keyword).asBoolean(false);
    }

    /** No reutiliza lagunas dependientes del contexto de referencias o del presupuesto restante. */
    private boolean cacheable(SchemaValueContract schema, Set<SchemaValueContract> visited) {
        if (!visited.add(schema)) return true;
        // Compare the exact context-dependent gaps, not words in translated explanations.
        if (schema.unsupported().stream().anyMatch(reason -> reason.equals(REFERENCE_GAP) || reason.equals(NORMALIZATION_GAP))) return false;
        return (schema.objectShape() == null || schema.objectShape().properties().values().stream().allMatch(child -> cacheable(child, visited)))
                && (schema.arrayShape() == null || schema.arrayShape().items() == null || cacheable(schema.arrayShape().items(), visited));
    }

    private String escape(String text) { return text.replace("~", "~0").replace("/", "~1"); }
}
