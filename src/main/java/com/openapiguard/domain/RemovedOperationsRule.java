package com.openapiguard.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Pure operation availability comparison; no parser or Spring dependency. */
public final class RemovedOperationsRule {
    /** Regla pura de disponibilidad sin estado. */
    public RemovedOperationsRule() {}
    /** Compara rutas y métodos literales; las plantillas ambiguas requieren revisión.
     * @param baseline contrato anterior usado como referencia de compatibilidad
     * @param candidate contrato propuesto, cuya compatibilidad se evalúa
     * @return informe limitado al perfil con rupturas y diagnósticos
     */
    public ComparisonReport compare(OperationContract baseline, OperationContract candidate) {
        List<DetectedChange> changes = new ArrayList<>();
        List<ComparisonReport.Diagnostic> diagnostics = new ArrayList<>();
        Map<String, Long> baselineShapes = shapes(baseline);
        Map<String, Long> candidateShapes = shapes(candidate);
        baseline.paths().keySet().stream().sorted().forEach(path -> {
            if (baseline.paths().get(path).isEmpty()) {
                return;
            }
            String shape = templateShape(path);
            boolean ambiguous = baselineShapes.get(shape) > 1
                    || candidateShapes.getOrDefault(shape, 0L) > 1;
            if (ambiguous || (!candidate.paths().containsKey(path)
                    && candidateShapes.containsKey(shape))) {
                diagnostics.add(new ComparisonReport.Diagnostic("PATH_MATCH_REQUIRES_REVIEW",
                        "comparison", path, "Renamed or ambiguous path template; review the operation."));
            } else if (!candidate.paths().containsKey(path)) {
                changes.add(DetectedChange.removed(path, null));
            } else {
                baseline.paths().get(path).stream().sorted(Comparator.comparing(Enum::name))
                        .filter(method -> !candidate.paths().get(path).contains(method))
                        .forEach(method -> changes.add(DetectedChange.removed(path, method)));
            }
        });
        return ComparisonReport.of(changes, diagnostics, false);
    }

    static String templateShape(String path) {
        return path.replaceAll("\\{[^/{}]+}", "{}");
    }

    static Map<String, Long> shapes(OperationContract contract) {
        return contract.paths().keySet().stream().collect(Collectors.groupingBy(
                RemovedOperationsRule::templateShape, Collectors.counting()));
    }
}
