# Desarrollo local

## Paquete para modo prueba de tienda (22 de octubre de 2026)

Se añade prepare-test-delivery.mjs sin dependencias nuevas ni cambios Java. TDD: cinco de seis pruebas iniciales fallaron con el stub; después pasaron las seis. Se añadió una regresión roja para impedir salida dentro del origen; tras resolver el padre real pasan las siete, incluido transporte ZIP y alteración de licencia. Una batería completa de scripts pasó 99 pruebas antes de esa última regresión; después se repitieron las siete específicas. No se afirma una nueva ejecución conjunta de cien pruebas.

Maven estándar `mvn -o verify`: 937 pruebas habituales y 26 del JAR, todas correctas. Javadoc con SkipBuild pasa sin avisos. Portal: contenido generado, siete pruebas de contenido, tres del HTML y build correctos; 218 comparaciones documentales correctas. Se corrigió un enlace a un TXT no indexado por el portal, sin ampliar su generador ni copiar el directorio privado.

El ZIP usa el motor rc.3 ya verificado y conserva hash/commit/prueba de compilación originales; delivery.packagingCommit identifica 2e8f0f2. No se finge un build nuevo del binario. Se verifican 194 archivos, transporte/extracción y nueve casos sobre el JAR extraído; además se comprueba ausencia de los datos de identidad del titular en los archivos textuales. El [registro](pilot-delivery.md) contiene ruta y hashes. No se ha subido el ZIP ni activado ventas, y privacidad sigue separada y pendiente.

## Condiciones comerciales confirmadas (20 de octubre de 2026)

Fran confirma operación desde España en nombre propio, uso profesional o personal y total de 4,99 EUR/año/repositorio con impuestos incluidos. Se actualizan borrador, oferta, ficha de checkout, roadmap, README e instrucciones; no se altera motor, catálogo ni rc.3. Las condiciones para particulares conservan desistimiento aplicable, conformidad y actualizaciones necesarias; no se presume pérdida de derechos por descarga. La aceptación/ejecución reales de la tienda y el aviso legal siguen pendientes. Esta revisión documental no equivale a una auditoría jurídica ni aplica una licencia efectiva.

## Cierre de rc.3 y contacto confirmado (20 de octubre de 2026)

Fran confirma rayopuyi5@gmail.com como contacto público de Francisco Arce Ruiz; se incorpora a los borradores y ficha de venta, sin cambiar la identidad Git. Desde aebc40b limpio se genera rc.3 con ese contacto y las atribuciones de 1d0ac98. mvnd/Maven pasan 963 pruebas cada uno, Node 93 y CI diez. ZIP y extracción verifican 193 archivos; [registro y hashes](pilot-delivery.md). Javadoc sin avisos, 218 comparaciones documentales y diez pruebas del portal correctas. No se declara probado el candidato en GitHub remoto ni aprobada la publicación/licencia.

El intento previo desde 1d0ac98 quedó interrumpido sin proof ni ZIP; se conserva como intento incompleto. La documentación posterior no altera los bytes de rc.3 ni su procedencia. La licencia efectiva, datos legales/impuestos y publicación siguen pendientes de aprobación.

## Atribuciones y licencias de distribución (19 de octubre de 2026)

Se añaden un catálogo offline de 35 JAR y el loader, 94 archivos de evidencia (incluidos ocho archivos de fuentes) y avisos generados con rutas a textos/fuentes. Los originales se copian de rc.2 y la caché existente, sin descargas ni cambios de dependencias. Se prepara EPL-2.0 para Logback/Jakarta Annotations, manteniendo avisos y alternativas originales. El estado técnico no aprueba la licencia propia ni publicación; ver [revisión](third-party-review.md) y [borrador para el titular](product-license-draft.md).

TDD: fallan primero cuatro pruebas del preparador sin implementar, tres del paquete real sin avisos/loader y una regresión de checkout Windows que demuestra cambios de bytes. La fixture ZIP se reconstruye en vez de actualizar en sitio para evitar corromper cabeceras de JAR anidados y obtener fallos funcionales reales. Después pasan las ocho pruebas nuevas. El catálogo rechaza cambios/ausencias/duplicados, exige fuentes EPL y rechaza rutas no válidas o evidencia alterada. .gitattributes conserva los textos originales sin normalizarlos.

