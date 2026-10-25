package com.openapiguard.domain;

import java.util.*;

/** Incluye anterior en candidato para petición y candidato en anterior para respuesta. */
public final class SchemaValuesRule {
    /** Regla de inclusión direccional sin parser ni estado externo. */
    public SchemaValuesRule() {}
    /** Conserva el perfil escalar de llamadas anteriores a objetos/arrays.
     * @param baseline contrato anterior usado como referencia de compatibilidad
     * @param candidate contrato propuesto, cuya compatibilidad se evalúa
     * @return informe limitado al perfil con rupturas y diagnósticos
     */
    public ComparisonReport compare(OperationContract baseline, OperationContract candidate) {
        return compare(baseline, candidate, ComparisonProfile.SCHEMA_VALUES);
    }

    /** Conserva simultáneamente rupturas probadas y lagunas, sin operar sobre plantillas ambiguas.
     * @param baseline contrato anterior usado como referencia de compatibilidad
     * @param candidate contrato propuesto, cuya compatibilidad se evalúa
     * @param profile cobertura versionada del análisis; no expresa compatibilidad global
     * @return informe limitado al perfil con rupturas y diagnósticos
     */
    public ComparisonReport compare(OperationContract baseline, OperationContract candidate, ComparisonProfile profile) {
        List<DetectedChange> changes = new ArrayList<>();
        List<ComparisonReport.Diagnostic> diagnostics = new ArrayList<>();
        Set<SchemaUseId> uses = new HashSet<>(baseline.schemas().keySet());
        uses.addAll(candidate.schemas().keySet());
        var oldShapes = RemovedOperationsRule.shapes(baseline);
        var newShapes = RemovedOperationsRule.shapes(candidate);
        uses.stream().sorted(Comparator.comparing((SchemaUseId id) -> id.operation().path())
                .thenComparing(id -> id.operation().method().name()).thenComparing(id -> id.direction().name())
                .thenComparing(id -> id.parameter() == null ? "" : id.parameter().in())
                .thenComparing(id -> id.parameter() == null ? "" : id.parameter().name())
                .thenComparing(id -> id.responseCode() == null ? "" : id.responseCode())
                .thenComparing(id -> id.mediaType() == null ? "" : id.mediaType())).forEach(id -> {
            var operation = id.operation();
            if (!baseline.paths().getOrDefault(operation.path(), Set.of()).contains(operation.method())
                    || !candidate.paths().getOrDefault(operation.path(), Set.of()).contains(operation.method())
                    || oldShapes.getOrDefault(RemovedOperationsRule.templateShape(operation.path()), 0L) != 1
                    || newShapes.getOrDefault(RemovedOperationsRule.templateShape(operation.path()), 0L) != 1) return;
            var before = baseline.schemas().get(id);
            var after = candidate.schemas().get(id);
            if (before == null || after == null) {
                if (id.direction() == SchemaUseId.Direction.RESPONSE && !profile.includesResponsePolicy()) diagnostics.add(new ComparisonReport.Diagnostic("RESPONSE_SCHEMA_COVERAGE_REQUIRES_REVIEW",
                        before == null ? "candidate" : "baseline", (before == null ? after : before).usagePointer(), "Response status or representation coverage requires review."));
                else if (id.direction() == SchemaUseId.Direction.REQUEST && before != null && id.parameter() == null && candidate.requestBodies().containsKey(operation))
                    diagnostics.add(new ComparisonReport.Diagnostic("REQUEST_SCHEMA_COVERAGE_REQUIRES_REVIEW", "baseline", before.usagePointer(), "Review the schema applicable to the baseline media type."));
                return;
            }
            String issue = before.representationIssue().isEmpty() ? after.representationIssue() : before.representationIssue();
            if (!issue.isEmpty()) {
                diagnostics.add(new ComparisonReport.Diagnostic("SCHEMA_REPRESENTATION_REQUIRES_REVIEW", "comparison", after.usagePointer(), issue));
                return;
            }
            // El motor de inclusión solo conoce source ⊆ target. La regla establece la perspectiva:
            // el servidor nuevo recibe valores antiguos; el cliente antiguo recibe valores nuevos.
            // Cambiar estos operandos en respuesta produce falsos positivos en enums y required.
            var source = id.direction() == SchemaUseId.Direction.REQUEST ? before.schema() : after.schema();
            var target = id.direction() == SchemaUseId.Direction.REQUEST ? after.schema() : before.schema();
            var result = new SchemaInclusion().compare(source, target);
            if (result.outcome() == SchemaInclusion.Outcome.EXCLUDED) changes.add(DetectedChange.schema(profile, id, before, after, result));
            // Una prueba de ruptura no demuestra que el resto del esquema esté cubierto.
            // Guardar ambos resultados permite bloquear la ruptura sin ocultar incertidumbre.
            if (result.incomplete()) diagnostics.add(new ComparisonReport.Diagnostic("SCHEMA_INCLUSION_REQUIRES_REVIEW", "comparison", after.usagePointer(),
                    result.outcome() == SchemaInclusion.Outcome.UNKNOWN ? result.reason() : "Other usage constraints require review even when the main inclusion has been established or disproved."));
        });
        return ComparisonReport.of(profile, changes, diagnostics, false);
    }
}
