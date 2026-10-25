# Validación de contratos en Git y GitHub

## Estado de la entrega

El usuario autorizó tres incrementos con TDD: comparación entre revisiones, workflow de PR y check obligatorio en main. Los dos primeros tienen implementación y pruebas locales. El tercero exige ejecuciones reales y acceso a configuración de GitHub: no está completado por escribir YAML. El navegador disponible no tiene sesión autenticada; las descargas del runner requieren la aprobación concreta solicitada y no se han ejecutado localmente. La documentación Docusaurus en curso conserva sus archivos y requiere su aprobación de instalación independiente.

No se añaden dependencias al pom, infraestructura ni reglas semánticas nuevas. Node, ya disponible en el equipo, utiliza módulos estándar para automatización externa; el motor sigue siendo el JAR Java puro con Swagger Parser y su arranque Boot no web.

## Uso local

```powershell
mvnd -o clean verify
node scripts/git-contracts.mjs --base main --candidate HEAD
node scripts/git-contracts.mjs --base main --candidate HEAD --root 'api specs'
node --test scripts/git-contracts.test.mjs scripts/github-workflow.test.mjs
```

Se necesitan Git, Java 25, Node >= 20 y el JAR compilado. Si Node no está en PATH, usar su instalación existente. La receta PowerShell para comparar archivos directamente sigue en [CI](ci.md).

El directorio por defecto es `contracts/`, recursivamente, seleccionando `.yaml`, `.yml` y `.json` sin distinguir mayúsculas. Todos esos archivos deben ser contratos completos; no incluir YAML de configuración ni fragmentos. No se resuelven referencias externas. Los argumentos adicionales son `--repo`, `--jar` y `--output`; se rechazan opciones desconocidas o duplicadas y revisiones inválidas. La salida debe estar bajo `target/` del repositorio.

Este repositorio no contiene todavía contratos propios bajo `contracts/`. El check puede dar éxito por no haber cambios seleccionados; eso queda explícito en el resumen. Para ensayarlo, copiar el contrato sintético `examples/catalog/base.yaml` a `contracts/catalog.yaml`, versionarlo como base y después modificar ese mismo archivo en una rama de prueba con las variantes compatible/breaking/review. No basta con modificar fixtures de `examples/`, porque están fuera de la selección por defecto.

Se leen blobs de commits, nunca archivos modificados sin commit. Se generan `target/git-guard/report.json` y `summary.md`; una nueva ejecución sobrescribe esos dos informes. Usar un subdirectorio diferente para conservar otra ejecución. Un fallo de argumentos/escritura puede no producir informe: no leer informes anteriores después de ese fallo y bloquear por el código del proceso.

## Política de archivos

| Cambio | Comportamiento |
| --- | --- |
| Modificación | Comparar blob antiguo y nuevo con `full`. |
| Adición | Validar nuevo contra sí mismo; aceptar solo cobertura completa. No existe contrato previo. |
| Eliminación | Bloquear con `CONTRACT_FILE_REMOVED`, severidad ERROR, ubicación antigua y descripción. Es política de integración, no regla semántica OpenAPI. |
| Renombrado reconocido por Git | Comparar ruta antigua y nueva; el nombre de archivo por sí mismo no rompe. |
| Salida del directorio vigilado | Tratar como eliminación. |
| Entrada al directorio vigilado | Tratar como adición. |
| Sin cambios seleccionados | Código 0 y lista vacía, sin arrancar Java. |

Git identifica renombrados por similitud. Un cambio grande puede aparecer como eliminación más adición y bloquear conservadoramente. No se infiere identidad por `info.title`. Eliminar el archivo no prueba que el servicio se elimine, pero impide mantener su protección: esa es la razón de la política. Un contrato nuevo con construcciones opacas puede dar código 3 al validarlo contra sí mismo.

## Informes y decisión de CI

El envoltorio JSON contiene `formatVersion: 1`, `profile: full`, hashes resueltos `base` y `candidate`, `root`, `exitCode` y `files`. Cada archivo conserva `kind`, `path`, `baselinePath`, `candidatePath` y su `exitCode`. Para comparaciones, `report` conserva exactamente el informe original del motor: reglas, severidades, ubicaciones, descripciones, diagnósticos y exclusiones. Las eliminaciones incluyen los campos de política indicados; los fallos de ejecución tienen `error`.

