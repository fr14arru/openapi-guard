# Qué garantiza la batería de pruebas

La batería actual contiene 952 ejecuciones JUnit: 927 en test y 25 adicionales del JAR en verify, más diez comprobaciones PowerShell. Son ejecuciones parametrizadas, no 952 métodos distintos. La entrega de uso incorpora siete pruebas de `UsabilityTest`, diez de `SourceOutputTest`, cinco pruebas del lanzador real y doclint estricto, junto a las 29 comprobaciones Git/workflow. La enumeración está en [inventario](inventory.md); las expectativas del corpus se conservan en cases.json y [su explicación](../contract-corpus.md).

## Clases y oráculos

| Clase | Ejecuciones | Propósito |
| --- | ---: | --- |
| ArrayInclusionTest | 324 | 18 formas de arrays × 18, con oráculo independiente de aceptación de valores. |
| ObjectInclusionTest | 196 | 14 formas de objetos × 14, con apertura, propiedad, required y valores booleanos. |
| SchemaInclusionTest | 41 | 36 pares de intervalos enteros y regresiones de enum, Unicode, opacidad y orden canónico. |
| RemovedOperationsRuleTest | 12 | Ocho métodos, retirada de endpoint, adiciones, ambigüedad y orden. |
| RequiredParametersRuleTest | 11 | Presencia, relajación, retiradas y revisión sin falsos positivos. |
| ResponsePolicyRuleTest | 3 | Política pura de disponibilidad, ubicaciones y cobertura incierta. |
| SwaggerContractAnalyzerTest | 20 | Caso de uso sin Spring, equivalencia YAML/JSON, familias, entradas inválidas y referencias. |
| EffectiveParametersTest | 16 | Herencia, override, referencias, validación y procedencia. |
| ParameterComparisonTest | 7 | Integración presencia/perfil histórico y serialización. |
| RequestBodyComparisonTest | 20 | Obligatoriedad, media types, métodos ambiguos y referencias. |
| SchemaValuesComparisonTest | 22 | Dirección, tipos/enums/nullabilidad, límites conjuntos y cobertura escalar. |
| ObjectSchemaComparisonTest | 29 | Objetos abiertos/cerrados, required, proyección, referencias, presupuestos y DAGs. |
| ArraySchemaComparisonTest | 18 | Items/longitudes por dirección, anidamiento, vacío, opacidad y dialectos. |
| ResponsePolicyComparisonTest | 16 | Retiradas/adiciones, bodyless, fallback, formatos, referencias y perfiles anteriores. |
| ContractCorpusTest | 146 | Pares declarativos revisables del perfil histórico de parámetros. |
| ContractBoundaryIntegrationTest | 16 | Límites exactos de bytes, Unicode, profundidad y ausencia de acceso a referencias externas. |
| ComparisonCommandTest | 2 | Contrato de salida y errores de argumentos del adaptador CLI. |
| ComparisonProcessTest | 1 | Proceso CLI y salida verificable. |
| HumanOutputTest | 9 | Texto, ayuda, selección de perfiles/opciones y mismos códigos. |
| OpenApiGuardApplicationTest | 1 | Arranque no web y cierre del contexto Spring. |
| PackagedCliIT | 25 | JVM separada ejecutando el JAR real: cobertura, códigos 0/1/2/3, UTF-8, paths y errores. |

`TestSpecifications.fixture` lee `/openapi/<name>` del classpath en UTF-8 y lanza IOException si falta. Su constructor privado impide instanciar una utilidad sin estado.

## Por qué los oráculos aportan confianza

ArrayInclusionTest enumera arrays de longitud 0–2 con valores representativos booleanos y otros tipos. Las 18 formas tienen items booleanos y límites dentro de ese universo. `accepts` aplica una definición pequeña e independiente; no llama a SchemaInclusion para decidir lo esperado. ObjectInclusionTest distingue propiedad ausente de null y nombres adicionales, enumerando objetos con `x`/`other`; sus 14 formas están dentro del fragmento descrito. SchemaInclusionTest compara intervalos finitos mediante IntStream y predicado matemático directo.

Los oráculos son exhaustivos para los universos finitos construidos, no para todos los esquemas. Los tests de adaptador añaden ejemplos de dirección, combinaciones y regresiones fuera de esos universos. Otra librería de diff no se usa como autoridad única.

