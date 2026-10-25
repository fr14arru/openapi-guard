# Ruta de lectura del proyecto

| Orden | Lectura | Objetivo |
| --- | --- | --- |
| 1 | [README](../README.md), [alcance](mvp-scope.md) | Entender baseline/candidate, perfiles y códigos 0/1/2/3. |
| 2 | [Uso](product-usability.md), [desarrollo](development.md) | Ejecutar pares compatibles/incompatibles; localizar JAR e informes. |
| 3 | [Arquitectura](architecture.md), [guía del código](code/README.md) | Reconocer CLI → CompareContracts → ContractAnalyzer → adaptador → reglas. |
| 4 | [Ejecución](code/runtime.md), [ubicaciones](source-locations.md) | Seguir argumentos, lectura, informe y presentación. |
| 5 | [Inventario](code/inventory.md) y sus capítulos | Leer operaciones/parámetros antes de valores, objetos y arrays; comprender la inversión request/response. |
| 6 | [Tests](code/tests.md), [build](code/build.md), [mantenimiento](code/maintenance.md) | Encontrar fixtures, ejecutar regresiones e incorporar cambios. |
| 7 | [Integración Git](git-integration.md), [roadmap](roadmap.md) | Separar entregas de activación remota, distribución y validación comercial pendientes. |

Para leer código: ComparisonCommand → CompareContracts → SwaggerContractAnalyzer → RemovedOperationsRule. Después estudiar una comparación de schemas. Consultar Javadoc al abrir cada clase: sus invariantes complementan Markdown. Generarlo con `pwsh -NoProfile -File scripts/javadoc.ps1` y abrir `target/javadoc/index.html`.

Las cifras de documentos históricos describen sus entregas; el README refleja el estado actual. El portal Docusaurus presenta estas mismas guías, ejemplos y fuentes en el navegador. Ver [arranque local](portal/documentation-site.md). También se pueden leer los Markdown sin instalar el portal.
