package com.openapiguard.domain;

import java.util.*;

/** Published response availability policy, separate from directional payload inclusion. */
public final class ResponsePolicyRule {
    /** Política de disponibilidad separada de la inclusión de valores. */
    public ResponsePolicyRule() {}
    /** Aplica disponibilidad de códigos y formatos sin inferir inclusión del payload.
     * @param baseline contrato anterior usado como referencia de compatibilidad
     * @param candidate contrato propuesto, cuya compatibilidad se evalúa
     * @return informe limitado al perfil con rupturas y diagnósticos
     */
    public ComparisonReport compare(OperationContract baseline, OperationContract candidate) {
        List<DetectedChange> changes = new ArrayList<>();
        List<ComparisonReport.Diagnostic> diagnostics = new ArrayList<>();
        var oldShapes = RemovedOperationsRule.shapes(baseline);
        var newShapes = RemovedOperationsRule.shapes(candidate);
        baseline.responses().entrySet().stream().sorted(Comparator.comparing((Map.Entry<OperationId, Map<String, ResponseContract>> e) -> e.getKey().path())
                .thenComparing(e -> e.getKey().method().name())).forEach(entry -> {
            var operation = entry.getKey();
            if (!candidate.paths().getOrDefault(operation.path(), Set.of()).contains(operation.method())
                    || oldShapes.getOrDefault(RemovedOperationsRule.templateShape(operation.path()), 0L) != 1
                    || newShapes.getOrDefault(RemovedOperationsRule.templateShape(operation.path()), 0L) != 1) return;
            var old = entry.getValue();
            var updated = candidate.responses().getOrDefault(operation, Map.of());
            var codes = new TreeSet<>(old.keySet());
            codes.addAll(updated.keySet());
            for (String code : codes) {
                var before = old.get(code);
                var after = updated.get(code);
                if (!code.matches("[1-5][0-9]{2}")) {
                    review(diagnostics, "RESPONSE_FALLBACK_REQUIRES_REVIEW", after == null ? before : after,
                            "Coverage of " + code + " requires reviewing precedence and effective responses.");
                } else if (before == null) {
                    review(diagnostics, "RESPONSE_STATUS_ADDED_REQUIRES_REVIEW", after,
                            "The new response status " + code + " requires reviewing consumer behavior.");
                } else if (after == null) {
                    if (updated.containsKey("default") || updated.containsKey(code.charAt(0) + "XX"))
                        review(diagnostics, "RESPONSE_STATUS_COVERAGE_REQUIRES_REVIEW", before,
                                "Response status " + code + " may be covered by default or a status range.");
                    else changes.add(DetectedChange.responsePolicy(operation, "RESPONSE_STATUS_REMOVED", code, null, before, null));
                } else compareMedia(operation, code, before, after, changes, diagnostics);
            }
        });
        return ComparisonReport.of(ComparisonProfile.RESPONSE_POLICY, changes, diagnostics, false);
    }

    private void compareMedia(OperationId operation, String code, ResponseContract before, ResponseContract after,
                              List<DetectedChange> changes, List<ComparisonReport.Diagnostic> diagnostics) {
        if (before.representations().keySet().stream().anyMatch(media -> !concrete(media))
                || after.representations().keySet().stream().anyMatch(media -> !concrete(media))) {
            review(diagnostics, "RESPONSE_MEDIA_COVERAGE_REQUIRES_REVIEW", after,
                    "Media type wildcards or parameters require reviewing coverage and precedence.");
            return;
        }
        before.representations().keySet().stream().sorted().filter(media -> !after.representations().containsKey(media))
                .forEach(media -> changes.add(DetectedChange.responsePolicy(operation, "RESPONSE_MEDIA_TYPE_REMOVED", code, media, before, after)));
        after.representations().keySet().stream().sorted().filter(media -> !before.representations().containsKey(media))
                .forEach(media -> review(diagnostics, "RESPONSE_MEDIA_TYPE_ADDED_REQUIRES_REVIEW", after,
                        "The new media type " + media + " requires reviewing negotiation and consumers."));
    }

    private boolean concrete(String media) { return media.matches("[a-z0-9!#$&^_.+\\-]+/[a-z0-9!#$&^_.+\\-]+"); }
    private void review(List<ComparisonReport.Diagnostic> diagnostics, String code, ResponseContract response, String message) {
        diagnostics.add(new ComparisonReport.Diagnostic(code, "comparison", response.usagePointer(), message));
    }
}
