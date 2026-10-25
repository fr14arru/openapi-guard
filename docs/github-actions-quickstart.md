# Configurar GitHub Actions en tu proyecto

Con esta receta obtendrás un comentario en la conversación de las PR del mismo repositorio, anotaciones sobre el contrato candidato y los informes descargables. Solo necesitas incorporar el paquete y copiar un workflow. Si tus contratos están en `contracts/`, puedes utilizarlo tal cual.

## 1. Coloca el paquete en tu repositorio

El candidato local rc.2 incluye un kit con una carpeta `.guard-action` y soporte de `pr-comment`; ver [registro de entrega](pilot-delivery.md). Cópiala **completa** a la raíz de tu proyecto de evaluación autorizado, incluido el JAR y `manifest.json`. La carpeta debe llamarse exactamente `.guard-action` para usar el workflow siguiente sin cambios. La [instalación pública por referencia](commercial-launch.md) es el siguiente incremento; todavía no está disponible.

Copia también la entrada de `.gitattributes` del kit. Si ya tienes ese archivo, añade esta línea conservando las demás:

```gitattributes
.guard-action/** -text
```

Así se conservan los bytes y hashes del paquete al hacer checkout en Windows. No edites archivos dentro de `.guard-action`; tu configuración pertenece al workflow.

No necesitas instalar Maven, configurar Java en el workflow ni crear un token personal. La Action prepara Corretto 25 y utiliza el token temporal que proporciona GitHub. Esta es la instalación del **piloto con paquete local**; todavía no hay una versión publicada en Marketplace ni una referencia remota de instalación que podamos recomendar.

## 2. Copia este workflow completo

Guárdalo como `.github/workflows/openapi-guard.yml`. La configuración siguiente reproduce la experiencia validada en el piloto: comentario visible y rupturas permitidas cuando el análisis está completo.

```yaml
name: OpenAPI contract review

on:
  pull_request:
  merge_group:
    types: [checks_requested]

permissions:
  contents: read
  pull-requests: write

concurrency:
  group: openapi-guard-${{ github.workflow }}-${{ github.event.pull_request.number || github.ref }}
  cancel-in-progress: true

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

      - id: guard
        uses: ./.guard-action
        with:
          contracts-root: contracts
          pr-comment: 'true'
          fail-on-breaking: 'false'

      - name: Retain comparison reports
        if: always() && steps.guard.outputs.report-path != ''
        uses: actions/upload-artifact@cf430e030ddbb5b0abf93d22962f4752f3646cd9
        with:
          name: openapi-guard-reports
          if-no-files-found: error
          retention-days: 7
          path: |
            ${{ steps.guard.outputs.report-path }}
            ${{ steps.guard.outputs.summary-path }}
```

**El único ajuste habitual es `contracts-root`.** Si utilizas `api/contracts/`, sustituye `contracts` por `api/contracts`. Usa una ruta relativa con `/`, también en Windows. Ese directorio debe contener contratos OpenAPI completos en `.yaml`, `.yml` o `.json`, versionados en Git; no fragmentos sueltos. El perfil inicial no resuelve referencias a otros archivos ni referencias remotas.

El permiso `pull-requests: write` permite que el bot cree y actualice su comentario. No hacen falta permisos de escritura sobre el código, secrets adicionales ni configurar `github-token`: la Action ya utiliza `${{ github.token }}`. Si una política de la organización impide ese permiso, el administrador deberá habilitarlo o tendrás que desactivar el comentario.

Los commits de las Actions oficiales están fijados para esta receta; mantenlos con las actualizaciones revisadas de la distribución.

## 3. Abre una PR con un cambio de contrato

Incorpora el paquete, `.gitattributes`, el workflow y un contrato base a la rama principal mediante el proceso habitual de tu equipo. Después crea una rama, modifica el contrato y abre una PR hacia esa base. No ejecutes un merge para probar: el análisis se hace antes, al abrir o actualizar la PR.

Al terminar la ejecución encontrarás:

- **Conversation:** un comentario de `github-actions[bot]` con el resultado, las operaciones afectadas, las líneas del candidato y los cambios concretos. Los nuevos commits actualizan ese comentario; si desaparecen las rupturas, también cambia su estado.
- **Checks:** el estado del job y las anotaciones para navegar a las posiciones del contrato candidato.
- **Actions → ejecución → Artifacts:** `openapi-guard-reports`, con `report.json` y `summary.md`, después de completarse la subida.

