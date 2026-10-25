> **Cambio de modelo (22 de octubre de 2026):** el código propio pasa a MIT y uso gratuito. Las condiciones comerciales, precios y aprobaciones pendientes que aparecen más abajo son historial, no requisitos vigentes de esta distribución. Las evidencias y licencias de terceros se conservan. Estado y siguientes pasos: [transición MIT](open-source.md).

# Bibliotecas y atribuciones: revisión y cierre técnico

## Actualización del 19 de octubre de 2026

Entrega verificada el 20 de octubre: [rc.3, rutas y hashes](pilot-delivery.md) contiene esta recopilación y el contacto confirmado, con 193 archivos cubiertos por integridad. No se cambia retrospectivamente rc.2.

Se han incorporado textos, avisos, procedencia y hashes en licensing/catalog.json, con evidencia original en licensing/files/ y ocho archivos de fuentes en licensing/sources/. Todo procede de archivos existentes del candidato/caché local; no se han descargado dependencias. El inventario siguiente conserva la evidencia inicial de rc.2; sus huecos se resuelven en los paquetes nuevos, sin modificar rc.2.

| Hueco original | Resolución técnica |
| --- | --- |
| Siete JAR con solo POM | Texto completo de Apache/EPL según el componente, cabeceras originales disponibles y fuentes de sus versiones exactas. Se conserva la procedencia sin inventar NOTICE. |
| Loader ausente | Componente separado con versión y fingerprint de todas sus clases, texto y NOTICE de Spring Boot. |
| License.class como evidencia | El recolector excluye .class, .jar y .zip de los textos embebidos. |
| Fuente EPL | Archivos de fuentes originales de Logback Core/Classic 1.5.34 y Jakarta Annotations 2.1.1 incluidos junto con EPL y avisos; accesibles desde THIRD-PARTY-NOTICES.md. |
| Actualizaciones sin revisión | El empaquetador falla ante bibliotecas/loader desconocidos o hashes distintos, fuentes/textos alterados o ausentes. |
| Conversión de líneas Git | .gitattributes preserva los bytes; regresión de checkout con core.autocrlf=true. |

El paquete nuevo incluye THIRD-PARTY-NOTICES.md, catálogo y fuentes; todo entra en la integridad del ZIP. El estado TECHNICAL_EVIDENCE_COMPLETE identifica estas comprobaciones, no una aprobación legal. PRODUCT-LICENSE-STATUS.txt conserva OWNER APPROVAL PENDING y la distribución sigue sin aprobarse para publicación.

**Unidad de distribución:** ZIP completo, con avisos y fuentes junto al JAR. No se han añadido estos textos al interior del ejecutable; ofrecer guard.jar aislado no conserva esta entrega. La futura instalación pública deberá obtener y mantener el paquete completo.

**Pendiente del titular:** completar y aprobar la [licencia del producto](product-license-draft.md), revisar especialmente obligaciones comerciales EPL y autorizar el destino de publicación. La opción EPL-2.0 preparada para los componentes duales mantiene sus avisos y derechos; no aplica una licencia nueva a terceros. No se han auditado vulnerabilidades ni dependencias del portal como parte de este inventario runtime.

## Registro inicial de rc.2 (16 de octubre)

Revisión técnica del 16 de octubre de 2026 sobre las versiones empaquetadas en el candidato rc.2, procedentes del código estable b6e32d4. No es una autorización legal ni una auditoría de vulnerabilidades. No se han instalado herramientas ni descargado dependencias.

## Método y alcance

Se inspeccionaron manifest.json, los textos de third-party/, POM embebidos y POM padres disponibles en la caché Maven. El paquete enumera 35 JAR de ejecución. Se revisó además el loader de Spring Boot que está en la raíz del ejecutable, fuera de BOOT-INF/lib.

Los hashes exactos por artefacto están en el manifiesto del candidato; el [registro de entrega](pilot-delivery.md) identifica ese paquete. Un nombre de licencia en un POM no reemplaza su texto ni sus obligaciones. El estado automático PENDING se conserva: no se transforma esta revisión en una aprobación de distribución.