La prioridad global es error de entrada/ejecución (2), ruptura (1), incertidumbre (3), éxito (0). Se conservan todos los resultados por archivo, aunque uno falle. Los códigos 1/2/3 bloquean; solo 0 permite continuar. Compatibilidad se limita al perfil, nunca a toda la API. El resumen presenta archivos y códigos; los detalles de reglas están en JSON. Si existe `GITHUB_STEP_SUMMARY`, se añade allí el resumen. No se publican comentarios ni se requieren permisos de escritura.

## Código y límites

| Pieza de `scripts/git-contracts.mjs` | Responsabilidad |
| --- | --- |
| `git` | Ejecutar Git sin shell, con timeout y límite de salida. |
| `revision` / `watchedRoot` | Validar selección y resolver commits. |
| `changes` / `selected` | Diff delimitado por NUL, clasificación, filtrado y orden determinista. |
| `snapshot` | Extraer blobs regulares, exigir UTF-8 estricto y máximo 5 MiB. |
| `runJava` | Invocar JAR con `full`, capturar JSON y comprobar código/salida. |
| `compareRevisions` | Orquestar, aislar errores por archivo, agregar y limpiar temporales propios. |
| `escape` / `summarize` | Resumen que escapa HTML/Markdown en rutas. |
| `parseArguments` / `main` | CLI, persistencia de informes y código global. |

Cada Git tiene timeout de 30 segundos y salida máxima 32 MiB. Cada Java tiene timeout de 60 segundos y salida máxima 20 MiB. El job completo tiene 20 minutos. Los excesos bloquean, no omiten contratos. Se rechazan symlinks y submódulos seleccionados; no se lee su destino. Los argumentos nunca se interpolan en shell. Los blobs se escriben en nombres temporales propios, sin reproducir rutas del contrato en disco. Solo se borra el directorio obtenido con `mkdtemp`, comprobando su ubicación.

El adaptador detecta YAML/JSON por contenido, por lo que los temporales no necesitan extensión. Los informes originales pueden citar temporales como fuentes; el envoltorio establece su correspondencia con rutas y hashes. Las ubicaciones semánticas no se alteran.

## Workflow

La presentación añade [anotaciones por línea y resumen detallado](github-annotations.md). El lanzador consume --format=ci sin interpretar texto de consola, conserva report y añade findings con posiciones candidatas por archivo. El workflow devuelve el mismo código y mantiene permisos de lectura. La validación de renderizado remoto sigue pendiente.

[openapi-contract-guard.yml](../.github/workflows/openapi-contract-guard.yml) se ejecuta en PR hacia main y `merge_group` para cola de merge. No tiene filtro `paths`, para que el check aparezca también sin cambios de contratos. Usa el commit objetivo `pull_request.base.sha` o `merge_group.base_sha` y el merge propuesto `GITHUB_SHA`, con checkout e historial completos.

Un job estable **OpenAPI contract guard** usa Windows, PowerShell, Corretto 25 y Maven 3.10.0. Primero ejecuta `mvn clean verify`, pruebas de Git/workflow y diez comprobaciones de `test-ci.ps1`; después compara contratos. Conserva informes durante siete días, también cuando falla la comparación. Un fallo previo puede no generar artefactos, pero falla el job.

Las Actions oficiales se fijan a commits consultados el 2026-10-09: checkout v7, setup-java v6 y upload-artifact v7. Los runners deben soportar sus runtimes. `scripts/setup-maven.ps1` descarga Maven de Apache en el runner y comprueba SHA512 antes de extraer. `-ArchivePath`, `-ExpectedSha512` y `-ValidateArchiveOnly` permiten verificar integridad offline; no instalan nada. El checksum del mismo origen HTTPS verifica integridad, no una firma GPG independiente. No se usa mvnd ni caché adicional en runners efímeros.

Permiso `contents: read`, checkout sin credenciales persistentes, sin secrets, `pull_request_target` ni `continue-on-error`. Las PR de forks pueden requerir aprobación del mantenedor para ejecutar Actions. Una ejecución pendiente no debe permitir merge. El build ejecuta código de la PR con token de lectura: no elevar permisos. Cambios al workflow, scripts y pom necesitan revisión humana para evitar debilitar el control.

