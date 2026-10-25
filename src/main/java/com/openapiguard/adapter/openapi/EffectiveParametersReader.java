package com.openapiguard.adapter.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.openapiguard.domain.EffectiveParameter;
import com.openapiguard.domain.HttpMethod;
import com.openapiguard.domain.OperationId;
import com.openapiguard.domain.ParameterId;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Uses original nodes for provenance after Swagger validates the document. Never resolves external input. */
final class EffectiveParametersReader {
    Map<OperationId, Map<ParameterId, EffectiveParameter>> read(JsonNode root,
                                                              Map<String, Set<HttpMethod>> paths) {
        Map<OperationId, Map<ParameterId, EffectiveParameter>> result = new HashMap<>();
        paths.forEach((path, methods) -> {
            String pathPointer = "/paths/" + escape(path);
            var inherited = list(root, root.at(pathPointer).path("parameters"), pathPointer + "/parameters", path);
            methods.forEach(method -> {
                String operationPointer = pathPointer + "/" + method.name().toLowerCase(Locale.ROOT);
                Map<ParameterId, EffectiveParameter> effective = new HashMap<>(inherited);
                effective.putAll(list(root, root.at(operationPointer).path("parameters"),
                        operationPointer + "/parameters", path));
                result.put(new OperationId(path, method), Map.copyOf(effective));
            });
        });
        return Map.copyOf(result);
    }

    private Map<ParameterId, EffectiveParameter> list(JsonNode root, JsonNode list, String pointer, String path) {
        if (list.isMissingNode()) {
            return Map.of();
        }
        if (!list.isArray()) {
            throw invalid(pointer, "parameters must be an array");
        }
        Map<ParameterId, EffectiveParameter> parameters = new HashMap<>();
        Set<ParameterId> declared = new HashSet<>();
        for (int index = 0; index < list.size(); index++) {
            String usage = pointer + "/" + index;
            String definition = usage;
            JsonNode node = list.get(index);
            Set<String> visited = new HashSet<>();
            while (node.has("$ref")) {
                String ref = node.path("$ref").asText();
                if (!ref.startsWith("#/") || !visited.add(ref) || visited.size() > 64) {
                    throw invalid(usage, "External, cyclic or excessively deep parameter reference");
                }
                definition = ref.substring(1);
                node = root.at(definition);
            }
            if (!node.isObject() || !node.path("name").isTextual() || !node.path("in").isTextual()) {
                throw invalid(usage, "Invalid parameter definition");
            }
            ParameterId id = new ParameterId(node.path("name").asText(), node.path("in").asText());
            if (!declared.add(id)) {
                throw invalid(usage, "Duplicate parameter: " + id);
            }
            if (node.has("required") && !node.path("required").isBoolean()) {
                throw invalid(usage, "required must be a boolean");
            }
            boolean required = node.path("required").asBoolean(false);
            if (id.in().equals("path") && (!required || !path.contains("{" + id.name() + "}"))) {
                throw invalid(usage, "A path parameter must be required and match the path template");
            }
            if (id.in().equals("header") && Set.of("accept", "content-type", "authorization").contains(id.name())) {
                continue;
            }
            if (node.has("schema") == node.has("content")) {
                throw invalid(usage, "A parameter must declare exactly one of schema or content");
            }
            String style = node.path("style").asText(id.in().equals("query") || id.in().equals("cookie")
                    ? "form" : "simple");
            boolean explode = node.path("explode").asBoolean(style.equals("form"));
            String serialization = style + ":" + explode + ":" + node.path("allowReserved").asBoolean(false)
                    + ":" + node.path("allowEmptyValue").asBoolean(false) + ":" + node.has("content");
            // Presence guarantees for flattened objects/content need additional serialization analysis.
            String type = node.path("schema").path("type").asText();
            boolean supported = Set.of("string", "integer", "number", "boolean").contains(type)
                    && !node.has("content") && !node.path("allowEmptyValue").asBoolean(false)
                    && !node.path("schema").has("$ref") && !node.path("schema").has("allOf")
                    && !node.path("schema").has("anyOf") && !node.path("schema").has("oneOf");
            parameters.put(id, new EffectiveParameter(id, required, definition, usage, serialization, supported));
        }
        return parameters;
    }

    private static IllegalArgumentException invalid(String pointer, String description) {
        return new IllegalArgumentException(pointer + ": " + description);
    }

    private static String escape(String value) {
        return value.replace("~", "~0").replace("/", "~1");
    }
}
