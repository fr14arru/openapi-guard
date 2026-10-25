package com.openapiguard.domain;

import java.util.List;

/**
 * Informe limitado a un perfil: estado y veredicto son independientes.
 * Las fábricas calculan combinaciones coherentes; el constructor solo copia las listas.
 * @param profileId cobertura versionada del análisis; no expresa compatibilidad global
 * @param status estado de ejecución y completitud de cobertura
 * @param verdict conclusión semántica; puede coexistir ruptura con cobertura parcial
 * @param excludedFacets facetas deliberadamente ajenas a este perfil
 * @param changes rupturas demostradas, separadas de la incertidumbre
 * @param diagnostics errores o lagunas localizados que impiden certificar cobertura
 */
public record ComparisonReport(String profileId, Status status, Verdict verdict,
                               List<String> excludedFacets, List<DetectedChange> changes,
                               List<Diagnostic> diagnostics) {
    /** Cobertura completada, con lagunas o entrada fallida. */
    public enum Status {
        /** Cobertura completada dentro del perfil. */ COMPLETED,
        /** Lagunas localizadas; puede existir ruptura demostrada. */ PARTIAL,
        /** Entrada inválida o ejecución fallida. */ FAILED
    }
    /** Una ruptura probada puede coexistir con estado PARTIAL. */
    public enum Verdict {
        /** Existe una ruptura demostrada. */ BREAKING,
        /** Sin rupturas ni lagunas dentro del perfil. */ COMPATIBLE,
        /** Cobertura insuficiente para certificar compatibilidad. */ INDETERMINATE
    }
    /** Incertidumbre o error localizado; no representa una ruptura probada.
     * @param code identificador estable del diagnóstico
     * @param source origen del diagnóstico
     * @param location procedencia del hallazgo en definición y uso
     * @param description explicación de la conclusión para revisión humana
     */
    public record Diagnostic(String code, String source, String location, String description) {}

    /** Copia listas sin recalcular estado ni veredicto; las fábricas of calculan ambos.
     * @param profileId cobertura versionada del análisis; no expresa compatibilidad global
     * @param status estado de ejecución y completitud de cobertura
     * @param verdict conclusión semántica; puede coexistir ruptura con cobertura parcial
     * @param excludedFacets facetas deliberadamente ajenas a este perfil
     * @param changes rupturas demostradas, separadas de la incertidumbre
     * @param diagnostics errores o lagunas localizados que impiden certificar cobertura
     */
    public ComparisonReport {
        excludedFacets = List.copyOf(excludedFacets);
        changes = List.copyOf(changes);
        diagnostics = List.copyOf(diagnostics);
    }

    /** Fábrica histórica limitada a operaciones.
     * @param changes rupturas demostradas, separadas de la incertidumbre
     * @param diagnostics errores o lagunas localizados que impiden certificar cobertura
     * @param failed true si la entrada no pudo analizarse
     * @return informe limitado al perfil con rupturas y diagnósticos
     */
    public static ComparisonReport of(List<DetectedChange> changes, List<Diagnostic> diagnostics,
                                      boolean failed) {
        return of(ComparisonProfile.OPERATIONS_ONLY, changes, diagnostics, failed);
    }

    /** Calcula estado por fallo/diagnósticos y veredicto por ruptura/cobertura, con exclusiones.
     * @param profile cobertura versionada del análisis; no expresa compatibilidad global
     * @param changes rupturas demostradas, separadas de la incertidumbre
     * @param diagnostics errores o lagunas localizados que impiden certificar cobertura
     * @param failed true si la entrada no pudo analizarse
     * @return informe limitado al perfil con rupturas y diagnósticos
     */
    public static ComparisonReport of(ComparisonProfile profile, List<DetectedChange> changes,
                                      List<Diagnostic> diagnostics, boolean failed) {
        Status status = failed ? Status.FAILED
                : diagnostics.isEmpty() ? Status.COMPLETED : Status.PARTIAL;
        Verdict verdict = !changes.isEmpty() ? Verdict.BREAKING
                : status == Status.COMPLETED ? Verdict.COMPATIBLE : Verdict.INDETERMINATE;
        return new ComparisonReport(profile.id(), status, verdict, profile.excludedFacets(), changes, diagnostics);
    }
}
