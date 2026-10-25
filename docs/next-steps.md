# Estado y siguientes pasos

Prioridad del 12 de octubre: entregar y revisar el [candidato privado](candidate-roadmap.md), ejecutar el kit en PR real, cerrar términos/atribuciones y decidir publicación. No añadir reglas ni infraestructura antes de validar esta adopción.

## Estado actual: 11 de octubre de 2026

Motor y CLI completos dentro de los perfiles publicados, 960 pruebas Java y presentación GitHub verificada localmente. La continuación prepara un prototipo de Action compuesta y un paquete local autocontenido, con ocho pruebas adicionales de consumo externo. No hay release pública ni validación de PR remota. Prioridad: términos del piloto, prueba en otro proyecto, atribuciones y distribución versionada; después publicación autorizada y mejoras guiadas por contratos reales. Ver [estado consolidado](portal/roadmap.md) y [distribución evaluable](evaluable-distribution.md).

## Evidencia histórica de los incrementos anteriores

D08 y D09 están implementados en la continuación autorizada, con perfiles `arrays` y `full`; se conserva el perfil histórico `schemas`. mvnd/Maven estándar pasan 935 pruebas cada uno. La receta CI neutral dispone de scripts PowerShell 7 y diez comprobaciones contra el JAR real, incluido un catálogo SaaS sintético. Ver [arrays](array-schemas-profile.md), [política de respuestas](response-policy-profile.md) y [CI](ci.md). Tras autorización explícita del usuario, los ocho commits de la entrega hasta ccd9e1c están integrados en main mediante fast-forward y publicados en GitHub.

Fecha: 2026-10-08. El usuario autorizó completar los tres pasos secuencialmente con TDD. D03a y D03b están implementados; D03c incorpora el JAR real, documentación y un corpus sintético solicitado al confirmar que no hay contratos reales disponibles.

| Paso | Implementación | Evidencia |
| --- | --- | --- |
| D03a | Parámetros efectivos: herencia, sustitución, identidad y procedencia, sin Spring en el núcleo | 16 casos de extracción, referencias internas y entradas inválidas; tests escritos y ejecutados antes de crear el modelo/extracción |
| D03b | REQUIRED_PARAMETER_ADDED y PARAMETER_BECAME_REQUIRED; retiradas/serialización opaca como revisión | 11 pruebas del dominio y 7 de integración; fase roja antes de las reglas; regresiones de perfil y origen de diagnósticos |
| D03c | Validación vertical y corpus reproducible | 16 casos del JAR y 146 pares del corpus; tests del JAR fallaron contra el artefacto previo y el test del corpus falló antes de crear los contratos |

El núcleo sigue siendo Java puro. No se añaden dependencias, módulos ni servicios. El perfil predeterminado pasa a `operations-parameter-presence-v1`; el anterior conserva su identificador y comportamiento al seleccionarlo explícitamente en Java. Ver [perfil y catálogo](parameter-presence-profile.md).

## Ejecutar la entrega

```powershell
mvnd -o clean verify
mvn -o verify
java -jar target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar src/test/resources/openapi/parameters-baseline.yaml src/test/resources/openapi/parameters-required.yaml
$LASTEXITCODE
```

El ejemplo devuelve tres rupturas y código 1. El corpus permite demostrar resultados compatibles, incompatibles, inválidos y parciales con expectativas explícitas: [contratos y comandos](contract-corpus.md). Para validar toda la entrega hay que llegar a verify, no basta con test.

## Incrementos aprobados posteriormente

Los tres incrementos aprobados están implementados con TDD, junto con salida legible. D05 introduce `--profile=request-body`; D06 `--profile=schema-values`; D07 `--profile=schemas`, que reúne toda la cobertura nueva e incluye objetos, obligatoriedad, apertura y proyección readOnly/writeOnly. `--format=text` y `--help` están disponibles. [Perfil ampliado y demostración](object-schemas-profile.md). El predeterminado histórico de parámetros se conserva y queda declarado en la ayuda.

## Validación comercial y ampliaciones

1. Validar el informe con contratos reales cuando estén disponibles. El corpus demuestra funcionamiento técnico; no demuestra utilidad comercial ni ausencia universal de falsos positivos.
2. Evaluar si referencias en Path Items o presencia de arrays/objetos bloquean el uso. Resolver el bloqueador concreto antes de ampliar toda la semántica.
3. D08/D09 ya cubren arrays homogéneos y política acotada de respuestas. Medir bloqueadores reales de composición, serialización, fallback o negociación antes de ampliar semántica. No ampliar infraestructura antes de validar estas necesidades.

El usuario confirmó que no dispone de contratos reales y pidió generarlos. Esa validación se sustituye por el corpus sintético para D03c; la validación comercial queda pendiente, no se declara realizada. Las nuevas descargas futuras siguen sujetas a [AGENTS.md](../AGENTS.md).
