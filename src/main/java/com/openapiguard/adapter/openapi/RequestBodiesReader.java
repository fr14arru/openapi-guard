package com.openapiguard.adapter.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.openapiguard.domain.HttpMethod;
import com.openapiguard.domain.OperationId;
import com.openapiguard.domain.RequestBodyContract;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Extrae presencia, formatos y procedencia de cuerpos, sin interpretar sus schemas. */
final class RequestBodiesReader {
    /** Sigue referencias internas acotadas y separa el lugar de definición del de uso. */
    Map<OperationId, RequestBodyContract> read(JsonNode root, Map<String, Set<HttpMethod>> paths) {
        var bodies = new HashMap<OperationId, RequestBodyContract>();
        paths.forEach((path, methods) -> methods.forEach(method -> {
            String usage = "/paths/" + path.replace("~", "~0").replace("/", "~1") + "/"
                    + method.name().toLowerCase(Locale.ROOT) + "/requestBody";
            JsonNode body = root.at(usage);
            if (body.isMissingNode()) return;
            String definition = usage;
            Set<String> seen = new HashSet<>();
            while (body.has("$ref")) {
                String ref = body.path("$ref").asText();
                if (!ref.startsWith("#/") || !seen.add(ref) || seen.size() > 64)
                    throw new IllegalArgumentException("Invalid or cyclic request body reference: " + usage);
                definition = ref.substring(1);
                body = root.at(definition);
            }
            if (!body.isObject() || !body.path("content").isObject() || body.path("content").isEmpty()
                    || body.has("required") && !body.path("required").isBoolean())
                throw new IllegalArgumentException("Invalid request body: " + usage);
            Set<String> media = new HashSet<>();
            body.path("content").fieldNames().forEachRemaining(media::add);
            bodies.put(new OperationId(path, method), new RequestBodyContract(body.path("required").asBoolean(false), media, definition, usage));
        }));
        return Map.copyOf(bodies);
    }
}
