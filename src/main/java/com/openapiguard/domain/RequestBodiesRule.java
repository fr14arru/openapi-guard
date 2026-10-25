package com.openapiguard.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Política de presencia y cobertura de formatos de petición, separada de inclusión de datos. */
public final class RequestBodiesRule {
    /** Regla pura de presencia y formatos de petición. */
    public RequestBodiesRule() {}
    /** Compara operaciones compartidas no ambiguas; retiradas de documentación pueden requerir revisión.
     * @param baseline contrato anterior usado como referencia de compatibilidad
     * @param candidate contrato propuesto, cuya compatibilidad se evalúa
     * @return informe limitado al perfil con rupturas y diagnósticos
     */
    public ComparisonReport compare(OperationContract baseline, OperationContract candidate) {
        List<DetectedChange> changes = new ArrayList<>();
        List<ComparisonReport.Diagnostic> diagnostics = new ArrayList<>();
        var oldShapes = RemovedOperationsRule.shapes(baseline);
        var newShapes = RemovedOperationsRule.shapes(candidate);
        baseline.paths().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            String path = entry.getKey();
            if (oldShapes.getOrDefault(RemovedOperationsRule.templateShape(path), 0L) != 1
                    || newShapes.getOrDefault(RemovedOperationsRule.templateShape(path), 0L) != 1) return;
            entry.getValue().stream().sorted(Comparator.comparing(Enum::name)).forEach(method -> {
                if (!candidate.paths().getOrDefault(path, Set.of()).contains(method)) return;
                var operation = new OperationId(path, method);
                var before = baseline.requestBodies().get(operation);
                var after = candidate.requestBodies().get(operation);
                if (before == null && after == null) return;
                if (!Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH).contains(method)) {
                    diagnostics.add(new ComparisonReport.Diagnostic("REQUEST_BODY_METHOD_REQUIRES_REVIEW", "comparison",
                            after == null ? before.usagePointer() : after.usagePointer(),
                            "Request body semantics for " + method + " do not establish a contractual breaking change."));
                    return;
                }
                if (after == null) {
                    diagnostics.add(new ComparisonReport.Diagnostic("REQUEST_BODY_REMOVAL_REQUIRES_REVIEW", "baseline",
                            before.usagePointer(), "Removing the documented request body requires review of its support."));
                    return;
                }
                if (after.required() && (before == null || !before.required())) {
                    changes.add(DetectedChange.requestBody(operation, before == null ? "REQUEST_BODY_REQUIRED_ADDED"
                            : "REQUEST_BODY_BECAME_REQUIRED", before, after, null));
                }
                boolean unsupported = after.mediaTypes().stream().anyMatch(media -> !supported(media))
                        || before != null && before.mediaTypes().stream().anyMatch(media -> !supported(media));
                if (unsupported) {
                    diagnostics.add(new ComparisonReport.Diagnostic("REQUEST_MEDIA_TYPE_REQUIRES_REVIEW", "comparison",
                            after.usagePointer(), "Media type ranges with unsupported parameters or patterns."));
                } else if (before != null) {
                    before.mediaTypes().stream().sorted().filter(old -> after.mediaTypes().stream().noneMatch(updated -> covers(updated, old)))
                            .forEach(media -> changes.add(DetectedChange.requestBody(operation, "REQUEST_MEDIA_TYPE_REMOVED", before, after, media)));
                }
            });
        });
        return ComparisonReport.of(ComparisonProfile.REQUEST_BODY, changes, diagnostics, false);
    }

    /** Fragmento de formatos concretos y comodines simples con cobertura demostrable. */
    private static boolean supported(String media) {
        return media.matches("(?i)(\\*/\\*|[a-z0-9!#$&^_.+\\-]+/(\\*|[a-z0-9!#$&^_.+\\-]+))");
    }

    /** El rango candidato debe seguir admitiendo el formato anterior, ignorando mayúsculas. */
    private static boolean covers(String candidate, String baseline) {
        String newValue = candidate.toLowerCase(Locale.ROOT);
        String oldValue = baseline.toLowerCase(Locale.ROOT);
        if (newValue.equals("*/*") || newValue.equals(oldValue)) return true;
        String[] parts = newValue.split("/");
        return parts[1].equals("*") && oldValue.startsWith(parts[0] + "/");
    }
}