Verificación: mvnd offline clean verify y Maven estándar offline verify pasan 963 pruebas Java cada uno; 93 Node y diez comprobaciones CI pasan, incluidos integridad, kit, candidato, Action y paquete. Javadoc sin avisos y 218 comparaciones documentales correctas. El portal conserva siete pruebas de contenido y tres de HTML; se recompila con la documentación actualizada. No cambia el motor ni sus códigos/perfiles. rc.2 queda congelado; cualquier candidato nuevo registra su propia procedencia.

## Comentario actualizable en PR (16 de octubre de 2026)

TDD: pruebas nuevas fallaron antes de implementar publicador, opción/estado de Action, inclusión en paquete y ejecución en CI. Los eventos sintéticos de integración se completaron con base/head antes de verificar el fallo funcional. La API se simula: crear un comentario, actualizarlo al desaparecer rupturas, paginar, rechazar comentarios de usuarios, detectar ejecuciones/PR obsoletas, omitir forks/eventos y mostrar fallos de permisos sin exponer token. Una regresión comprueba que un extracto truncado conserva exclusiones y límite de garantía.

Sin cambios Java ni dependencias nuevas. mvnd -o verify y mvn -o verify verifican 963 pruebas Java; la batería exterior pasa 85 pruebas, incluidos paquete, transporte y ocho escenarios del consumidor. CI neutral, Javadoc y portal se verifican por sus recetas existentes. La evidencia local con API simulada no acredita una publicación real: se prepara el paquete y se actualiza la PR del consumidor para esa validación. Ver [configuración y límites](github-annotations.md#comentario-en-la-conversación-de-la-pr).

## Alcance de la estructura inicial

Esta sección describe la entrega inicial verificada en `193844d`. La comparación posterior está verificada en la sección final. Para ejecutarla, seguir [README](../README.md); el JAR antiguo no contiene esa funcionalidad.

Un módulo Maven, Java 25 y arranque Spring Boot no web conforme a la decisión aprobada en [arquitectura](architecture.md). Los paquetes `domain`, `application`, `adapter.openapi` y `adapter.cli` documentan sus límites mediante `package-info.java`. No se crean interfaces, entidades ni reglas sin un caso de uso. La aplicación arranca y cierra su contexto; todavía no compara archivos ni ofrece un resultado de compatibilidad.

La prueba JUnit 5 verifica arranque, contexto no web y cierre. No necesita starter web, base de datos ni servicios externos. Surefire falla si no descubre tests.

## Herramientas y comandos

Usar Amazon Corretto 25 con `JAVA_HOME` apuntando al JDK y `java`, Maven 3.10.0 y `mvnd` disponibles en PATH. No guardar rutas del equipo en el POM. mvnd utiliza su propia distribución Maven; comprobar `mvnd --version` y distinguirla de `mvn --version` al informar resultados.

Tras aprobar las descargas, desde la raíz:

```powershell
java -version
mvn -version
mvnd --version
mvnd clean verify
mvn verify
java -jar target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar src/test/resources/openapi/baseline.yaml src/test/resources/openapi/removed.yaml
```

El JAR de la entrega inicial solo verificaba el arranque. Tras recompilar los cambios actuales, la CLI requerirá los dos contratos anteriores y terminará con el código descrito en [README](../README.md). El build compila con `--release 25`, sin funcionalidades preview. Los resultados JUnit se guardan en `target/surefire-reports/`; `target/` y archivos de IntelliJ quedan fuera de Git.

## Dependencias aprobadas para esta tarea

- Spring Boot parent, starter y plugin 3.5.16: gestión de dependencias, arranque no web y JAR ejecutable.
- JUnit Jupiter/BOM 5.14.1: tests JUnit 5; dependencia solo de test. No añadir el starter de tests completo.
- Plugins Compiler 3.15.0, Surefire 3.5.6, Clean 3.5.0, Resources 3.3.1 y Jar 3.4.2: compilación, pruebas y ciclo de build.
- Sus dependencias transitivas y metadatos Maven; no hay parser OpenAPI en esta entrega.

La rama Boot 3.5 se elige para mantener esta base con JUnit 5 y Spring Framework 6, sin introducir una migración de framework en la estructura. Su documentación declara compatibilidad con Java 25 y Maven 3.6.3 o superior: [requisitos oficiales](https://docs.spring.io/spring-boot/3.5/system-requirements.html). La lista y sus transitivas se aprobaron explícitamente en esta sesión y el build confirmó su disponibilidad. La propiedad `junit-jupiter.version` selecciona el BOM JUnit 5; `junit.version` en este parent corresponde a JUnit 4 y no debe usarse para fijar Jupiter.

## Verificación histórica de la estructura inicial

Verificado el 2026-10-08 en Windows 11 con Amazon Corretto 25.0.4.1:

| Comprobación | Resultado |
| --- | --- |
| `mvnd clean verify` | BUILD SUCCESS; compilación release 25, una prueba, cero fallos/errores/omitidas, JAR ejecutable |
| `mvn verify` con Maven 3.10.0 | BUILD SUCCESS; la misma prueba vuelve a pasar |
| `java -jar target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar` | Arranca y termina con código 0, sin salida de banner/logs normales |
| Informe Surefire | JVM Corretto 25.0.4.1 y JUnit Jupiter efectivo 5.14.1 confirmados |
| `git diff --check` | Sin errores de whitespace |

mvnd 1.0.6 incluye Maven 3.9.16; no utiliza el Maven 3.10.0 instalado aparte. Ambos builds fueron verificados sin cambiar instalaciones. El primer intento de cada herramienta falló por el sandbox al escribir en el registro del daemon o los locks de `.m2`; la ejecución con acceso autorizado resolvió esos bloqueos. Se corrigió la propiedad de selección de Jupiter y se repitieron ambos builds con la configuración final.

## Verificación de la comparación de operaciones y corrección de tests

Verificado el 2026-10-08 en el mismo entorno:

| Comprobación | Resultado |
| --- | --- |
| `mvnd clean install` | BUILD SUCCESS; 36 pruebas, cero fallos/errores/omitidas; JAR instalado en el repositorio Maven local |
| `mvnd -o clean verify` | BUILD SUCCESS; las mismas 36 pruebas y JAR ejecutable |
| `mvn -o verify`, Maven 3.10.0 | BUILD SUCCESS; 36 pruebas |
| JAR: baseline.yaml → removed.yaml | COMPLETED/BREAKING, dos hallazgos, código 1 |
| JAR: baseline.yaml → equivalent.json | COMPLETED/COMPATIBLE, sin hallazgos, código 0 |

La reproducción inicial compilaba, pero fallaba en dos pruebas: Jackson aceptaba alias YAML pese al límite configurado y el test leía avisos nativos de Windows como UTF-8. Se corrige el adaptador para rechazar los tokens de alias; la CLI fuerza UTF-8 para su salida y logs. La prueba de proceso lee los avisos de JVM según la codificación forzada y comprueba que el JSON conserva `método` en UTF-8. No se relajan las expectativas de compatibilidad ni se omiten pruebas.

Swagger Parser v3 2.1.48, Jackson Databind/YAML 2.21.4 y SnakeYAML 2.4 ya estaban en la caché tras el build del usuario; las primeras verificaciones fueron offline. El usuario autorizó después la descarga de Maven Resolver API/Util 1.9.22, necesarias para el plugin Install 3.1.4; con ellas `clean install` también pasa. No se incorporan otras reglas. Queda un aviso no bloqueante del compilador por una API obsoleta de Jackson en el adaptador.

Ver [siguientes pasos](next-steps.md) y [límites del perfil](operations-profile.md).

## Refuerzo posterior de integración

La batería posterior añade 15 casos del motor/adaptador y 11 del JAR empaquetado. `mvnd -o clean verify` y `mvn -o verify` con Maven 3.10.0 pasan: 51 pruebas en la fase habitual y 11 en verify, 62 en total, sin fallos, errores ni omisiones. No se cambiaron reglas ni código de producción ni se descargaron dependencias nuevas.

Surefire ejecuta `*IT` después de empaquetar y guarda sus informes en `target/integration-test-reports/`. Las pruebas lanzan el JAR real, con archivos temporales y códigos 0/1/2/3. Incluyen límites, UTF-8, incertidumbre y determinismo. Los detalles, comandos y límites de esta garantía están en [test-strategy.md](test-strategy.md). `mvnd test` por sí solo no ejecuta esos casos del JAR.

## D03: ciclos TDD y corpus

1. D03a: `mvnd -o '-Dtest=EffectiveParametersTest' test` falla inicialmente por ausencia de OperationId, ParameterId y extracción. Tras implementar se verifican herencia, sustituciones, defaults, procedencia, refs y rechazo de entrada inválida; 66 pruebas habituales pasaban antes de iniciar D03b. La fixture con required textual usa comillas para distinguir texto de un booleano YAML.
2. D03b: tests del dominio e integración fallan antes de existir RequiredParametersRule y la nueva ubicación. Tras implementar pasan; la regresión CLI revela que INPUT_READ_FAILED aún usaba el perfil anterior, y se corrige exponiendo el perfil en el único puerto existente. Al cierre del paso pasan 82 pruebas habituales.
3. D03c: las nuevas pruebas del JAR fallan contra el artefacto antiguo (no detectaba obligatoriedad ni refs cíclicas de parámetros); se recompila y se actualizan las expectativas de perfil, sin debilitar hallazgos. ContractCorpusTest falla antes de existir su manifiesto; luego verifica los 146 pares definidos por escenario. Una regresión adicional falla por atribuir incertidumbre de baseline a candidate y se corrige la procedencia.

Verificación final con `mvnd -o clean verify` y `mvn -o verify` (Maven 3.10.0): BUILD SUCCESS, 231 pruebas habituales y 16 del JAR, 247 en total en cada build, cero fallos/errores/omitidas. La sustitución de JsonNode.fields por properties elimina el aviso de API obsoleta anterior. Sin descargas nuevas ni cambios en POM adicionales a la batería de integración previa.

Se ejecutan manualmente los ejemplos del JAR: parameters-baseline → parameters-required produce tres hallazgos/código 1; query-required → query-optional produce COMPATIBLE/código 0; cookie-optional → empty produce PARTIAL/INDETERMINATE/código 3; empty → invalid-required produce FAILED/INDETERMINATE/código 2. Los resultados coinciden con los comandos documentados.

La CLI y el constructor predeterminado usan operations-parameter-presence-v1. El perfil anterior conserva su semántica para consumidores Java explícitos; los IDs de eliminaciones permanecen estables. Las reglas nuevas no importan Spring, Swagger, Jackson ni E/S. Ver [perfil](parameter-presence-profile.md), [corpus](contract-corpus.md) y [estado del roadmap](next-steps.md).

El usuario confirmó que no dispone de contratos reales. La validación técnica se realiza con contratos sintéticos y se declara como tal; la validación comercial permanece pendiente.

## D05 y salida legible

El formato de texto tuvo tres pruebas rojas antes de implementarlo; las pruebas verdes incluyen después `java -jar`, UTF-8 y ayuda. Commit `fc966ab` publicado. D05 tuvo diez casos rojos por ausencia del perfil y un segundo ciclo rojo/verde para su selección desde CLI. Se añaden referencias/procedencia, ciclos, cobertura de comodines, formatos inciertos y fixtures de cuerpos opcionales/obligatorios.

Verificado el 2026-10-08: `mvnd -o clean verify` y `mvn -o verify` finalizan con BUILD SUCCESS, 249 pruebas habituales y 18 del JAR (267 en total), cero fallos, errores u omisiones. No se descargan dependencias ni se cambia el POM. El perfil adicional se selecciona mediante `--profile=request-body` y mantiene los esquemas excluidos hasta D06/D07. Los límites de entrada y la ausencia de E/S externa conservan sus regresiones anteriores.

## D06: valores simples por dirección

Fase roja inicial: 17 casos por ausencia del perfil SCHEMA_VALUES; fase verde después de normalizar tipos/enums/nullabilidad y límites conjuntos. Otro ciclo rojo/verde conecta `--profile=schema-values` y muestra dirección/código/formato en texto. El núcleo mantiene solo JDK. Un oráculo independiente contrasta 36 pares de intervalos enteros mediante enumeración exhaustiva, más Unicode, restricciones opacas y enums grandes.

La revisión reproduce y corrige con TDD punteros de schemas ausentes, dialectos desconocidos y cobertura de enums grandes con límites. No se debilitan los resultados esperados ni se omiten pruebas. Las fixtures schema-values-old/new muestran reducción de enum en petición y expansión de tipo en respuesta; el JAR verifica ambas y su comparación inversa.

Verificación final el 2026-10-08: `mvnd -o clean verify` y `mvn -o verify` (Maven 3.10.0) dan BUILD SUCCESS, 312 pruebas habituales y 19 del JAR: 331 en total, cero fallos/errores/omitidas. Sin nuevas dependencias ni cambios en POM. D07 sigue pendiente; objetos/arrays/composición opacos dan análisis parcial. [Perfil y límites](schema-values-profile.md).

## D07 y cierre de los tres incrementos

Los 21 casos iniciales de objetos fallaron antes de implementar el perfil. Las fases verdes incorporan properties/required/additionalProperties booleano y proyección readOnly/writeOnly por dirección. Un oráculo independiente prueba 196 pares de objetos abiertos/cerrados, opcionales/requeridos y enums booleanos; las expectativas no proceden del comparador.

Las regresiones adicionales cubren transiciones de acceso como revisión, ruptura demostrada con cobertura parcial, límites integrales escritos como 1.0, referencias/componentes compartidos por dirección y procedencia anidada. El presupuesto de 10.000 nodos se declara como análisis parcial y un grafo compartido de 32 niveles verifica equivalencia y ruptura en la hoja sin expansión exponencial. La revisión de cuerpos en métodos de semántica ambigua conserva revisión en ambos dialectos.

La última regresión reprodujo una descripción variable entre JVMs para una unión string/null incompatible; se corrigió ordenando los tipos antes de elegir el motivo. El test conserva una expectativa canónica, sin aceptar alternativas ni reintentar fallos en la batería final.

El 2026-10-08, `mvnd -o clean verify` y `mvn -o verify` (Maven estándar 3.10.0, Corretto 25.0.4.1) finalizan con BUILD SUCCESS: 547 pruebas habituales y 21 del JAR real, 568 en total en cada build, cero fallos/errores/omitidas. Sin cambios de POM ni descargas nuevas. La CLI conserva el predeterminado de parámetros y ofrece `--profile=schemas` para reunir D05/D06/D07, con JSON o `--format=text`.

El ejemplo objects-old → objects-new devuelve dos rupturas/código 1; la comparación inversa es compatible en el perfil. Los tests del JAR comprueban ambos dialectos, direcciones, ubicaciones, determinismo y cobertura parcial. Arrays, composición, ciclos y política completa de respuestas quedan pendientes; no se declara compatibilidad global ni validación comercial. Ver [perfil ampliado](object-schemas-profile.md).

## D08: arrays

Fase roja: quince casos nuevos por ausencia del perfil ARRAY_SCHEMAS. Fases adicionales rojas/verdes cubren selección CLI y dos contraejemplos sin existencia demostrada de elementos opacos. El oráculo independiente contrasta 324 pares de arrays homogéneos con restricciones conjuntas de items y longitudes; se verifican anidamiento, referencias acíclicas, ciclos parciales, items booleano en 3.1 y conservación del perfil anterior.

`mvnd -o clean verify` y `mvn -o verify` (Maven estándar 3.10.0) pasan con 890 pruebas habituales y 23 del JAR real en cada build: 913 en total, cero fallos/errores/omitidas. Las dos nuevas pruebas del JAR fallaron contra el artefacto anterior antes de empaquetar y pasan con la nueva CLI. No se añaden dependencias ni se realizan descargas. [Perfil de arrays](array-schemas-profile.md).

## D09: respuestas y receta CI

Trece casos rojos por ausencia de RESPONSE_POLICY; fases rojas/verdes adicionales para selección full en CLI y diagnóstico incorrecto de petición al retirar una respuesta. Las pruebas del dominio son Java puro y comprueban IDs, instantáneas inmutables y ruptura con análisis parcial. El JAR anterior falló en los dos casos nuevos antes de empaquetar; la nueva CLI verifica ambos dialectos, retiradas, equivalencia y adiciones como revisión.

`mvnd -o clean verify` y `mvn -o verify` (Maven estándar 3.10.0) pasan con 910 pruebas habituales y 25 del JAR en cada build: 935 en total, cero fallos/errores/omitidas. [Política publicada](response-policy-profile.md). Sin nuevas dependencias ni cambios de POM.

La receta CI de PowerShell 7 tuvo fase roja por ausencia del script. `pwsh -NoProfile -File scripts/test-ci.ps1` pasa diez comprobaciones contra el JAR real, con rutas con espacios, JSON UTF-8 y códigos 0/1/2/3, incluyendo tres variantes del catálogo SaaS sintético. No se instala Pester ni se ejecuta un pipeline remoto. [Guía CI](ci.md).

## Entrega de posiciones candidatas (2026-10-10)

Verificados mvnd -o clean verify y mvn -o verify: 952 pruebas (927 habituales + 25 del JAR), cero fallos/errores/omisiones. Javadoc público estricto e interno generado sin avisos. Diez regresiones nuevas de consola, con tres pruebas inicialmente en rojo. El ejemplo parameters-baseline.yaml → parameters-required.yaml da tres rupturas en renglones independientes con posiciones del candidato. Ver [diseño y límites](source-locations.md). Las cifras anteriores son evidencia de entregas históricas.

## Errores numerados y resumen de líneas candidatas (2026-10-11)

Cinco expectativas de consola fallaron antes de implementar la numeración y el resumen. Se conserva un hallazgo por renglón, con Error N - Líneas, contexto candidato ordenado y sin duplicados, definición y uso de refs y enlaces archivo:línea:columna. Los diagnósticos del baseline y archivos ilegibles no reciben líneas candidatas inventadas. JSON, Markdown, perfiles y códigos permanecen iguales. No se añaden dependencias ni descargas.

Verificación: mvnd -o clean verify y mvn -o verify pasan 953 pruebas (928 habituales + 25 del JAR), sin fallos, errores ni omisiones. El ejemplo de parámetros produce tres errores numerados y código 1. Javadoc estricto e interno se generan correctamente. Docusaurus compila con enlaces estrictos y pasan sus siete pruebas de contenido y tres del HTML construido. Ver [presentación y límites](source-locations.md).

## Anotaciones y resumen de GitHub Actions (2026-10-11)

TDD: formato CI en rojo antes de implementar; prueba del JAR y cinco comparaciones Git fallaron contra el artefacto anterior. Siete pruebas de presentación cubren posiciones separadas, escape de comandos/Markdown, errores anteriores y eliminaciones sin línea inventada, límites y errores de ejecución. La prueba de renombrado utiliza similitud suficiente para que Git lo reconozca y verifica anotaciones de definición y uso en la ruta candidata real.

mvnd -o clean verify y mvn -o verify pasan 957 pruebas (931 habituales y 26 del JAR), sin fallos/errores/omisiones. Pasan 42 pruebas Node de Git, workflow, presentación y lanzador; Javadoc público estricto e interno se generan. No hay dependencias nuevas ni descargas. Las variables de Actions se simulan en pruebas locales; el renderizado remoto y la distribución para consumidores siguen pendientes. Ver [guía de Actions](github-annotations.md).

## Salida inglesa y JSON legible (11 de octubre de 2026)

Tres pruebas nuevas fallaron antes de implementar JSON/CI con sangría, cabeceras y descripciones en inglés, ayuda y errores de entrada en inglés. Se preservan los nombres Unicode del contrato, la estructura JSON, los identificadores y los códigos de salida. Las expectativas históricas de texto se actualizan al nuevo idioma. El lanzador PowerShell y la receta CI incorporan expectativas inglesas que también fallaron antes de traducir sus mensajes. Las lagunas de referencias y presupuesto se identifican mediante constantes exactas al decidir la caché, evitando depender de palabras traducidas.

La verificación cubre 960 pruebas Java por build (934 habituales y 26 del JAR), 42 pruebas Node, diez comprobaciones CI, Javadoc público estricto e interno, siete pruebas de contenido del portal, tres del sitio compilado y 218 comparaciones de documentación. Se usan exclusivamente herramientas y dependencias existentes, sin descargas. JSON y CI se leen directamente en consola; text sigue mostrando un hallazgo por renglón. La consola debe admitir UTF-8 para nombres, rutas y fragmentos Unicode del usuario. Ver [presentación y límites](source-locations.md).

## Preparación de distribución y Action (11 de octubre de 2026)

El usuario pidió avanzar con iniciativa en distribución evaluable y Action reutilizable. Ocho pruebas nuevas fallaron antes de implementar. La Action compuesta prepara Java 25, verifica SHA256 del JAR, resuelve base de PR/merge queue y merge propuesto, delega al adaptador Git existente y publica salidas antes de conservar códigos fallidos 1/2/3. El paquete local copia binario/scripts/ejemplos e inventaría 35 bibliotecas reales con hashes y evidencia embebida. Rechaza sobrescribir destinos existentes. El motor Java y sus reglas no cambian.

Se comprueban consumidores Git independientes con códigos 0/1/2/3, resumen, anotaciones, hash incorrecto y eventos explícitos. El paquete copiado ejecuta tanto ayuda del JAR como la Action desde otro repositorio sin usar Maven allí. La batería registra 50 pruebas Node y conserva 960 Java y diez comprobaciones CI. El portal tiene 37 guías, pruebas de contenido/HTML y 218 comparaciones; Javadoc estricto sigue pasando. No se instalan dependencias ni se publica una release, artefacto externo o Marketplace. Las evidencias locales no cierran la elección de licencia, atribuciones, firma/procedencia de release, soporte Linux/macOS ni renderizado en PR real. [Diseño y siguientes pasos](evaluable-distribution.md).

## Candidato privado y kit de aceptación (12 de octubre de 2026)

Ejecución completa posterior desde 4fd4fdb: candidato 0.1.0-rc.1 generado correctamente bajo dist/0.1.0-rc.1-4fd4fdbd. mvnd y Maven estándar pasan 960 pruebas cada uno; 66 Node y diez comprobaciones CI pasan sobre ese binario. El ZIP se extrae y verifica (98 archivos) y genera el kit. Proof conserva publicación/PR remota pendientes. [Registro y hashes exactos](pilot-delivery.md).

Fran eligió piloto comercial privado con términos para revisar. La continuación añade manifiesto de archivos v2, verificación cerrada, ZIP/checksum, extracción con presupuestos y rechazo de rutas peligrosas, kit con workflow completo y ocho escenarios, versión Maven configurable y build desde checkout limpio con commit/version en el JAR. Los términos se incluyen como borrador no efectivo. No cambian las reglas ni se añade infraestructura.

TDD: las regresiones de integridad/transporte, kit y puertas del build fallaron antes de sus implementaciones; la comprobación de -Drevision falló contra el POM anterior. Una prueba de manipulación se corrigió para exigir el rechazo por integridad, evitando un falso positivo debido al nombre del directorio temporal. Maven clean se solapó accidentalmente con un log abierto; se corrigió la secuencia y se repitió clean verify con éxito.

El build predeterminado verifica 960 pruebas Java y la batería exterior 66 pruebas Node, incluidos los ocho casos de consumidor real temporal. El pipeline de candidato registra sus propios logs y proof al ejecutarse desde el commit limpio, con verificación adicional Maven estándar y CI. La publicación y los resultados de una PR remota no se deducen de esa prueba local. [Roadmap, entrega y puertas comerciales](candidate-roadmap.md).

## Informes de PR y rupturas intencionadas (16 de octubre de 2026)

Se contrastó el primer registro remoto incompatible con el diff real `4bdd2f1` → `2813545`: cuatro hallazgos correctos sobre quantity/status/maxItems. Eso no acredita todavía navegación de anotaciones, descarga de artefactos ni todos los casos remotos.

TDD: fallaron primero las regresiones de descripciones concretas y location en CI, y las nuevas expectativas de política/resumen/consola. Una fixture de objetos se ajustó al perfil full antes de confirmar el fallo funcional. También se reprodujo el caso de ruptura con revisión pendiente: permitir rupturas no puede ocultar esa incertidumbre. La comprobación de integridad existente detectó una importación prematura del helper; ahora se carga dinámicamente después de verificar los archivos del paquete.

`mvnd -o clean verify` y `mvn -o verify` pasan 963 pruebas cada uno (937 habituales y 26 del JAR), sin fallos/errores/omisiones. Pasan 76 pruebas Node, diez escenarios CI, 218 comparaciones documentales, siete pruebas del portal y tres de HTML compilado. Javadoc público con doclint completo e interno sin avisos. Una regresión adicional comprueba que los enums abreviados no cortan caracteres Unicode suplementarios. Sin dependencias nuevas ni descargas de dependencias.

La política solo altera el estado del check: `fail-on-breaking: 'false'` conserva el código 1 y el informe BREAKING, permite análisis completos con rupturas y devuelve 3 si también queda revisión. Errores de entrada/ejecución siguen devolviendo 2. La CLI y la prueba de inclusión conservan su semántica. El candidato ZIP anterior permanece intacto; [configuración y actualización del paquete](github-annotations.md).

## Cierre técnico rc.2 y revisión comercial (16 de octubre de 2026)

Por autorización de Fran se integra mediante fast-forward el código estable b6e32d4 en main y se sube a origin. Desde ese commit limpio se genera 0.1.0-rc.2: mvnd offline clean verify y Maven estándar offline verify pasan 963 pruebas Java cada uno, 85 pruebas Node y diez comprobaciones CI. ZIP, extracción y kit verifican 99 archivos. El [registro de entrega](pilot-delivery.md) conserva rutas, hashes y la distinción entre evidencia local y la PR del paquete anterior.

La revisión posterior solo cambia documentación/menú del portal. Se verifican Javadoc público con doclint completo e interno sin avisos, 218 comparaciones documentales y diez pruebas del portal (siete de contenido y tres del HTML compilado). Las 17 comprobaciones de integridad, paquete, kit y puertas del candidato pasan con el JAR rc.2 tras actualizar los documentos; el primer intento bajo sandbox falló por permisos al crear .git/config de repositorios temporales, y se repitió con los permisos necesarios sin cambiar código. La preparación del portal detectó un enlace a TXT no soportado; se corrigió la referencia documental y el build final pasa.

No se amplía semántica ni se añade infraestructura, dependencia, pago o activación. La [revisión de terceros](third-party-review.md) registra siete JAR sin texto de licencia recogido, el loader fuera del inventario y cierre de fuentes EPL pendiente. Fran cambia a evaluación pública/autoservicio, sin tres equipos obligatorios. Inicialmente quedaron pendientes precio y plataforma; posteriormente confirma **4,99 EUR al año por repositorio** y preparar **Lemon Squeezy**. Impuestos, licencia y aprobación final de publicación siguen pendientes. La [guía de preparación anual](lemon-squeezy-setup.md) distingue pruebas de suscripción de descarga real, deshabilitada por la plataforma en modo prueba. No se ha configurado una cuenta remota. Los borradores del ZIP quedan congelados con su origen histórico; no son términos vigentes.

## Revisión comercial del 22 de octubre de 2026

Se registran las decisiones confirmadas por Fran: evaluación de 30 días desde el primer uso, colaboradores y ejecuciones sin tarificación adicional de Guard por repositorio, traslado de licencia, cancelación al final del periodo y posterior renovación/cese, versiones publicadas y soporte por correo sin SLA. Se prepara la redacción completa para revisión en product-license-draft.md; no se hace efectiva ni se publica en el repositorio documental público. Se actualizan README, oferta, términos de evaluación y guía de tienda para distinguir cuenta/producto de prueba comunicados por Fran de compra/entrega verificadas.

Se explicita la incompatibilidad entre copiar el JAR en un repositorio público y la cláusula propuesta de no redistribución: preparar adquisición en runner o revisar el permiso antes de anunciar ese escenario. No se modifica código, el empaquetador ni los ZIP rc.2/rc.3. La licencia, datos del aviso legal, privacidad y apertura de ventas siguen pendientes.

Verificación documental: siete pruebas de contenido y tres del HTML del portal; compilación Docusaurus completada usando su lanzador instalado (configura NODE_PATH de pnpm). La invocación directa al binario sin ese entorno falló al resolver @mdx-js/react; no se instalaron dependencias ni se cambiaron versiones. Los 218 ejemplos documentales pasan contra el motor existente. Las 16 pruebas de integridad, kit y construcción de candidato pasan con el JAR rc.3 congelado. No se recompiló Java por un cambio exclusivamente documental. Esta verificación no es una revisión jurídica ni una prueba de compra.
