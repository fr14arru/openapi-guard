package com.openapiguard.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Compares request presence only; never interprets a response schema as a request. */
public final class RequiredParametersRule {
    /** Regla pura de obligatoriedad sobre parámetros efectivos. */
    public RequiredParametersRule() {}
    /** Detecta altas obligatorias y refuerzos de obligatoriedad en parámetros efectivos.
     * @param baseline contrato anterior usado como referencia de compatibilidad
     * @param candidate contrato propuesto, cuya compatibilidad se evalúa
     * @return informe limitado al perfil con rupturas y diagnósticos
     */
    public ComparisonReport compare(OperationContract baseline, OperationContract candidate) {
        List<DetectedChange> changes = new ArrayList<>();
        List<ComparisonReport.Diagnostic> diagnostics = new ArrayList<>();
        var oldShapes = RemovedOperationsRule.shapes(baseline);
        var newShapes = RemovedOperationsRule.shapes(candidate);
        baseline.parameters().keySet().stream()
                .sorted(Comparator.comparing(OperationId::path).thenComparing(operation -> operation.method().name()))
                .filter(operation -> candidate.paths().getOrDefault(operation.path(), java.util.Set.of())
                        .contains(operation.method()))
                .filter(operation -> oldShapes.getOrDefault(RemovedOperationsRule.templateShape(operation.path()), 0L) == 1
                        && newShapes.getOrDefault(RemovedOperationsRule.templateShape(operation.path()), 0L) == 1)
                .forEach(operation -> {
                    Map<ParameterId, EffectiveParameter> old = baseline.parameters().get(operation);
                    Map<ParameterId, EffectiveParameter> updated = candidate.parameters().getOrDefault(operation, Map.of());
                    TreeSet<ParameterId> ids = new TreeSet<>(old.keySet());
                    ids.addAll(updated.keySet());
                    for (ParameterId id : ids) {
                        EffectiveParameter before = old.get(id);
                        EffectiveParameter after = updated.get(id);
                        if (after == null) {
                            diagnostic(diagnostics, "PARAMETER_REMOVAL_REQUIRES_REVIEW", "baseline", before,
                                    "Removing the parameter requires checking whether the server still accepts it.");
                        } else if (before != null && !before.presenceSupported()) {
                            diagnostic(diagnostics, "PARAMETER_PRESENCE_UNSUPPORTED", "baseline", before,
                                    "Baseline parameter presence requires analyzing its representation.");
                        } else if (!after.presenceSupported()) {
                            diagnostic(diagnostics, "PARAMETER_PRESENCE_UNSUPPORTED", "candidate", after,
                                    "Parameter presence requires analyzing its representation.");
                        } else if (before != null && !before.serialization().equals(after.serialization())) {
                            diagnostic(diagnostics, "PARAMETER_SERIALIZATION_REQUIRES_REVIEW", "candidate", after,
                                    "Parameter serialization changed; review its effective presence.");
                        } else if (after.required() && (before == null || !before.required())) {
                            changes.add(DetectedChange.parameterRequired(operation, before, after));
                        }
                    }
                });
        return ComparisonReport.of(ComparisonProfile.PARAMETER_PRESENCE, changes, diagnostics, false);
    }

    private static void diagnostic(List<ComparisonReport.Diagnostic> diagnostics, String code,
                                   String source, EffectiveParameter parameter, String description) {
        diagnostics.add(new ComparisonReport.Diagnostic(code, source, parameter.usagePointer(), description));
    }
}
