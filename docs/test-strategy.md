# Batería de pruebas del perfil de operaciones

Fecha: 2026-10-08. Objetivo: asegurar la funcionalidad existente antes de añadir reglas de parámetros. No se añaden reglas, librerías ni plugins. La batería usa JUnit 5, fixtures pequeñas y procesos Java reales.

Esta sección inicial documenta el refuerzo previo a D03. D03 añade reglas de parámetros y extiende la misma batería a 247 pruebas (231 habituales y 16 del JAR), incluyendo [146 comparaciones sintéticas](contract-corpus.md). El perfil actual está documentado en [parameter-presence-profile.md](parameter-presence-profile.md).

## Niveles y ejecución

| Nivel | Qué atraviesa | Ejecución |
| --- | --- | --- |
| Dominio | Objetos inmutables y regla de operaciones | `mvnd test` |
| Integración del motor | Contenido → Jackson/Swagger → caso de uso → informe, sin Spring | `mvnd test` |
| Adaptador CLI y arranque | Archivos reales, JSON, contexto Boot y proceso sobre classpath | `mvnd test` |
| Artefacto distribuible | `java -jar` en un directorio temporal diferente, archivos y salida del proceso | `mvnd verify` después de empaquetar |

Se reutiliza Surefire 3.5.6: su ejecución normal descubre `*Test`, y una segunda ejecución en `verify` descubre exclusivamente `*IT`. El nombre `ContractBoundaryIntegrationTest` pertenece al primer grupo porque no requiere empaquetado; `PackagedCliIT` pertenece al segundo. No se necesita Failsafe para este proyecto sin servicios que iniciar/detener. Reconsiderar esa elección cuando existan recursos con un ciclo de integración propio.

```powershell
mvnd -o test
mvnd -o clean verify
mvn -o verify
```

`-o` permite repetir la comprobación sin descargas cuando la caché está completa. `mvnd clean install` también recorre `verify`, por lo que ejecuta ambos grupos antes de instalar el artefacto local. `mvnd test` no ejecuta la batería del JAR: para asegurar toda la entrega hay que usar `verify`.

Informes: `target/surefire-reports/` para el primer grupo y `target/integration-test-reports/` para el JAR. Las pruebas del JAR fallan si falta el artefacto, si el proceso no termina en 20 segundos o si stdout contiene texto adicional al JSON. No se omiten silenciosamente. Los procesos se cierran al terminar la prueba o se destruyen tras un timeout.

## Matriz de regresión

| Comportamiento | Evidencia automática |
| --- | --- |
| Endpoint eliminado | Un hallazgo de endpoint, sin duplicar todos sus métodos; ruta, severidad y puntero original comprobados |
| Método eliminado | Los ocho métodos atraviesan el parser real; el dominio y el JAR comprueban su identidad |
| Equivalencia y altas | YAML/JSON con distinto orden, identidad y nuevas operaciones sin falsos positivos |
| Familias de versión | Operaciones 3.0 y 3.1; comparación entre familias y versiones no soportadas rechazada |
| Incertidumbre | Plantilla renombrada → PARTIAL/INDETERMINATE/código 3; ruptura independiente + incertidumbre → PARTIAL/BREAKING/código 1 |
| Informe público | Perfil, exclusiones, reglas, SHA-256, severidad, ruta/método, JSON Pointer y ausencia de puntero candidato |
| Determinismo | Dos JVM independientes producen exactamente el mismo JSON y los mismos identificadores |
| Uso real del JAR | Códigos 0/1/2/3, rutas con espacios y acentos, directorio de trabajo distinto, UTF-8 forzado frente a streams nativos Windows-1252 |
| Argumentos | Cero, uno o tres argumentos → código 2, uso en stderr y stdout vacío |
| Archivos | Archivo ausente, UTF-8 inválido y tamaño excesivo → diagnóstico de lectura, sin eliminaciones especulativas |
| Contratos inválidos | Baseline o candidata malformada → FAILED/INDETERMINATE, origen correcto y sin hallazgos |
| Límites del parser | Documento exactamente de 5 MiB aceptado y un byte más rechazado; límite contado en bytes UTF-8; profundidad excesiva en YAML/JSON rechazada |
| Sintaxis peligrosa | Alias de mapa/secuencia/escalar, claves duplicadas y documentos múltiples rechazados |
| Referencias | Internas existentes y recursivas en esquemas excluidos, referencias rotas y Path Item con ref rechazados según el perfil |
| Ausencia de fetch HTTP | Un socket de observación en loopback confirma que una referencia HTTP rechazada no abre conexión |
| Rutas sin operaciones | Paths vacío y Path Items con solo metadata no producen falsas eliminaciones |

