# Hallazgos visibles en GitHub Actions

Para instalarlo con los mínimos ajustes, empieza por la [guía rápida con workflow completo](github-actions-quickstart.md). Este documento explica la presentación y sus límites.

La integración presenta el resultado en tres lugares: estado del check **OpenAPI contract guard**, anotaciones asociadas al contrato candidato y resumen de ejecución. Se ejecuta al abrir o actualizar una PR, antes del merge. El repositorio consumidor decide si exige el check para fusionar.

## Qué verá el desarrollador

Ejemplo ilustrativo, no captura de una ejecución remota:

```text
OpenAPI contract guard: failed
contracts/catalog.yaml:27:3
PARAMETER_BECAME_REQUIRED [ID del hallazgo]
The parameter limit in query becomes required.
```

GitHub recibe comandos error o warning con ruta relativa, línea y columna. Las anotaciones aparecen en los detalles del check; su asociación visual al diff depende de la revisión y de las líneas presentes en ese diff. No se crean comentarios de conversación de PR ni se necesitan permisos de escritura. Ver [comandos oficiales](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-commands).

El resumen de Actions incluye archivos seleccionados, commits comparados, código de salida, tabla numerada con método/ruta/dirección/código de respuesta/media type, reglas/motivos/líneas y cobertura por contrato. Un mismo hallazgo con definición y uso separados genera varias anotaciones con el mismo ID, pero una sola fila. No se representa la línea 27 y la 89 como un rango continuo. El reporte JSON descargable conserva todos los hallazgos.

En consola aparece una línea por hallazgo lógico, sin tablas Markdown ni entidades como `&#95;`. Las anotaciones individuales quedan dentro del grupo desplegable `Candidate annotations`, conservando la navegación por posición. La misma definición compartida puede generar hallazgos distintos para operaciones diferentes. Los detalles de restricciones se expresan siempre como baseline → candidate, también en respuestas, por ejemplo `maximum: 100 -> 10`, `enum: [PAID, PENDING] -> [CANCELLED, PAID, PENDING]` y `maxItems: 100 -> 200`. Son diferencias del nodo cuya inclusión ya se refutó, no reglas nuevas ni ejemplos de payload completo. Se acotan a 20 valores de enum y 160 puntos de código Unicode por valor en esa explicación; los contratos originales conservan el contenido completo.

El mensaje distingue rupturas demostradas (`4 breaking changes detected.`), errores de entrada/ejecución y necesidad de revisión. Cuando una ruptura coexiste con cobertura incompleta se indica también la revisión pendiente.

## Permitir una ruptura intencionada

Detectar una ruptura y decidir si se permite son decisiones distintas. La Action conserva el bloqueo por defecto; el responsable del repositorio puede habilitar el modo informativo:

```yaml
- id: guard
  uses: ./.guard-action
  with:
    contracts-root: contracts
    fail-on-breaking: 'false'
```

| Comparación | `exit-code` del motor | Check con `'true'` | Check con `'false'` |
| --- | --- | --- | --- |
| Compatible dentro del perfil | 0 | 0 | 0 |
| Ruptura con análisis completo | 1 | 1 | 0 |
| Entrada/ejecución inválida | 2 | 2 | 2 |
| Revisión pendiente | 3 | 3 | 3 |
| Ruptura y revisión pendiente | 1 | 1 | 3 |

`check-exit-code` expone el estado del paso después de aplicar la política. `exit-code`, los informes y la severidad semántica permanecen intactos. Las anotaciones de rupturas permitidas se presentan como warning; los errores de entrada/ejecución mantienen error. El resumen declara la política: un check verde con `BREAKING` significa que el workflow permite la ruptura, nunca que el contrato sea compatible. También se permite retirar un contrato completo bajo esta política.

