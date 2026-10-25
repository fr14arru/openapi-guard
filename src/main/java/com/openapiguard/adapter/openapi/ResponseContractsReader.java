package com.openapiguard.adapter.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.openapiguard.domain.*;
import java.util.*;

/** Reads only response coverage metadata; schema values stay in the existing normalizer. */
final class ResponseContractsReader {
    Map<OperationId, Map<String, ResponseContract>> read(JsonNode root, Map<String, Set<HttpMethod>> paths) {
        Map<OperationId, Map<String, ResponseContract>> result = new HashMap<>();
        paths.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(path -> path.getValue().stream()
                .sorted(Comparator.comparing(Enum::name)).forEach(method -> {
            String operationPointer = "/paths/" + escape(path.getKey()) + "/" + method.name().toLowerCase(Locale.ROOT);
            Map<String, ResponseContract> responses = new HashMap<>();
            root.at(operationPointer).path("responses").properties().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                String code = entry.getKey();
                if (code.startsWith("x-")) return;
                if (!code.matches("[1-5][0-9]{2}|[1-5]XX|default")) throw new IllegalArgumentException("Invalid response status: " + code);
                String usage = operationPointer + "/responses/" + escape(code);
                String definition = usage;
                JsonNode node = entry.getValue();
                Set<String> visited = new HashSet<>();
                while (node.has("$ref")) {
                    String reference = node.path("$ref").asText();
                    if (!visited.add(reference) || visited.size() > 64) throw new IllegalArgumentException("Cyclic response reference: " + usage);
                    definition = reference.substring(1);
                    node = root.at(definition);
                }
                if (!node.isObject() || node.has("content") && !node.path("content").isObject())
                    throw new IllegalArgumentException("Invalid response or content: " + definition);
                Map<String, String> media = new HashMap<>();
                String contentPointer = definition + "/content/";
                node.path("content").properties().stream().sorted(Map.Entry.comparingByKey()).forEach(format -> {
                    String identity = format.getKey().toLowerCase(Locale.ROOT);
                    if (media.put(identity, contentPointer + escape(format.getKey())) != null)
                        throw new IllegalArgumentException("Duplicate media type after case normalization: " + identity);
                });
                responses.put(code, new ResponseContract(media, definition, usage));
            });
            result.put(new OperationId(path.getKey(), method), responses);
        }));
        return Map.copyOf(result);
    }

    private String escape(String value) { return value.replace("~", "~0").replace("/", "~1"); }
}
