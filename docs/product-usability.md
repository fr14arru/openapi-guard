# Cuatro objetivos de producto y guía de uso

## Decisión como PO

La prioridad es que un backend developer pruebe contratos y entienda un bloqueo sin instalar infraestructura ni conocer el código. El usuario autorizó elegir y completar al menos cuatro funcionalidades, manteniendo TDD y documentación. Se eligieron estas cuatro porque aprovechan el motor existente y reducen errores de uso antes de ampliar reglas sin contratos de clientes:

| Objetivo | Funcionalidad entregada | Criterio observable |
| --- | --- | --- |
| Evitar invertir versiones | Contratos por nombre: `--baseline=`, `--candidate=` | Se permite cambiar el orden de opciones, se rechazan mezclas con posiciones y duplicados. |
| Facilitar revisión y compartir resultados | `--format=markdown` | Incluye veredicto, estado, perfil, reglas, IDs, severidad, procedencia, diagnósticos y exclusiones. |
| Obtener un artefacto reproducible sin redirecciones | `--output=` | UTF-8 sin BOM, mismo contenido que stdout, reemplazo tras completar un temporal y protección de contratos/aliases. |
| Reducir el primer uso a un comando | `guard.ps1`, con `-Demo` | Compila offline si falta el JAR estándar, usa full y texto por defecto, conserva códigos y admite rutas con espacios. |

Además, `--list-profiles` descubre aliases, versiones y exclusiones sin leer archivos. La CLI Java mantiene el perfil histórico parameters por defecto; el lanzador elige full expresamente. Se conservan los formatos JSON/texto y los argumentos posicionales anteriores. No se cambia ninguna regla ni versión del perfil.

## Primer uso

Desde PowerShell 7 en el repositorio, con Corretto 25 y Maven/mvnd existentes:

```powershell
.\guard.ps1 -Demo
```

Compara el catálogo sintético base con su variante compatible. Si falta el JAR estándar, el lanzador ejecuta `clean verify` offline con mvnd o Maven. Si faltan dependencias en caché, falla sin descargarlas; su instalación requiere aprobación. No es una instalación automática ni una distribución independiente del repositorio. Si el JAR ya existe se reutiliza: después de modificar código, ejecutar `mvnd -o clean verify` para actualizarlo.

Con contratos propios:

```powershell
.\guard.ps1 -Baseline 'C:\Mis contratos\anterior.yaml' -Candidate 'C:\Mis contratos\nuevo.yaml'
.\guard.ps1 -Baseline anterior.yaml -Candidate nuevo.yaml -Format markdown -Output 'informes\revision.md'
Get-Help .\guard.ps1 -Examples
```

Las rutas de entrada/salida se interpretan desde el directorio de quien invoca. Los ejemplos Demo y el JAR estándar se localizan desde el repositorio. Un JAR personalizado se pasa con `-Jar`; si falta, se devuelve error sin compilar ni descargar otro. `-Demo` no puede mezclarse con contratos adicionales. PowerShell valida parámetros de shell; errores del motor conservan 0/1/2/3. No silenciar códigos 1/2/3 en CI.

## CLI directa y perfiles

```powershell
java -jar target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar --help
java -jar target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar --list-profiles
java -jar target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar --profile=full --format=markdown --output=informes/revision.md --baseline=anterior.yaml --candidate=nuevo.yaml
```

Todas las opciones de comparación preceden a los contratos posicionales. Con nombres explícitos, el orden de opciones es libre. No admitir combinaciones ambiguas es deliberado: baseline siempre significa contrato anterior y candidate contrato propuesto. Opciones repetidas, vacías, desconocidas o perfil/formato inválido dan código 2. `--help` y `--list-profiles` funcionan como únicos argumentos y devuelven 0 sin analizar contratos.

## Guardado y seguridad

El informe guardado coincide con stdout; no se escribe un mensaje adicional sobre el protocolo JSON. JSON sigue siendo el formato de automatización. El Markdown es presentación para revisión, con datos escapados para no introducir HTML o enlaces desde el contrato. Incluye todas las rupturas y lagunas, también cuando hay BREAKING con PARTIAL.

El escritor rechaza destinos que coinciden con un contrato, incluso si el alias existente es un symlink/hardlink. Crea carpetas necesarias y un temporal vecino, escribe UTF-8 y luego intenta reemplazo atómico. Si el sistema no soporta movimiento atómico, utiliza reemplazo normal: no promete atomicidad universal ni seguridad frente a cambios hostiles concurrentes. Limpia su propio temporal. Un error al guardar devuelve 2, aunque el análisis fuera compatible; no convertirlo en éxito.

