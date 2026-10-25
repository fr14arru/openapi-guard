package com.openapiguard.application;

import com.openapiguard.domain.ComparisonReport;
import com.openapiguard.domain.ComparisonProfile;

import java.util.Objects;

/** Caso de uso invocable sin Spring ni acceso a archivos; el puerto analiza contenidos. */
public final class CompareContracts {
    private final ContractAnalyzer analyzer;

    /** Ensambla el caso de uso con un analizador no nulo.
     * @param analyzer puerto que normaliza entradas y ejecuta reglas sin depender de Spring
     */
    public CompareContracts(ContractAnalyzer analyzer) {
        this.analyzer = Objects.requireNonNull(analyzer);
    }

    /** Compara contenido anterior y candidato, en ese orden, dentro del perfil del puerto.
     * @param baseline contrato anterior usado como referencia de compatibilidad
     * @param candidate contrato propuesto, cuya compatibilidad se evalúa
     * @return informe limitado al perfil con rupturas y diagnósticos
     */
    public ComparisonReport compare(String baseline, String candidate) {
        return analyzer.analyze(baseline, candidate);
    }

    /** Expone la cobertura versionada del analizador utilizado.
     * @return perfil publicado del analizador
     */
    public ComparisonProfile profile() {
        return analyzer.profile();
    }
}