Esta opción se fija en el workflow del consumidor. No interpreta etiquetas, mensajes de commit ni incrementos de versión como aprobación automática. Para una excepción puntual puede habilitarse explícitamente en la PR y restaurarse después, conforme a la revisión y permisos del equipo. Los rulesets/protecciones de rama y los demás checks siguen gobernando el merge; la Action no los modifica. Fuentes de plataforma: [estados de checks](https://docs.github.com/en/pull-requests/reference/status-checks) y [anotaciones](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-commands).

El kit/ZIP anterior a esta mejora no reconoce el nuevo input. Preparar un paquete nuevo con el JAR y los scripts verificados y reemplazar `.guard-action` como conjunto completo, conservando su nuevo manifiesto; no mezclar scripts con hashes antiguos. Ver [distribución evaluable](evaluable-distribution.md). No se publica ni modifica automáticamente el repositorio consumidor.

Para regenerarlo localmente después del build, usando destinos nuevos:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/prepare-evaluation.ps1 -Destination dist/pr-feedback-2026-10-16/package
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/prepare-pilot.ps1 -Package dist/pr-feedback-2026-10-16/package -Destination dist/pr-feedback-2026-10-16/pilot-kit
node scripts/verify-package.mjs dist/pr-feedback-2026-10-16/pilot-kit/.guard-action
```

El kit incluye `.guard-action` completo y el workflow de ejemplo. Para actualizar un consumidor existente, conservar sus contratos, reemplazar el paquete completo e introducir la política deseada en su workflow. Esta preparación es una evaluación local, sin proof de candidato/release nueva ni publicación externa.

## Comentario en la conversación de la PR

Las anotaciones y el resumen del job no se trasladan automáticamente a Conversation. La opción `pr-comment` publica allí un comentario con estado visible, hallazgos agrupados, posiciones, operación y diferencias. Se actualiza el mismo comentario cuando cambia la comparación, incluso cuando queda compatible dentro del perfil o no hay cambios de contratos. No altera descripción, título, etiquetas ni comentarios de usuarios.

```yaml
permissions:
  contents: read
  pull-requests: write
concurrency:
  group: guard-${{ github.event.pull_request.number || github.ref }}
  cancel-in-progress: true
jobs:
  guard:
    runs-on: windows-latest
    steps:
      - uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1
        with:
          fetch-depth: 0
          persist-credentials: false
      - id: guard
        uses: ./.guard-action
        with:
          contracts-root: contracts
          fail-on-breaking: 'false'
          pr-comment: 'true'
```

Mantener los eventos pull_request/merge_group y el paso de conservación de artefactos del workflow completo. El comentario enlaza la ejecución y su intento; report.json y summary.md se descargan de sus artefactos cuando el upload termine correctamente. El extracto se limita a 48 000 unidades UTF-16, sin cortar pares Unicode, con aviso de truncamiento; los informes completos se conservan.

`github-token` usa por defecto `${{ github.token }}` y admite el token de workflow con los permisos del consumidor; se identifica como github-actions[bot]. No es una interfaz para tokens personales ni otros bots. El token no se transmite al subproceso de Git/Java. No se eleva a pull_request_target ni se añaden secrets. Solo publica para pull_request del mismo repositorio; forks, merge queue y otros eventos conservan análisis/informes pero devuelven skipped-fork o skipped-event. No se intenta publicar para esas PR usando credenciales privilegiadas.

Antes de escribir se consulta la PR actual: debe seguir abierta y coincidir su head, base y candidato analizado. Un marcador identifica el comentario propio por workflow y contracts-root; solo se actualizan comentarios de github-actions[bot], nunca de usuarios que imiten el marcador. Se comprueban run ID/intento para evitar sobrescribir un informe más reciente y se vuelve a comprobar la PR después de paginar. El workflow debe mantener la concurrencia por PR: la API no proporciona aquí una escritura atómica que elimine todas las carreras. No se promete que el comentario quede fijado arriba de la conversación.

`comment-status` informa disabled, created, updated, skipped-event, skipped-fork, skipped-stale o failed. Si se habilita la publicación y falla por token, permisos, red o búsqueda superior a 1000 comentarios, el paso falla con código 2, conservando el código del motor y las rutas de informes; check-exit-code refleja 2. No se presenta un check verde mientras falta la publicación solicitada. El cuerpo incluye comparación/cobertura y declara cuándo una ruptura está permitida; esa política sigue siendo independiente del comentario.

La plantilla del kit conserva la opción apagada y contents: read; activar ambas opciones anteriores expresamente en el consumidor. El paquete actualizado incluye pr-comment.mjs bajo integridad de manifiesto. [API oficial para comentarios de PR](https://docs.github.com/en/rest/issues/comments) y [consulta de PR](https://docs.github.com/en/rest/pulls/pulls#get-a-pull-request).

## Datos estructurados, sin interpretar texto

Los mensajes generados se presentan en inglés, incluidas las descripciones de las anotaciones. El JSON de `--format=ci` y `--format=json` lleva sangría para poder leerlo directamente en consola. La estructura, coordenadas, identificadores y códigos de salida se conservan; los consumidores deben parsear JSON, no depender del número de renglones ni del idioma de una descripción.

La CLI añade `--format=ci`. Es JSON con `formatVersion: 1`, `positionLimit: 8`, `report` y `findings`. report conserva exactamente el informe semántico que devuelve --format=json. Cada finding incluye id, ruleId, severity, source, description y positions. Los hallazgos de ruptura añaden `location` como extensión aditiva, con la misma ubicación semántica del cambio (método, ruta y contexto); los diagnósticos sin contexto de operación no lo inventan. La presentación también obtiene esa ubicación mediante el ID en `report.changes` para envolturas CI anteriores. Cada posición contiene line/column (desde 1), approximate y role (definition, usage, context o diagnostic). No incluye rutas temporales. Los IDs de rupturas son los del dominio; diagnostic-N solo identifica orden dentro del informe.

CandidateSource obtiene posiciones del texto original ya leído; no añade E/S ni reglas. ReportRenderer construye la envoltura exterior. El núcleo continúa siendo Java puro. El índice aplica los límites existentes: un documento inválido o sin índice fiable tiene positions vacío. Se conservan como máximo ocho posiciones por hallazgo; son contexto de navegación, no una prueba de que cada keyword haya cambiado. Para una eliminación se anota solamente el contenedor existente, con approximate=true.

El lanzador Git pide ci al JAR. Conserva report y añade findings por archivo en su envoltura Git v1 (extensión aditiva). Asocia posiciones a candidatePath de la revisión analizada, incluidas las rutas nuevas de renombrados. No utiliza el working tree sucio ni rutas de snapshots temporales. Los contratos eliminados, fallos de ejecución y diagnósticos anteriores se muestran sin file/line para no inventar una línea del candidato.

El workflow actual compara la base con el merge propuesto GITHUB_SHA. Las coordenadas se refieren a esa revisión, que puede diferir del head de la PR. Consultar los hashes del resumen cuando el diff de la PR no coincida. Ver [selección Git](git-integration.md).

## Decisión y límites de presentación

- Rupturas: anotaciones error con bloqueo o warning cuando la política las permite. Fallos de entrada/ejecución: error. Incertidumbre: warning, pero sigue bloqueando.
- El proceso conserva códigos 0/1/2/3; publicar un error de Actions no sustituye devolver un código fallido.
- El emisor publica hasta 50 anotaciones por ejecución y avisa de las omitidas. La tabla se limita a 200 hallazgos; report.json conserva todos. Estos son presupuestos propios, no una garantía sobre los límites de la interfaz de GitHub.
- Los datos externos se escapan por separado para comandos y Markdown, incluidos porcentajes, saltos, comas, HTML y caracteres de control.
- No se publica una Action instalable ni una release mediante este incremento. La distribución para otros repositorios sigue pendiente. Proteger main en este repositorio es opcional.

## Probar localmente

```powershell
mvnd -o clean verify
node --test scripts/git-contracts.test.mjs scripts/github-workflow.test.mjs scripts/github-presentation.test.mjs
java -jar target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar --profile=full --format=ci src/test/resources/openapi/parameters-baseline.yaml src/test/resources/openapi/parameters-required.yaml
```

El último comando devuelve 1 porque encuentra rupturas. Las pruebas usan repositorios Git temporales, el JAR real y variables de Actions simuladas; verifican comandos emitidos, rutas, resumen y códigos. No certifican que GitHub haya renderizado una ejecución real.

## Comprobarlo en una PR real

1. Incorporar el workflow y el contrato base en un repositorio de prueba. Para otro proyecto se necesita además distribuir el JAR y scripts; copiar solo YAML todavía no basta.
2. Abrir una PR que vuelva obligatorio un parámetro existente. Esperar el check fallido y abrir Checks y los detalles de la ejecución.
3. Confirmar ruta candidata, línea, regla y motivo; abrir el resumen y descargar report.json. Comparar las líneas con el commit candidato indicado.
4. Repetir con cambio compatible, referencia interna, eliminación, entrada inválida, incertidumbre y PR sin cambios de contratos. Esta última debe pasar sin arrancar Java.
5. Guardar enlaces a las ejecuciones. Solo después decidir si el consumidor configura el check como obligatorio. Ver [checks de GitHub](https://docs.github.com/en/pull-requests/reference/status-checks).

La primera PR remota incompatible comparó `4bdd2f1` con el merge propuesto `2813545`: cuatro hallazgos correctos sobre quantity/status/maxItems, con código 1. Las capturas posteriores del piloto confirman el check verde con rupturas permitidas y la creación real del comentario de Conversation (candidato `66ecc12`, base `4bdd2f1`). Quedan por verificar navegación, descarga de artefactos, casos compatibles/indeterminados/inválidos y actualización del comentario al desaparecer las rupturas. La distribución comercial sigue pendiente; la batería local no sustituye esas comprobaciones.