Los records auxiliares de tests son datos del escenario: ArrayInclusionTest.Shape contiene mínimo, máximo y valores de items; ObjectInclusionTest.Shape contiene apertura, existencia/obligatoriedad de `x` y valores. ContractCorpusTest.Case conserva las expectativas del manifest y su toString devuelve el ID para identificar la ejecución parametrizada. PackagedCliIT.Result captura código y streams del proceso. No pertenecen al modelo público del producto.

## Corpus, contratos pequeños y catálogo

`src/test/resources/openapi/corpus/cases.json` contiene 146 entradas con id, baseline, candidate, status, verdict, rules y diagnóstico opcional. ContractCorpusTest carga cada par, ordena reglas esperadas/obtenidas, comprueba ausencia o presencia del diagnóstico y repite la comparación verificando igualdad del informe. Exige IDs únicos y un mínimo de casos. No contiene expectativas de todos los perfiles ampliados: utiliza el analizador predeterminado de parámetros.

Los recursos fuera de corpus son pares legibles por funcionalidad: operaciones, parámetros, cuerpos, valores, objetos, arrays y respuestas. Las clases también construyen documentos mínimos en strings para variar una sola restricción sin mantener cientos de archivos duplicados. El inventario enumera todos los archivos; los métodos auxiliares de tests generan shapes/documentos o verifican informes/procesos, y no son API de producción.

`examples/catalog` es una API SaaS sintética de productos/pedidos con componentes compartidos. Base → compatible da 0, base → breaking da cuatro hallazgos y 1, base → review da PARTIAL/INDETERMINATE y 3. Su README detalla las modificaciones. No es un contrato real de cliente ni valida hipótesis comerciales.

PackagedCliIT usa guard.executableJar proporcionado por verify. Ejecuta procesos reales, separa stdout/stderr y comprueba JSON completo, encoding y códigos. Fuerza una codificación windows-1252 exterior para comprobar que el informe siga siendo UTF-8; cada proceso tiene un límite de 20 segundos y limpieza si queda vivo. Su record Result contiene código, stdout y stderr. El helper report verifica el formato/perfil histórico; las pruebas de perfiles ampliados hacen sus propias comprobaciones. Los tests de frontera emplean recursos locales controlados para demostrar que no se sigue una referencia externa; no dependen de servicios remotos.

## Cómo revisar un fallo

El candidato añade pruebas de integridad/transporte (incluidos ZIP peligrosos), procedencia/checkout y ocho escenarios del kit real copiado. GUARD_TEST_JAR selecciona el binario de candidato para los tests exteriores; los tests *IT siguen usando la propiedad del JAR realmente empaquetado. No solapar limpieza de target con tests de lanzador, hashes o paquetes. [Aceptación y evidencias](../candidate-roadmap.md).

La distribución añade ocho pruebas Node en action-runner.test.mjs y evaluation-package.test.mjs: repositorios consumidores, códigos 0/1/2/3, PR/merge queue, hash incorrecto, manifiesto/inventario, ejecución del JAR copiado y rechazo de sobrescritura. Requieren un build terminado. No prueban el runner remoto ni constituyen revisión legal de licencias. [Guía de entrega](../evaluable-distribution.md).

1. Abrir `target/surefire-reports` o `target/integration-test-reports`, según la fase.
2. Localizar el método/caso en el inventario; en parametrizados mirar los argumentos o ID del manifest.
3. Decidir el resultado esperado con dirección e inclusión, después compararlo con el código. No modificar el test únicamente para aceptar la salida actual.
4. Si el fallo está en el JAR, revisar el informe del proceso y stderr, no solo los tests del comando en memoria.
5. Una regresión nueva debe aportar un caso que falla antes del arreglo; preservar pruebas de perfiles históricos e identidad/procedencia.

Para seleccionar una clase: `mvnd -o '-Dtest=SchemaInclusionTest' test`. Para comprobar entrega: `mvnd -o clean verify`. El script PowerShell se verifica por separado contra el JAR construido. Ninguna cantidad de tests elimina los límites publicados del perfil ni sustituye un piloto con contratos reales.

Entrega de posiciones candidatas: 952 pruebas (927 habituales y 25 del JAR). SourceOutputTest aporta diez regresiones; HumanOutputTest y PackagedCliIT verifican la nueva presentación. Ver [casos y garantías](../source-locations.md). Las cifras anteriores corresponden a sus entregas.
