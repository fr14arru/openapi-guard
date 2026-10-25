# Revisar y mantener sin perder las garantías

## Puntos de entrada por tipo de cambio

| Necesidad | Dónde empezar | Qué comprobar después |
| --- | --- | --- |
| Argumentos, formato o códigos | ComparisonCommand / CliOptions / ReportRenderer | UsabilityTest, HumanOutputTest, ComparisonCommandTest y PackagedCliIT. |
| Guardado de informes | ReportFiles | Colisiones, hardlinks, reemplazo y fallos con código 2. |
| Primer uso | guard.ps1 | Demo, full por defecto y códigos con launcher.test.mjs; no ejecutar build concurrentemente en target. |
| Arranque/configuración | OpenApiGuardApplication y recursos | OpenApiGuardApplicationTest y JAR real. |
| Lectura, referencias o presupuesto | SwaggerContractAnalyzer y lectores | Tests del lector, ContractBoundaryIntegrationTest y procedencia. |
| Restricción conjunta de datos | SchemaValueContract y SchemaInclusion | Oráculo independiente, casos direccionales e incompletitud. |
| Disponibilidad de operaciones | RemovedOperationsRule | Plantillas ambiguas, orden e IDs históricos. |
| Presencia de parámetros/cuerpos | RequiredParametersRule / RequestBodiesRule | Representación, herencia y relajación sin falsos positivos. |
| Respuestas documentadas | ResponsePolicyRule | Separar política de disponibilidad de inclusión de payloads. |
| Nuevo fragmento soportado | ComparisonProfile, lector y regla | Perfil/versiones, exclusiones y conservación de anteriores. |
| Automatización local | scripts | Protección de inputs, UTF-8, códigos y test-ci.ps1. |

## Secuencia para un comportamiento futuro autorizado

1. Definir un contrato mínimo válido y la expectativa por petición y respuesta, con los casos de ruptura, compatibilidad e incertidumbre aplicables.
2. Escribir y ejecutar el test rojo. La documentación o los comentarios por sí solos no requieren inventar un comportamiento nuevo para cumplir TDD.
3. Normalizar únicamente la faceta necesaria en el adaptador. Mantener IO fuera de las reglas y no introducir clases/interfaces si no resuelven una necesidad.
4. Probar inclusión de restricciones conjuntas y satisfacibilidad antes de declarar un contraejemplo. Una laguna idéntica en ambos documentos no se considera resuelta.
5. Mantener procedencia de definición/uso e IDs estables; documentar cualquier cambio público con versión de perfil y Conventional Commit apropiados.
6. Ejecutar tests relevantes, luego verify del JAR y Maven estándar para una entrega relevante. Actualizar esta guía, inventario y documento del perfil.
7. Commit descriptivo en inglés y push de la rama de trabajo tras verificar. Un merge posterior requiere su autorización específica.

La documentación original no ampliaba funcionalidad; la entrega posterior autorizada añade [cuatro mejoras de uso](../product-usability.md). Nuevas dependencias/herramientas siguen requiriendo aprobación. Ejecutar `scripts/javadoc.ps1 -SkipBuild` tras verificar: el API público debe pasar doclint completo sin avisos.

## Trampas frecuentes durante la revisión

- `required` y `nullable` son distintos: una propiedad puede ser obligatoria y admitir null.
- Eliminar una propiedad no permite decidir nada sin apertura del objeto y dirección.
- Para respuesta se invierte source/target, pero el informe sigue nombrando los documentos baseline/candidate.
- Un hallazgo demostrado no elimina otras lagunas: BREAKING puede ser PARTIAL.
- La eliminación de documentación de parámetro/cuerpo no demuestra que el servidor deje de aceptar el dato.
- Un código de respuesta retirado pertenece a una política publicada de disponibilidad; no es una keyword de payload.
- Igualdad textual de una construcción no soportada no demuestra igualdad semántica.
- Map.copyOf protege colecciones; no convierte snapshots hechos a mano en contratos validados.
- Las caches son locales al documento/evaluación. Compartirlas globalmente cambiaría aislamiento, memoria y semántica de proyección.

## Documentación junto al código

Los Javadocs explican contratos de tipos y entradas principales; el detalle del algoritmo y sus helpers se concentra en inclusion.md/openapi.md para no duplicar páginas completas en cada método. El inventario conserva todos los miembros declarados, incluidos helpers privados y constructores. Los accessors/equals/hashCode/toString generados por records obedecen la semántica de Java; las excepciones relevantes, como canonización de ParameterId/ScalarValue y Pair por identidad, se explican expresamente.

Para actualizar el inventario, revisar los archivos versionados de src/main, src/test, scripts, examples y pom.xml; comprobar también recursos nuevos y métodos auxiliares. Los enlaces deben seguir apuntando a archivos existentes. No versionar target/javadoc ni otros resultados del build.

## Limitaciones y deuda deliberada

No hay API HTTP, persistencia ni cobros/activación de licencias. Sí existe integración GitHub mediante Action, informes y comentario opcional de PR; la distribución pública simplificada está pendiente. El arranque Boot aporta configuración familiar, mientras el caso de uso sigue invocable sin Boot. La CLI selecciona perfiles con un switch explícito y puede ensamblar el analizador directamente: es suficiente para el número actual de opciones.

El motor cubre un fragmento, no JSON Schema completo. Composición, ciclos, additionalProperties con schema, restricciones avanzadas de arrays, regex y negociación/fallback requieren revisión donde corresponda. La comprobación de entrada combina el parser y controles propios; no es un certificador completo del estándar. Las fronteras de tamaño/profundidad/budget están documentadas y comprobadas, pero no sustituyen aislamiento de recursos en una futura ejecución multiusuario.

Estas son restricciones existentes, no ampliaciones aprobadas. Los siguientes pasos de producto se mantienen en [roadmap](../roadmap.md) y [next-steps](../next-steps.md).