El informe anterior permanece si no se consigue reemplazarlo. No consumirlo como resultado de la ejecución fallida. Estas garantías protegen los contratos de entrada; no se permite elegir otro archivo valioso como salida esperando que se conserve, porque los informes existentes se reemplazan deliberadamente. El resultado sigue limitado al perfil, sin garantía global de API.

## Organización del código

Se conservan un módulo Maven y los paquetes `domain`, `application`, `adapter.openapi` y `adapter.cli`. El núcleo no recibe anotaciones Spring ni E/S. Se separan tres responsabilidades concretas del comando, sin introducir interfaces ni DTOs duplicados:

| Clase | Responsabilidad |
| --- | --- |
| `CliOptions` | Parsing puro, aliases/versiones, incompatibilidad entre nombres y posiciones. |
| `ComparisonCommand` | Consultas informativas, lectura UTF-8 acotada, selección de perfil y coordinación. |
| `ReportRenderer` | JSON/texto/Markdown a partir del mismo informe. |
| `ReportFiles` | Protección de inputs y escritura de presentación en filesystem. |

`guard.ps1` solo ensambla comandos existentes; no aplica reglas. Las reglas siguen usando request old ⊆ new y response new ⊆ old. Las caches se mantienen por documento/evaluación y diferencian dirección al normalizar componentes. Se han documentado parámetros/retornos/componentes del API Java público y añadido comentarios a los puntos que justifican satisfacibilidad, cobertura finita, inversión de operandos y coexistencia de ruptura/incertidumbre.

## Javadoc verificable

```powershell
.\scripts\javadoc.ps1
# Si ya se verificó el build:
.\scripts\javadoc.ps1 -SkipBuild
```

Primero valida el API público con `-Werror -Xdoclint:all`, sin permitir comentarios o etiquetas ausentes, y genera `target/javadoc-api/`. Después genera `target/javadoc/index.html`, incluyendo clases/métodos privados para poder recorrer todo el código; esa segunda pasada mantiene validación sintáctica/de referencias y omite únicamente exigencias de etiquetas ausentes en helpers. Sus algoritmos también se explican en código y Markdown. Usa el JDK existente, sin plugin ni descargas, deshabilita enlaces automáticos externos y omite timestamps. Clean elimina la salida; regenerarla después.

Para revisar: empezar por `docs/code/README.md`, después `docs/code/runtime.md`, dominio/inclusión y adaptador OpenAPI. El Javadoc enlaza tipos, métodos y componentes con sus contratos; no sustituye las pruebas semánticas. El [portal Docusaurus](portal/documentation-site.md) está construido y verificado localmente, con ruta de lectura, explorador de fuentes y 218 comparaciones comprobadas contra el motor. Su build es independiente del Javadoc y de Maven.

## Pruebas y seguimiento

Las cinco pruebas iniciales `UsabilityTest` se ejecutaron en rojo antes de implementar las opciones/formatos/escritura. La batería del lanzador también comenzó fallando por ausencia del script. Las regresiones de CLI/JAR anteriores deben pasar sin cambiar sus expectativas públicas. Se comprueban seguridad de Markdown, protección por hardlink, reemplazo del informe, entradas inválidas y los cuatro códigos con contratos sintéticos.

El workflow incorpora la batería del lanzador y doclint, además de las pruebas anteriores. Su activación remota y la protección de main siguen dependiendo del acceso a GitHub y la aprobación de las descargas solicitadas; esta entrega no hace merge ni modifica protección.

Verificación del 2026-10-10: `mvnd -o clean verify` y `mvn -o verify` pasan con 942 pruebas JUnit (917 habituales y 25 del JAR), sin fallos ni omisiones. Pasan las 34 pruebas Git/workflow/lanzador y las diez comprobaciones anteriores de CI. Javadoc público estricto y documentación interna se generan sin avisos. Se comprobó también el primer uso real sin JAR: recompilación offline, perfil full, código 0 y stdout JSON sin logs de build. Durante la preparación una prueba del auto-build coincidió con otro build y alteró target; se repitió toda la verificación en exclusiva y se documentó esa restricción.

Siguientes objetivos de PO, pendientes de validación con clientes: distribuir una versión fijada del JAR, probar integración real de PR y medir falsos positivos/lagunas con contratos de un piloto. No se inventa feedback comercial ni se amplían dialectos/composición sin evidencia de necesidad.

## Posiciones y ruta de lectura

La salida text muestra un hallazgo por renglón con ruta absoluta, línea/columna y contexto del candidato. Refs muestran definición y uso; eliminaciones se sitúan en un contenedor existente etiquetado. Ver [garantías y pruebas](source-locations.md) y la [ruta de lectura](reading-roadmap.md). INPUT_READ_FAILED ahora identifica el archivo ilegible y recomienda comprobar ruta, permisos, UTF-8 y tamaño. No cambia el motor ni el esquema JSON.
