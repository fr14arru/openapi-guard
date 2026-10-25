> **Cambio de modelo (22 de octubre de 2026):** el código propio pasa a MIT y uso gratuito. Las condiciones comerciales, precios y aprobaciones pendientes que aparecen más abajo son historial, no requisitos vigentes de esta distribución. Las evidencias y licencias de terceros se conservan. Estado y siguientes pasos: [transición MIT](open-source.md).

# Distribución evaluable y Action reutilizable

Actualización del 19 de octubre: la [recopilación de atribuciones](third-party-review.md) incluye catálogo offline, textos completos y fuentes, con hashes de 35 JAR y del loader. El empaquetador rechaza componentes nuevos o evidencia alterada; la licencia propia sigue [pendiente de aprobación](product-license-draft.md). No declarar publicable un paquete por superar solo estas comprobaciones.

Actualización del 16 de octubre: código estable integrado en main y [candidato rc.2 verificado](pilot-delivery.md). La estrategia vigente es [evaluación pública y venta en autoservicio](commercial-launch.md), aún sin publicación. La [revisión inicial de terceros](third-party-review.md) identifica textos/loader/fuentes pendientes. Este documento describe el empaquetado técnico actual; copiar .guard-action sigue siendo necesario hasta implementar la instalación pública por referencia.

Para configurar un consumidor, utiliza la [guía rápida de GitHub Actions](github-actions-quickstart.md): paquete local, workflow listo para copiar, comentario visible y elección de bloqueo en una línea.

Continuación del 12 de octubre: [candidato privado, integridad y kit completo](candidate-roadmap.md). El manifiesto del paquete pasa a v2, con hashes de todos los archivos; la envoltura del informe CI sigue en v1. Los paquetes v1 del prototipo deben regenerarse para esta Action; no se rompe el formato semántico del motor.

Estado: prototipo local implementado el 11 de octubre de 2026. No hay release publicada ni Action listada en Marketplace. El motor conserva cobertura, perfiles y códigos. Esta entrega reduce la fricción del piloto; no cierra por sí sola D10 ni valida ventas.

## Decisión de producto

La siguiente entrega debe permitir que un consumidor compare contratos sin compilar nuestro repositorio. Se prepara un directorio autocontenido con JAR, Action, scripts estándar de Node, ejemplos y evidencia de atribuciones. No se instala Maven en el consumidor ni se añade Docker, un servicio remoto, credenciales de escritura o dependencias de ejecución Node.

La Action es compuesta: usa setup-java fijada a un commit para preparar Corretto 25 y ejecuta el adaptador Git existente. Requiere Git, Node >=20 y Bash disponibles en un runner estándar. Windows es el entorno verificado localmente; Linux/macOS todavía deben probarse en runners reales. Instalar Java en el runner implica descarga según su caché, aunque no se descargan motores ni paquetes npm. La construcción del paquete usa PowerShell 7 solo en el equipo del productor.

## Preparar un paquete local

```powershell
mvnd -o clean verify
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/prepare-evaluation.ps1 -Destination target/evaluation-pilot
java -jar target/evaluation-pilot/guard.jar --profile=full --format=text target/evaluation-pilot/examples/base.yaml target/evaluation-pilot/examples/breaking.yaml
```

El último comando devuelve 1. El destino debe ser nuevo: el script no borra ni sobrescribe paquetes anteriores. Si falla durante la preparación puede dejar un directorio parcial; solo un comando completado con código 0 produce una entrega evaluable. No hay publicación automática, ZIP público, tag ni descarga.

Contenido:

- guard.jar: binario ejecutable con dependencias, sin incluir Java.
- action.yml y scripts/action-runner.mjs, git-contracts.mjs, github-presentation.mjs: integración exterior.
- examples/: contratos base, compatible, incompatible e incierto.
- manifest.json: SHA256 del JAR y de las 35 bibliotecas de ejecución observadas; commit del checkout y señal de cambios sin commit.
- third-party/: textos de licencia, NOTICE y POM embebidos, con sus rutas originales registradas en el manifiesto.
- THIRD-PARTY-REVIEW.md: inventario de revisión pendiente; README.txt: instrucciones autónomas en inglés.

