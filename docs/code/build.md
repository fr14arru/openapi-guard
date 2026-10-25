**Transición MIT (22 de octubre):** prepareAttributions incluye LICENSE.txt desde la licencia raíz y devuelve productLicenseStatus MIT, productLicenseFile y productLicenseSha256. prepare-evaluation.ps1 ya no copia términos de piloto; genera estados OPEN_SOURCE_LOCAL_PACKAGE u OPEN_SOURCE_CANDIDATE_PUBLICATION_PENDING. Las pruebas de atribuciones y paquete real comprueban MIT y ausencia de condiciones comerciales. El verificador de integridad conserva compatibilidad con paquetes históricos. prepare-test-delivery.mjs permanece como utilidad histórica, no como flujo actual. Ver [distribución actual](../open-source.md).

# Build, configuración y scripts

## Empaquetar para probar la tienda sin recompilar el motor

[prepare-test-delivery.mjs](../../scripts/prepare-test-delivery.mjs) verifica el paquete de origen con verifyPackage, coteja build-proof y catálogo de atribuciones, exige el documento aprobado exacto y aplica una lista cerrada de archivos. approvedLicense retira únicamente notas internas/rótulos del documento de revisión. prepareTestDelivery copia runtime y fuentes sin modificar sus bytes, añade README/LICENSE/estado, regenera hashes y verifica el resultado. El origen no se altera; un destino existente se rechaza.

La CLI exige checkout limpio y registra su commit en delivery.packagingCommit. sourceCommit, version y build-proof conservan la identidad del binario previo. La modalidad LEMON_SQUEEZY_TEST_ONLY nunca autoriza subir a tienda ni abrir ventas. [Procedimiento y verificaciones](../test-store-delivery.md).

## pom.xml

Un único módulo `com.openapiguard:openapi-breaking-change-guard:0.1.0-SNAPSHOT`. El parent Spring Boot 3.5.16 gestiona versiones/configuración compartida; relativePath vacío evita un parent local accidental. Java y compiler release son 25, UTF-8 en código e informes. No hay preview ni rutas de equipo.

| Dependencia directa | Finalidad |
| --- | --- |
| swagger-parser-v3 2.1.48 | Validación y operaciones OpenAPI en el adaptador. |
| jackson-databind, versión gestionada | Árbol original y serialización del informe. |
| jackson-dataformat-yaml, versión gestionada | Lectura YAML/JSON y restricciones de entrada. |
| spring-boot-starter | Contexto, configuración y logging; sin starter web. |
| junit-jupiter 5.14.1, scope test | Tests unitarios, parametrizados e integración. |

Las transitivas y aprobaciones se registran en [desarrollo local](../development.md). Esta documentación no instala ni añade dependencias. mvnd acelera el build, pero el POM también funciona con Maven estándar; la versión Maven embebida de mvnd puede diferir de Maven 3.10.0 del PATH.

| Plugin | Versión | Papel |
| --- | --- | --- |
| clean | 3.5.0 | Borra target en clean. |
| resources | 3.3.1 | Copia recursos de producción/tests. |
| compiler | 3.15.0 | Compila código/tests para Java 25. |
| surefire | 3.5.6 | Ejecuta tests normales y exige que haya tests. Segunda ejecución en verify para `*IT.java`. |
| jar | 3.4.2 | Crea el JAR inicial. |
| spring-boot-maven-plugin | 3.5.16 | Reempaqueta el JAR ejecutable con dependencias. |

La ejecución `packaged-cli-integration-tests` usa Surefire también, no Failsafe. Incluye `*IT.java`, escribe en integration-test-reports y pasa guard.executableJar con la ruta del artefacto. `test` no prueba el JAR empaquetado; `verify` sí. `install` añade la instalación en la caché local y ejecuta las fases anteriores.

## Recursos y archivos raíz

`application.properties` fija web-application-type=none, banner-mode=off y logging root WARN. `logback-spring.xml` configura un ConsoleAppender a System.err con charset UTF-8 y patrón nivel/logger/mensaje. Protege el protocolo stdout incluso cuando una librería emite warnings.

`.gitignore` excluye target, .idea y archivos iml: informes, JAR, Javadoc generado y configuración local no se versionan. `AGENTS.md` regula alcance, TDD, autorizaciones y Git. `README.md` es la entrada de uso; los documentos de perfil describen políticas públicas y esta guía explica implementación. No existe configuración cloud, base de datos ni workflow específico de un proveedor.

## scripts/compare-contracts.ps1

Requiere PowerShell 7. Parámetros obligatorios Baseline/Candidate; opcionales Jar, Report y Profile (full por defecto). Las rutas predeterminadas Jar/Report parten de PSScriptRoot; los argumentos relativos se resuelven desde el directorio del llamante mediante GetFullPath.