El socket de observación vive solo durante una prueba, usa un puerto efímero y se cierra al terminar. No requiere red externa, servidor desplegado ni infraestructura del producto. La comprobación de referencias a archivo es de rechazo; no instrumenta todas las lecturas del sistema de archivos. No afirmar que esta batería demuestra ausencia universal de E/S.

## Resultado y límites

`mvnd -o clean verify` y `mvn -o verify` (Maven 3.10.0) pasan con 51 pruebas en el primer grupo y 11 pruebas del JAR: 62 en total, cero fallos, errores u omisiones. La batería conserva las 36 pruebas anteriores y añade 26. No se han requerido correcciones de producción.

Esta evidencia asegura las rutas probadas del perfil `operations-only-v1`. No demuestra validación completa de OpenAPI, semántica de parámetros/esquemas, todos los dialectos, SLA, comportamiento del servidor real ni compatibilidad global. Falta un corpus de contratos de clientes y CI en otros sistemas operativos; aquí se verifica Windows/Corretto 25. Mantener los límites de [operations-profile.md](operations-profile.md).

## Puerta para el siguiente incremento

Antes de ampliar el perfil: ambos builds correctos, batería del JAR ejecutada, exclusiones explícitas y ningún test ignorado. Cada futura regla añadirá sus pruebas de dominio, integración sin Spring y al menos un recorrido del JAR. Una nueva regla no debe cambiar silenciosamente el significado del perfil existente.

Los tres incrementos de D03 están implementados; sus ciclos TDD y la evidencia se detallan en [next-steps.md](next-steps.md) y [development.md](development.md).
# Cobertura de los incrementos D05/D06/D07

Además de la batería histórica descrita a continuación, la entrega incorpora:

- Matriz de obligatoriedad y rangos de formatos de petición; retirada de cuerpos y métodos ambiguos como revisión.
- Tipos, enums, nullabilidad y límites conjuntos por dirección, con 36 pares de intervalos enteros contrastados mediante un oráculo independiente, Unicode y enums grandes.
- Objetos con properties/required/additionalProperties booleano: 196 pares contrastados con un oráculo independiente de objetos, más proyecciones de acceso, anidamiento, referencias, ciclos y componentes compartidos por petición/respuesta.
- Presupuesto explícito de 10.000 nodos normalizados y grafo acíclico compartido de 32 niveles, evitando expansión exponencial y falsos verdes.
- JAR real en ambos dialectos, JSON/texto, ayuda, opciones, determinismo, comparación inversa y cobertura parcial con ruptura demostrada.
- Rechazo de referencias externas activas en el perfil ampliado, observado con un socket local que no recibe conexiones.

Las expectativas semánticas proceden de los contratos y oráculos matemáticos explícitos, no de otra herramienta de diff. No se afirma cobertura universal de OpenAPI ni validación comercial. [Perfil ampliado](object-schemas-profile.md).

El cierre con `mvnd -o clean verify` y `mvn -o verify` ejecuta 547 pruebas habituales y 21 del JAR en cada build: 568 en total, cero fallos/errores/omitidas. Incluye una regresión para el orden canónico del motivo de ruptura de una unión de tipos: el fallo se reprodujo entre procesos antes de ordenar el recorrido. Las fases rojas/verdes y la comprobación con Maven estándar se registran en [desarrollo](development.md).