Este repositorio compila y usa su propio motor. Para proteger otro producto se necesitará una distribución versionada del JAR y el lanzador; copiar solo YAML no funciona. Existe un prototipo de Action reutilizable con paquete local y JAR precompilado; ver [distribución evaluable](evaluable-distribution.md). No se ha publicado una release, listado de Marketplace ni GitHub App.

## Check obligatorio en el consumidor: decisión opcional

Proteger main en este repositorio no es requisito del producto. Tras distribuir la herramienta y validar la integración en un repositorio consumidor, su responsable decide si exige el check. La entrega actual no configura protección ni publica una Action instalable.

Antes de activar protección, registrar PR reales de prueba con contrato previo común: compatible (0), ruptura (1), incertidumbre (3), entrada inválida (2) y sin contratos. Los fixtures del catálogo sirven como contenido, pero primero debe existir la base en el destino de prueba. Añadir una API nueva con contenido breaking solo la valida contra sí misma y no demuestra una ruptura respecto a una versión anterior.

Comprobar hashes, informe, resumen y estado del check; guardar enlaces de las ejecuciones. Las pruebas locales no sustituyen esta evidencia remota. Después:

1. Abrir Settings → Rules → Rulesets o la protección existente de main, conservando todos los requisitos anteriores.
2. Añadir **OpenAPI contract guard** a los status checks requeridos; seleccionar GitHub Actions como origen si está disponible.
3. Exigir actualización de la rama antes del merge o verificar la configuración equivalente de la cola.
4. Confirmar que ruptura, error e incertidumbre bloquean merge y la compatible permite continuar cuando los demás checks pasan.

El plan de GitHub y tipo de repositorio deben permitir esta protección, y se necesita acceso administrativo. Si no hay acceso o soporte, registrar el bloqueo y no afirmar que main está protegida. Este incremento no autoriza merge de la rama de implementación, force push, borrar ramas remotas ni cambiar credenciales.

## Tests y TDD

La batería comenzó fallando por ausencia del lanzador. Se implementó y verificó con repositorios reales. Las cuatro comprobaciones del workflow se ejecutaron fallando por ausencia del YAML antes de escribirlo y su bootstrap. Las pruebas posteriores del JAR validan compatibilidad, ruptura, incertidumbre y entrada inválida de extremo a extremo.

`git-contracts.test.mjs` combina callbacks para aislar orquestación y JAR real para integración. Cubre cambios, adiciones, retiradas, renombrados, entradas/salidas de directorio, múltiples archivos/prioridad, Unicode/espacios/corchetes, trabajo sucio ignorado, revisiones inválidas, ausencia de JAR, symlinks, tamaño y UTF-8. `github-workflow.test.mjs` verifica política, eventos, permisos, pins, build e informes; no prueba ejecución en GitHub. Estas baterías son externas al build Maven y el workflow ejecuta ambas.

Al cambiar política/selección/salida, añadir primero una prueba que falle. Versionar cambios del contrato público del envoltorio. No alterar perfiles del motor por cambios de automatización. El piloto comercial con contratos reales sigue pendiente.

Verificación local de esta entrega: `mvnd -o clean verify` y `mvn -o verify`, ambos con 935 pruebas JUnit sin fallos. Las 24 pruebas Git, cinco de workflow/bootstrap y diez comprobaciones anteriores de CI pasan. El YAML también se parseó con SnakeYAML de la caché existente y rechazo de claves duplicadas; no se instaló un validador adicional. Una regresión de severidad se ejecutó primero en rojo y corrigió la eliminación a ERROR, coherente con el vocabulario público del motor. La generación del contenido documental incorpora ahora la guía y los archivos de automatización. Esto no certifica un build Docusaurus ni una ejecución de Actions.

Fuentes oficiales: [eventos Actions](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows), [checks requeridos](https://docs.github.com/en/pull-requests/how-tos/merge-and-close-pull-requests/troubleshooting-required-status-checks), [Maven](https://maven.apache.org/download.cgi), [checkout](https://github.com/actions/checkout), [setup-java](https://github.com/actions/setup-java), [upload-artifact](https://github.com/actions/upload-artifact).
