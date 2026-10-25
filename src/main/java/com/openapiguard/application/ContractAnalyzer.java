package com.openapiguard.application;

import com.openapiguard.domain.ComparisonReport;
import com.openapiguard.domain.ComparisonProfile;

/** Puerto de análisis de contenidos, independiente de filesystem, Spring y parser concreto. */
@FunctionalInterface
public interface ContractAnalyzer {
    /** Analiza contenidos ya leídos; los errores de entrada se representan en el informe.
     * @param baseline contrato anterior usado como referencia de compatibilidad
     * @param candidate contrato propuesto, cuya compatibilidad se evalúa
     * @return informe limitado al perfil con rupturas y diagnósticos
     */
    ComparisonReport analyze(String baseline, String candidate);

    /** Mantiene la cobertura histórica de implementaciones funcionales sin perfil explícito.
     * @return perfil publicado del analizador
     */
    default ComparisonProfile profile() {
        return ComparisonProfile.OPERATIONS_ONLY;
    }
}