1. Canoniza rutas absolutas y evita que Report sea la misma ruta textual que un contrato o el JAR, sin distinguir mayúsculas. No es una defensa contra todos los aliases de archivos, hard links o enlaces simbólicos.
2. Exige JAR existente, crea la carpeta del informe y vacía su contenido. El script sobrescribe un informe anterior autorizado por Report; los contratos permanecen protegidos por la comprobación anterior.
3. Inicia java sin shell, con argumentos separados, streams redirigidos y UTF-8. No construye una línea de comando a partir de strings ni inicia Maven.
4. Lee stdout/stderr de forma asíncrona para evitar bloqueos por buffers. Límite de proceso Java 60 segundos; al excederlo termina su árbol de procesos.
5. Admite códigos 0/1/2/3. Valida JSON y nombres de status/verdict, escribe informe UTF-8 sin BOM, muestra resumen y conserva el código del motor. Código 2 sin JSON conserva el error de uso; códigos inesperados/formato incorrecto producen 2.
6. Catch escribe error en stderr y devuelve 2; finally libera Process. La validación del script no sustituye un schema completo de todos los campos del informe.

## scripts/test-ci.ps1

Requiere PowerShell 7 y un JAR previamente construido. Crea archivos temporales bajo `target/ci script tests`, con espacios en las rutas. `Invoke-Check` lanza un proceso pwsh por comparación, captura ambas salidas, verifica código y, para resultados que no sean error 2, lee informe UTF-8 y comprueba perfil/verdict/status aplicables. La espera del proceso pwsh no tiene un timeout propio; la comparación Java del script hijo sí está acotada.

Los diez escenarios son: compatible, breaking, review, archivo ausente, perfil inválido, JAR ausente, protección de contrato, y las tres variantes del catálogo. Tras la protección compara bytes para verificar que el input no se sobrescribió. No usa Pester ni instala herramientas. Es una receta CI local neutral, sin proveedor externo.

## Verificación habitual

```powershell
mvnd -o test
mvnd -o clean verify
mvn -o verify
pwsh -NoProfile -File scripts/test-ci.ps1
```

`-o` evita descargas; exige las dependencias aprobadas ya cacheadas. Si falta una nueva dependencia, detener la instalación y pedir aprobación concreta. No interpretar códigos 1/3 del motor como fallos de build: son resultados de comparación que los tests deben comprobar.

## Generar documentación navegable con el JDK

El POM usa revision con valor predeterminado 0.1.0-SNAPSHOT. Un candidato cambia solo la versión solicitada y añade Guard-Source-Commit/Guard-Version al manifiesto del JAR mediante el plugin existente. build-candidate.ps1 realiza verificación limpia, registra proof y transporta un paquete local bajo dist/, conservado después de clean. No publica a Maven Central ni crea una release; la instalación como dependencia Maven para otros proyectos no forma parte de esta distribución binaria. [Procedimiento](../candidate-roadmap.md).

Después del build se puede preparar un directorio local autocontenido con `scripts/prepare-evaluation.ps1 -Destination target/evaluation-pilot`. No compila, descarga ni publica; exige JAR existente y destino nuevo. Conserva hashes y evidencia de las bibliotecas reales, sin aprobar su redistribución. Llama a prepare-attributions.mjs para comprobar catálogo, 35 JAR, loader y fuentes/textos antes de cerrar la integridad. Los activos versionados en licensing/ evitan depender de la caché Maven del consumidor. [Action y distribución evaluable](../evaluable-distribution.md).

```powershell
./scripts/javadoc.ps1
# Si ya se verificó el build:
./scripts/javadoc.ps1 -SkipBuild
```

El script construye offline cuando es necesario y obtiene el classpath del informe de tests aprobado. Usa el JDK existente, sin plugin/dependencia nueva. Primero `-Werror -Xdoclint:all` valida el API público completo en target/javadoc-api. Después genera `target/javadoc/index.html` con helpers privados, validando sintaxis/referencias y omitiendo únicamente etiquetas ausentes de helpers. `--no-platform-links` evita consultas externas y `-notimestamp` evita una fecha variable.

El detalle de los helpers complejos se conserva también en las guías de dominio/inclusión/adaptador y en comentarios de código. Clean borra el HTML, que se regenera después. No versionar classpath ni archivos generados. [Primer uso y cuatro mejoras](../product-usability.md) describe `guard.ps1`, argumentos por nombre, Markdown y salida a archivo. No ejecutar compilaciones concurrentes sobre el mismo target.