El manifiesto describe el checkout que prepara el paquete, no prueba que el JAR se compilara de ese commit. Para una release se exigirá build limpio del commit etiquetado, preparación inmediata y verificación del paquete resultante. No se presenta esta evidencia local como SBOM completa, firma, build reproducible ni auditoría legal.

## Probar la Action en otro repositorio

Hay dos modalidades técnicas:

1. Paquete local: colocar el directorio preparado en el runner y usar `uses: ./ruta/al/paquete`. Usa guard.jar y su hash del manifiesto. La transferencia y los términos del piloto deben acordarse antes de compartirlo.
2. Checkout fuente: usar la Action desde una revisión concreta de este repositorio y proporcionar `jar-path` y `jar-sha256`. No requiere compilar aquí, pero el consumidor debe tener el JAR ya disponible. Omitir el JAR en este modo no descarga una release inexistente.

Ejemplo de piloto con paquete local, cuya disponibilidad debe preparar el responsable antes de este paso. No es una receta de instalación remota completa:

```yaml
name: API compatibility
on:
  pull_request:
  merge_group:
    types: [checks_requested]
permissions:
  contents: read
jobs:
  guard:
    name: OpenAPI contract guard
    runs-on: windows-latest
    timeout-minutes: 20
    steps:
      - uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1
        with:
          fetch-depth: 0
          persist-credentials: false
      # Preparar aquí el paquete local en .guard-action antes del siguiente paso.
      - id: guard
        uses: ./.guard-action
        with:
          contracts-root: contracts
      - name: Retain reports
        if: always() && steps.guard.outputs.report-path != ''
        uses: actions/upload-artifact@cf430e030ddbb5b0abf93d22962f4752f3646cd9
        with:
          name: openapi-guard-reports
          path: |
            ${{ steps.guard.outputs.report-path }}
            ${{ steps.guard.outputs.summary-path }}
```

No elevar a pull_request_target ni añadir secrets al job de código no confiable. El consumidor decide si el check es obligatorio para fusionar. El workflow de este repositorio sigue compilando el motor para probar su desarrollo; no es la instalación propuesta para consumidores.

## Contrato público de la Action

| Entrada | Comportamiento |
| --- | --- |
| base | Por defecto pull_request.base.sha o merge_group.base_sha. |
| candidate | Por defecto GITHUB_SHA: merge propuesto, no head de PR. |
| contracts-root | Directorio relativo, contracts por defecto; conserva selección de contratos completos y política de archivos. |
| fail-on-breaking | `'true'` por defecto. `'false'` permite rupturas demostradas conservando `BREAKING` e informes; errores y revisión siguen fallando. Solo acepta esos dos valores. |
| pr-comment | `'false'` por defecto. `'true'` crea/actualiza un comentario en PR del mismo repositorio; requiere pull-requests: write. |
| github-token | Token de workflow, github.token por defecto; usado solo por el publicador opcional. |
| jar-path | Ruta de un JAR de piloto, relativa al workspace o absoluta; si se omite, JAR del paquete. |
| jar-sha256 | SHA256 esperado obligatorio para JAR de piloto; modo paquete lee el manifiesto. |

Otros eventos necesitan base y candidate explícitos. La Action no hace checkout ni adivina una rama base; necesita ambos commits disponibles en el checkout del consumidor. Los inputs pasan por variables de entorno y argumentos de procesos, nunca se interpolan como código shell.

| Salida | Contenido |
| --- | --- |
| exit-code | 0 compatible dentro del perfil; 1 ruptura; 2 inválido; 3 revisión. |
| check-exit-code | Estado del paso tras aplicar la política; puede ser 0 con exit-code 1, o 3 si además queda revisión. |
| comment-status | Estado de publicación opcional: disabled, created, updated, skipped-event, skipped-fork, skipped-stale o failed. |
| report-path | Ruta absoluta del JSON generado en target/openapi-guard-* del consumidor. |
| summary-path | Ruta absoluta del Markdown de esa ejecución. |