El comentario muestra los hashes comparados y enlaza la ejecución. GitHub analiza el merge propuesto, que puede tener un hash distinto del último commit de tu rama. Las líneas son ayudas de navegación a esa revisión, no un diff exacto de cada keyword.

## Elegir si una ruptura debe bloquear

| Lo que quieres | Configuración |
| --- | --- |
| Ver el informe y permitir rupturas intencionadas | Mantén `fail-on-breaking: 'false'` de esta receta. |
| Bloquear el check cuando haya rupturas | Cambia únicamente a `fail-on-breaking: 'true'`. |
| Mantener anotaciones e informes sin comentar la PR | Cambia a `pr-comment: 'false'` y retira `pull-requests: write`. |

**Un check verde con `BREAKING` significa que la política permite la ruptura, no que el contrato sea compatible.** Con ambas políticas, las entradas inválidas y la revisión pendiente siguen fallando. Una ruptura combinada con análisis parcial también requiere revisión. Las demás protecciones y comprobaciones del repositorio siguen aplicando.

Esta receta activa explícitamente el comentario y permite rupturas. Los valores predeterminados de la Action y la plantilla básica del kit conservan `pr-comment: 'false'` y `fail-on-breaking: 'true'`; copia la receta completa para obtener la experiencia descrita aquí.

## Si algo no aparece

| Síntoma | Qué comprobar |
| --- | --- |
| No se inicia ninguna ejecución | El workflow tiene el evento `pull_request`, Actions está habilitado y has abierto/actualizado una PR. Un push directo a main no activa esta receta. |
| `Can't find action.yml` o falta el JAR | `.guard-action` está completa, versionada y en la raíz; se hace checkout antes de invocarla. |
| Error de integridad del paquete | Conserva `.gitattributes` y reemplaza la carpeta completa desde el kit; no mezcles versiones ni normalices sus archivos. |
| Hay resumen, pero no comentario | `pr-comment: 'true'`, permiso `pull-requests: write` y ejecución de una PR del mismo repositorio. Revisa `comment-status` y el log. |
| Estado `skipped-fork` o `skipped-event` | La publicación se omite para forks, merge queue y otros eventos; el análisis y sus informes siguen disponibles. |
| Estado `skipped-stale` | La PR cambió o hay una ejecución más reciente. Revisa el resultado correspondiente al último candidato. |
| Fallo de publicación con HTTP 403 | El token no tiene el permiso necesario o una política del repositorio/organización lo impide. No añadas un token personal para resolverlo. |
| No aparecen archivos en Artifacts | Comprueba el paso `Retain comparison reports`. Una publicación fallida conserva las rutas de informes, pero el upload debe ejecutarse y terminar correctamente. |

En PR de forks GitHub puede requerir autorización para ejecutar el workflow según la configuración del repositorio. No cambies a `pull_request_target` para obtener el comentario. Si se solicita publicar y falla por permisos o red, el check falla con código 2 en lugar de ocultar esa falta de visibilidad.

Para actualizar la herramienta, sustituye `.guard-action` completa por el paquete nuevo y conserva tu workflow. Los paquetes congelados anteriores no incorporan funcionalidades nuevas automáticamente.

## Referencia

La captura del piloto confirma la publicación real del comentario con cuatro rupturas y merge permitido. Queda por contrastar en GitHub su actualización al corregir el contrato; ese caso está cubierto con API simulada. El piloto remoto y la revisión de atribuciones/licencia no se dan por cerrados por esta guía.

- [Qué analiza el perfil y qué excluye](response-policy-profile.md).
- [Presentación, permisos, códigos y límites de comentarios](github-annotations.md).
- [Entradas, salidas y distribución actual de la Action](evaluable-distribution.md).
- [Instalación y pruebas del kit privado](pilot-delivery.md).

Fuentes de plataforma: [permisos del token de workflow](https://docs.github.com/en/actions/security-for-github-actions/security-guides/automatic-token-authentication) y [eventos que activan workflows](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows).