## Inventario revisado

En la columna de evidencia, «texto» significa archivo de licencia conservado; «POM» significa que la declaración se encontró en el POM o su padre, sin texto de licencia recogido para ese JAR.

| Artefacto | Licencia observada | Evidencia / atención necesaria |
| --- | --- | --- |
| commons-io 2.22.0 | Apache-2.0 | Texto y NOTICE. |
| commons-lang3 3.17.0 | Apache-2.0 | Texto y NOTICE. |
| jackson-annotations 2.21 | Apache-2.0 | Texto y NOTICE. |
| jackson-core 2.21.4 | Apache-2.0 y subcomponentes | Textos y NOTICE; conservar también FastDoubleParser y Schubfach. |
| jackson-databind 2.21.4 | Apache-2.0 | Texto y NOTICE. |
| jackson-dataformat-yaml 2.21.4 | Apache-2.0 | Texto y NOTICE. |
| jackson-datatype-jsr310 2.21.4 | Apache-2.0 | Texto y NOTICE. |
| jakarta.activation-api 2.1.4 | EDL-1.0 / BSD-3-Clause | LICENSE.md y NOTICE.md. |
| jakarta.annotation-api 2.1.1 | EPL-2.0; alternativa GPL con Classpath Exception declarada | Texto y NOTICE; revisar redistribución y disponibilidad del código fuente bajo la opción elegida. |
| jakarta.validation-api 3.0.2 | Apache-2.0 | Solo POM; completar texto y atribución aplicable. |
| jakarta.xml.bind-api 4.0.5 | EDL-1.0 / BSD-3-Clause | Texto, NOTICE y declaración del padre. |
| jul-to-slf4j 2.0.18 | MIT | LICENSE.txt. |
| log4j-api 2.24.3 | Apache-2.0 | Texto y NOTICE. |
| log4j-to-slf4j 2.24.3 | Apache-2.0 | Texto y NOTICE. |
| logback-classic 1.5.34 | EPL-2.0 OR LGPL-2.1-only | Solo POM padre; completar texto y mecanismo de acceso al código fuente. |
| logback-core 1.5.34 | EPL-2.0 OR LGPL-2.1-only | Solo POM padre; mismo cierre que classic. |
| micrometer-commons 1.15.12 | Apache-2.0 | Texto y NOTICE. |
| micrometer-observation 1.15.12 | Apache-2.0 | Texto y NOTICE. |
| slf4j-api 2.0.18 | MIT | LICENSE.txt. |
| snakeyaml 2.4 | Apache-2.0 | Solo POM; completar texto y atribución aplicable. |
| spring-aop 6.2.19 | Apache-2.0 | license.txt y notice.txt. |
| spring-beans 6.2.19 | Apache-2.0 | license.txt y notice.txt. |
| spring-boot 3.5.16 | Apache-2.0 | LICENSE.txt y NOTICE.txt. |
| spring-boot-autoconfigure 3.5.16 | Apache-2.0 | LICENSE.txt y NOTICE.txt. |
| spring-boot-jarmode-tools 3.5.16 | Apache-2.0 | LICENSE.txt y NOTICE.txt. |
| spring-context 6.2.19 | Apache-2.0 | license.txt y notice.txt. |
| spring-core 6.2.19 | Apache-2.0 y subcomponentes | Textos; ASM BSD y avisos de CGLIB/JavaPoet/Objenesis incluidos. |
| spring-expression 6.2.19 | Apache-2.0 | license.txt y notice.txt. |
| spring-jcl 6.2.19 | Apache-2.0 | license.txt y notice.txt. |
| swagger-annotations 2.2.53 | Apache-2.0 | Texto, NOTICE y POM padre. |
| swagger-core 2.2.53 | Apache-2.0 | Texto, NOTICE y POM padre. |
| swagger-models 2.2.53 | Apache-2.0 | Texto, NOTICE y POM padre. |
| swagger-parser-core 2.1.48 | Apache-2.0 | Solo POM padre; completar texto y atribución aplicable. |
| swagger-parser-safe-url-resolver 2.1.48 | Apache-2.0 | Solo POM padre; completar texto y atribución aplicable. |
| swagger-parser-v3 2.1.48 | Apache-2.0 | Solo POM padre; completar texto y atribución aplicable. |