Por defecto los códigos 1/2/3 fallan el paso. Con `fail-on-breaking: 'false'`, una ruptura con cobertura completa conserva exit-code 1 y devuelve check-exit-code 0; si hay revisión pendiente devuelve 3. Entradas inválidas siguen devolviendo 2. Las salidas se escriben antes de terminar cuando hay informe válido. La envoltura Git añade `policy.failOnBreaking`; el informe semántico y la CLI conservan sus códigos y veredictos. Un error previo, por ejemplo hash incorrecto o política inválida, no publica rutas de informes anteriores. Cada ejecución usa un directorio nuevo, conservado para upload-artifact con always(). No se envían contratos a un servicio: se leen commits y la comparación es local al runner. Anotaciones, resumen y resultados siguen los [límites ya documentados](github-annotations.md).

## Qué falta para cerrar D10 y publicar

1. Decidir licencia y términos del código propio, binario y Action. Actualmente no hay LICENSE del producto en la raíz; no se inventa una licencia comercial ni se presume permiso de redistribución por ser un repositorio visible.
2. Revisar las 35 bibliotecas reales, sus transitivas, el loader de Boot y textos de componentes incluidos. Hay familias Apache-2.0, MIT y otras con alternativas de licencia; no basta con decir que Swagger usa Apache. Textos embebidos ausentes o licencias heredadas del POM necesitan evidencia adicional. La sección 4 de [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) contiene obligaciones de redistribución; el inventario no constituye asesoramiento ni aprobación legal.
3. Fijar una versión de evaluación sin SNAPSHOT y construir desde un checkout limpio. Crear archivo distribuible y checksum; probar después de extraerlo, con Java y sin Maven en PATH.
4. Validar PR reales: equivalente, compatible, ruptura, referencias, eliminación, inválido, incierto, sin cambios, renombrado, fork y merge queue cuando aplique. Guardar enlaces y comprobar base/candidato, líneas, artifacts y permisos.
5. Ejecutar una matriz Windows/Linux; macOS solo si se ofrece soporte. Medir duración e instalación de Java 25. Mantener Java 25 por ahora; reconsiderarlo solo si es una barrera demostrada.
6. Preparar un repositorio de distribución mínimo, action.yml y código necesario, separado del portal/build del motor si se elige Marketplace. Definir relación entre versión de Action y motor; fijar commits para consumo estable y documentar actualizaciones.
7. Autorizar explícitamente la publicación de release/artefactos/Marketplace. Esta tarea prepara archivos pero no los publica fuera de Git.

GitHub exige para [publicar una Action](https://docs.github.com/en/actions/how-tos/create-and-publish-actions/publish-in-github-marketplace) repositorio público, metadatos y release, con requisitos adicionales de nombre y cuenta. Los [planes de pago de Marketplace](https://docs.github.com/en/apps/github-marketplace/selling-your-app-on-github-marketplace/pricing-plans-for-github-marketplace-apps) corresponden a Apps, no a publicar una Action. Decidir pronto si se vende licencia/soporte fuera de ese mecanismo o si la demanda justifica una App; no construir facturación ni servicio central todavía.

Propuesta comercial para validar, no decisión de licencia: mantener la integración sencilla y ofrecer un piloto con acompañamiento manual; comprobar después si los equipos pagarían por soporte y actualizaciones. Una integración publicada en Marketplace no garantiza ingresos recurrentes. Si comprar dentro de GitHub es un requisito indispensable, evaluar los costes y requisitos de una App antes de construirla. Fran debe elegir los términos del código propio y autorizar distribución/publicación; ninguna elección de licencia queda aprobada por este documento.

## Validación y siguientes incrementos

Las ocho pruebas iniciales fallaron antes de implementar. action-runner.test.mjs usa repositorios temporales externos, commits y el JAR real, comprueba 0/1/2/3, PR/merge queue/eventos explícitos, hash incorrecto, informes y anotaciones. evaluation-package.test.mjs prepara un paquete fuera del checkout, verifica inventario/hashes, archivos necesarios, JAR ejecutable y rechazo de sobrescritura. No simula que GitHub haya ejecutado action.yml: esa evidencia sigue pendiente.

Prioridad inmediata: acordar términos para el piloto y probarlo en otro repositorio. Después, cerrar atribuciones y distribución versionada; finalmente, validar y publicar la Action con autorización. Ampliar reglas solo cuando los contratos del piloto indiquen el siguiente bloqueo.