El loader Spring Boot 3.5.16 es un componente adicional a esos 35 JAR. La raíz del ejecutable inspeccionado contiene JarLauncher.class, pero no un LICENSE/NOTICE raíz recogido por el inventario. Hay que atribuirlo explícitamente, aunque otros JAR de Boot ya aporten textos de Apache.

## Consecuencias para la distribución

[Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) permite redistribución comercial con condiciones, entre ellas conservar licencia y avisos aplicables e identificar modificaciones. MIT y BSD también exigen conservar sus avisos/textos aplicables. No eliminar NOTICE por considerar suficiente una lista de nombres.

No todo el paquete es Apache. [Logback documenta la elección EPL-2.0 o LGPL-2.1](https://logback.qos.ch/license.html), confirmada para 1.5.34 en su POM padre local. Recomendamos revisar la vía EPL-2.0 para Logback y Jakarta Annotations. La [EPL-2.0](https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html) impone condiciones al distribuir su programa, incluyendo fuente disponible, avisos y obligaciones específicas del distribuidor comercial; no significa automáticamente publicar todo el código independiente de OpenAPI Guard. Hay que definir y verificar cómo obtiene el destinatario el fuente exacto de esos componentes y mantener sus derechos, también en nuestra licencia comercial.

Jackson Core conserva licencias separadas de FastDoubleParser y Schubfach: se observaron textos MIT y Boost en su evidencia. Spring Core incorpora textos de subcomponentes, incluido ASM bajo BSD. No sustituir esos archivos por un único texto Apache.

El Java de Corretto no se entrega dentro del JAR/ZIP: se instala en el equipo o runner. No se ha auditado como runtime redistribuido. Node, Git y las Actions externas usadas por el workflow tampoco son JAR incluidos; revisar sus versiones/licencias al publicar la envoltura. Las dependencias Docusaurus son del portal y requieren su propia revisión si se distribuye el portal; no forman parte de estas 35.

## Huecos detectados originalmente en rc.2

1. **Siete JAR solo aportan POM como evidencia de licencia:** Jakarta Validation, los dos Logback, SnakeYAML y los tres Swagger Parser. Incorporar textos/avisos autorizados de las versiones exactas y su procedencia.
2. **Loader fuera del inventario de JAR anidados:** registrar versión, licencia y avisos de ese código en la entrega.
3. **Falsas evidencias por nombre:** se recogen archivos como License.class/LicenseMixin.class. Son clases del modelo Swagger, no textos de licencia. Excluir binarios de la recogida documental sin perder textos con nombres no estándar.
4. **Disponibilidad de fuente EPL:** definir el aviso, origen exacto y acceso efectivo al fuente de Logback y Jakarta Annotations; comprobar también posibles modificaciones. No afirmar cumplido con una URL genérica sin verificar.
5. **Licencia del propio producto:** no hay LICENSE efectivo del motor en la raíz. Los borradores no conceden autorización de evaluación pública ni de uso comercial.
6. **Documento de atribuciones final:** unir inventario, opciones elegidas, textos íntegros y acceso a fuentes; verificar desde el ZIP extraído. El informe automático actual no lo sustituye.

Estos fueron los criterios del incremento de atribuciones; la actualización superior registra su resolución técnica. Mantener rc.2 congelado. Cualquier nueva versión exige renovar el catálogo y revisar obligaciones; no convertir hashes nuevos automáticamente en una revisión aprobada.

## Estado

La revisión identifica licencias habituales compatibles con modelos comerciales si se cumplen sus condiciones, y trabajo concreto pendiente. No se ha encontrado fundamento para declarar «todo Apache» o «listo legalmente para vender». Fran debe aprobar el texto propio y la opción de distribución; las obligaciones de terceros no se pueden suprimir en ese texto. Ver [plan comercial](commercial-launch.md).

